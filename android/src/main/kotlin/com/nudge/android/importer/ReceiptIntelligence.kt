package com.nudge.android.importer

import com.nudge.engine.MerchantNormalizer
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

data class ReceiptLineDraft(
    val name: String,
    val quantity: Double = 1.0,
    val unitPriceCents: Long? = null,
    val lineTotalCents: Long,
    val categoryHint: String? = null,
    val selectedCategoryId: String? = null,
    val confidence: Float = .82f,
)

data class ReceiptPageDraft(
    val localUri: String,
    val ocrText: String,
    val warning: String? = null,
)

data class DetailedReceiptDraft(
    val merchant: String,
    val totalCents: Long,
    val printedTotalCents: Long = totalCents,
    val subtotalCents: Long? = null,
    val discountCents: Long = 0,
    /** Informational MRP savings already reflected in product values; never subtract again. */
    val savingsCents: Long = 0,
    val taxCents: Long = 0,
    val feeCents: Long = 0,
    val tipCents: Long = 0,
    val roundingCents: Long = 0,
    val purchaseTimestamp: Long = System.currentTimeMillis(),
    val items: List<ReceiptLineDraft> = emptyList(),
    val pages: List<ReceiptPageDraft> = emptyList(),
    val rawText: String = "",
    val confidence: Float = .75f,
) {
    val itemTotalCents: Long get() = items.sumOf { it.lineTotalCents }
    val calculatedTotalCents: Long get() = (subtotalCents ?: itemTotalCents) - discountCents + taxCents + feeCents + tipCents + roundingCents
    val mismatchCents: Long get() = printedTotalCents - calculatedTotalCents
}

/** Pure, deterministic receipt parser used by camera, gallery and file imports. */
object ReceiptIntelligence {
    private data class GstBreakup(val taxableCents: Long?, val taxCents: Long, val totalCents: Long?)

    fun stitchPages(pageTexts: List<String>): String {
        val pages = pageTexts.map { text -> text.lineSequence().map(::cleanLine).filter(String::isNotBlank).toList() }
            .filter(List<String>::isNotEmpty)
        if (pages.isEmpty()) return ""
        val stitched = pages.first().toMutableList()
        pages.drop(1).forEach { next ->
            val maxOverlap = minOf(12, stitched.size, next.size)
            val overlap = (maxOverlap downTo 1).firstOrNull { count ->
                val tail = stitched.takeLast(count)
                val head = next.take(count)
                tail.zip(head).count { (a, b) -> lineSimilarity(a, b) >= .78f } >= maxOf(1, count - 1)
            } ?: 0
            stitched += next.drop(overlap)
        }
        return stitched.joinToString("\n")
    }

    fun parse(pageTexts: List<String>, pages: List<ReceiptPageDraft> = emptyList(), now: Long = System.currentTimeMillis()): DetailedReceiptDraft? {
        val text = stitchPages(pageTexts)
        val lines = text.lineSequence().map { repairCommonOcr(cleanLine(it)) }.filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return null

        val gstBreakup = parseGstBreakup(lines)
        val printedTotal = detectPrintedTotal(lines, gstBreakup)
            ?: return null
        val subtotal = gstBreakup?.taxableCents ?: findLabeledAmount(lines, SUBTOTAL_LABELS, preferLast = true)
        val discount = findLabeledAmount(lines, DISCOUNT_LABELS, preferLast = false) ?: 0L
        val savings = findLabeledAmount(lines, SAVINGS_LABELS, preferLast = true) ?: 0L
        val tax = gstBreakup?.taxCents ?: lines
            .filter { TAX_LABELS.containsMatchIn(it) && !TOTAL_LABELS.containsMatchIn(it) }
            .mapNotNull(::taxAmount).sum()
        val fee = lines.filter { FEE_LABELS.containsMatchIn(it) }.mapNotNull(::lastAmount).sum()
        val tip = lines.filter { TIP_LABELS.containsMatchIn(it) }.mapNotNull(::lastAmount).sum()
        val rounding = lines.firstNotNullOfOrNull { line ->
            if (!ROUNDING_LABELS.containsMatchIn(line)) null else signedLastAmount(line)
        } ?: 0L
        val itemCeiling = (subtotal ?: (printedTotal + discount - tax).takeIf { it > 0 } ?: printedTotal)
            .coerceAtLeast(printedTotal)
        val itemLines = productSection(lines)
        val items = itemLines.mapNotNull { parseItemLine(it, itemCeiling) }
            .distinctBy { Triple(normalizeComparable(it.name), it.quantity, it.lineTotalCents) }
        val merchant = detectMerchant(lines)
        val purchaseDate = detectDate(lines, now)
        val structuralConfidence = when {
            items.isNotEmpty() && subtotal != null && abs((subtotal - items.sumOf { it.lineTotalCents })) <= 200 -> .94f
            items.isNotEmpty() -> .84f
            else -> .66f
        }
        return DetailedReceiptDraft(
            merchant = merchant,
            totalCents = printedTotal,
            printedTotalCents = printedTotal,
            subtotalCents = subtotal,
            discountCents = discount,
            savingsCents = savings,
            taxCents = tax,
            feeCents = fee,
            tipCents = tip,
            roundingCents = rounding,
            purchaseTimestamp = purchaseDate,
            items = items,
            pages = pages,
            rawText = text,
            confidence = structuralConfidence,
        )
    }

    /**
     * Select only the body of a structured product table when one exists. This is the most
     * important protection against HSN codes, GST breakup rows, tendered cash and change being
     * mistaken for products. Unstructured restaurant and delivery receipts keep the generic path.
     */
    private fun productSection(lines: List<String>): List<String> {
        val header = lines.indexOfFirst(ITEM_TABLE_HEADER::containsMatchIn)
        if (header < 0) return lines
        val end = ((header + 1) until lines.size).firstOrNull { index ->
            ITEM_SUMMARY.containsMatchIn(lines[index]) ||
                GST_BREAKUP_LABEL.containsMatchIn(lines[index]) ||
                PAYMENT_SECTION_LABEL.containsMatchIn(lines[index])
        } ?: lines.size
        return lines.subList((header + 1).coerceAtMost(lines.size), end)
    }

    private fun detectPrintedTotal(lines: List<String>, gst: GstBreakup?): Long? {
        // D'Mart/Reliance-style receipts print `Items: 12 Qty: 14 993.00`. This is a stronger
        // signal than any larger HSN, taxable, cash-tendered, balance or savings number.
        lines.forEachIndexed { index, line ->
            if (!ITEM_SUMMARY.containsMatchIn(line)) return@forEachIndexed
            val amounts = MONEY.findAll(line).mapNotNull { it.groupValues.getOrNull(1)?.moneyToCents() }.toList()
            // The first two values are the item count and aggregate quantity. A third value is
            // the printed amount. Never promote `Qty: 14` into a ₹14 receipt.
            amounts.getOrNull(2)?.takeIf { it > 0 }?.let { return it }
            lines.drop(index + 1).take(2).firstNotNullOfOrNull { candidate ->
                STANDALONE_AMOUNT.matchEntire(candidate)?.groupValues?.getOrNull(1)?.moneyToCents()
            }?.takeIf { it > 0 }?.let { return it }
        }

        lines.filter { STRONG_TOTAL_LABELS.containsMatchIn(it) && !GST_BREAKUP_LABEL.containsMatchIn(it) }
            .mapNotNull(::lastAmount).lastOrNull()?.takeIf { it > 0 }?.let { return it }

        gst?.totalCents?.takeIf { it > 0 }?.let { return it }

        return lines.takeLast(18)
            .filterNot { TENDER_LABELS.containsMatchIn(it) || SAVINGS_LABELS.containsMatchIn(it) || GST_BREAKUP_LABEL.containsMatchIn(it) }
            .mapNotNull(::lastAmount)
            .filter { it > 0 }
            .lastOrNull()
    }

    /** Parse the totals row of an Indian GST breakup table: taxable, CGST, SGST, CESS, total. */
    private fun parseGstBreakup(lines: List<String>): GstBreakup? {
        val start = lines.indexOfFirst(GST_BREAKUP_LABEL::containsMatchIn)
        if (start < 0) return null
        val end = ((start + 1) until lines.size).firstOrNull { PAYMENT_SECTION_LABEL.containsMatchIn(lines[it]) } ?: lines.size
        val table = lines.subList(start + 1, end)
        val totalsIndex = table.indexOfLast(GST_TOTAL_ROW::containsMatchIn)
        if (totalsIndex >= 0) {
            // OCR engines occasionally break a wide GST total row across two visual lines.
            val totalsLine = table[totalsIndex]
            val directAmounts = monetaryColumns(totalsLine, dropLeadingIndex = false)
            val amounts = if (directAmounts.size >= 2) directAmounts else {
                table.drop(totalsIndex).take(2).let { monetaryColumns(it.joinToString(" "), dropLeadingIndex = false) }
            }
            if (amounts.size >= 2) {
                val taxable = amounts.first()
                val total = amounts.last()
                val tax = amounts.drop(1).dropLast(1).sum()
                return GstBreakup(taxable, tax, total)
            }
        }

        // Fallback for damaged `T:` rows: sum numbered GST-band rows. The band number is a row
        // index, not money, and is removed before taxable/CGST/SGST/total are interpreted.
        val bands = table.mapNotNull { row ->
            val values = monetaryColumns(row, dropLeadingIndex = true)
            values.takeIf { it.size >= 2 }
        }
        if (bands.isEmpty()) return null
        return GstBreakup(
            taxableCents = bands.sumOf { it.first() },
            taxCents = bands.sumOf { it.drop(1).dropLast(1).sum() },
            totalCents = bands.sumOf { it.last() },
        )
    }

    private fun monetaryColumns(line: String, dropLeadingIndex: Boolean): List<Long> {
        val values = MONEY.findAll(line).mapNotNull { it.groupValues.getOrNull(1)?.moneyToCents() }.toMutableList()
        if (dropLeadingIndex && values.size >= 3 && GST_BAND_ROW.containsMatchIn(line)) values.removeAt(0)
        return values
    }

    fun allocateTotal(items: List<ReceiptLineDraft>, receiptTotalCents: Long): List<Long> {
        if (items.isEmpty()) return emptyList()
        val base = items.sumOf { it.lineTotalCents }.coerceAtLeast(1L)
        var remaining = receiptTotalCents
        return items.mapIndexed { index, item ->
            val amount = if (index == items.lastIndex) remaining else
                ((receiptTotalCents.toDouble() * item.lineTotalCents / base).roundToLong()).coerceAtLeast(0L)
            remaining -= amount
            amount
        }
    }

    private fun parseItemLine(source: String, receiptCeilingCents: Long): ReceiptLineDraft? {
        val line = repairCommonOcr(source)
        if (NON_ITEM_LABELS.containsMatchIn(line) || line.length < 4) return null
        val amountMatch = MONEY.findAll(line).lastOrNull() ?: return null
        val total = amountMatch.groupValues[1].moneyToCents() ?: return null
        if (total <= 0L || total > (receiptCeilingCents * 1.05).roundToLong().coerceAtLeast(receiptCeilingCents + 100)) return null
        val prefix = line.substring(0, amountMatch.range.first).trim(' ', '-', ':', '|')
        if (prefix.none(Char::isLetter) || prefix.length < 2) return null
        val quantityMatch = QUANTITY.find(prefix)
        val columnsMatch = if (quantityMatch == null) INDIAN_ITEM_COLUMNS.matchEntire(prefix) else null
        val quantity = (quantityMatch?.groupValues?.getOrNull(1) ?: columnsMatch?.groupValues?.getOrNull(2))
            ?.toDoubleOrNull()?.takeIf { it in .01..999.0 } ?: 1.0
        val unitPrice = (quantityMatch?.groupValues?.getOrNull(2) ?: columnsMatch?.groupValues?.getOrNull(3))?.moneyToCents()
        val namePrefix = columnsMatch?.groupValues?.getOrNull(1) ?: prefix
        val rawName = namePrefix
            .replace(QUANTITY, " ")
            .replace(LEADING_CODES, " ")
            .replace(TRAILING_ITEM_CODES, " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', '|')
        val name = normalizeProductName(rawName)
        if (name.length !in 2..72 || name.all(Char::isDigit)) return null
        return ReceiptLineDraft(
            name = name,
            quantity = quantity,
            unitPriceCents = unitPrice ?: if (quantity != 1.0) (total / quantity).roundToLong() else total,
            lineTotalCents = total,
            categoryHint = categoryHint(name),
            confidence = when {
                quantityMatch != null -> .94f
                columnsMatch != null -> .89f
                else -> .70f
            },
        )
    }

    private fun detectMerchant(lines: List<String>): String = lines.take(10).firstNotNullOfOrNull { line ->
        if (line.length !in 3..55 || line.none(Char::isLetter) || HEADER_NOISE.containsMatchIn(line) || MONEY.containsMatchIn(line)) null
        else MerchantNormalizer.normalize(line).normalized.takeUnless { it == "Unknown merchant" }
    } ?: "Receipt purchase"

    private fun detectDate(lines: List<String>, fallback: Long): Long {
        val raw = lines.firstNotNullOfOrNull { DATE.find(it)?.value } ?: return fallback
        DATE_FORMATS.forEach { pattern ->
            runCatching { SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(raw)?.time }
                .getOrNull()?.let { return it }
        }
        return fallback
    }

    private fun findLabeledAmount(lines: List<String>, labels: Regex, preferLast: Boolean): Long? {
        val matches = lines.filter(labels::containsMatchIn).mapNotNull(::lastAmount)
        return if (preferLast) matches.lastOrNull() else matches.firstOrNull()
    }

    private fun lastAmount(line: String): Long? = MONEY.findAll(line).lastOrNull()?.groupValues?.getOrNull(1)?.moneyToCents()
    /** Tax rate rows such as `CGST 2.5% SGST 2.5%` are not monetary GST amounts. */
    private fun taxAmount(line: String): Long? {
        val repaired = repairCommonOcr(line)
        // Strip percentage tokens before applying the deliberately permissive money regex. This
        // also prevents a noisy OCR token such as `2.50 %` from being read as a ₹2.50 tax amount.
        val withoutRates = repaired
            .replace(Regex("""^\s*\d+\s*[).:-]\s*"""), " ")
            .replace(Regex("""\b\d+(?:\.\d+)?\s*%"""), " ")
        val monetaryAmounts = MONEY.findAll(withoutRates).toList()
        return monetaryAmounts.lastOrNull()?.groupValues?.getOrNull(1)?.moneyToCents()
    }
    private fun signedLastAmount(line: String): Long? {
        val match = MONEY.findAll(line).lastOrNull() ?: return null
        val cents = match.groupValues[1].moneyToCents() ?: return null
        return if (line.substring(0, match.range.first).trimEnd().endsWith("-")) -cents else cents
    }
    private fun String.moneyToCents(): Long? = replace(",", "").toDoubleOrNull()?.let { (it * 100).roundToLong() }
    private fun cleanLine(value: String) = value.replace(Regex("""\s+"""), " ").trim()
    private fun repairCommonOcr(value: String): String = value
        .replace(Regex("""(?i)\bfss?a[iy]\b"""), "FSSAI")
        .replace(Regex("""(?i)\bphane\b"""), "Phone")
        .replace(Regex("""(?i)\bcest\b"""), "CGST")
        .replace(Regex("""(?i)\bsst\b"""), "SGST")
        .replace(Regex("""(?i)\bsayed\b"""), "Saved")

    private fun normalizeProductName(value: String): String {
        val cleaned = value
            .replace(Regex("""(?i)\bkrac(?:h|k)?j[a-z]*\b"""), "Krackjack")
            .replace(Regex("""(?i)\bhooern\s+h?ilk\s+plus\b"""), "Horlicks Milk Plus")
            .replace(Regex("""[^\p{L}\p{N}&+.'/% -]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':', '|')
        return cleaned.lowercase(Locale.ENGLISH).split(' ').joinToString(" ") { token ->
            if (token.length <= 3 && token.any(Char::isUpperCase)) token else token.replaceFirstChar(Char::titlecase)
        }.trim()
    }
    private fun normalizeComparable(value: String) = value.lowercase().replace(Regex("""[^a-z0-9]"""), "")
    private fun lineSimilarity(a: String, b: String): Float {
        val left = normalizeComparable(a)
        val right = normalizeComparable(b)
        if (left.isBlank() || right.isBlank()) return 0f
        if (left == right || left.contains(right) || right.contains(left)) return 1f
        val leftTokens = a.lowercase().split(Regex("""\W+""")).filter(String::isNotBlank).toSet()
        val rightTokens = b.lowercase().split(Regex("""\W+""")).filter(String::isNotBlank).toSet()
        return leftTokens.intersect(rightTokens).size.toFloat() / leftTokens.union(rightTokens).size.coerceAtLeast(1)
    }

    private fun categoryHint(name: String): String? {
        val value = name.lowercase()
        return when {
            listOf("milk", "rice", "atta", "dal", "chips", "kurkure", "biscuit", "vegetable", "fruit", "grocery").any(value::contains) -> "groceries"
            listOf("medicine", "tablet", "capsule", "pharma").any(value::contains) -> "healthcare"
            listOf("burger", "pizza", "coffee", "tea", "meal", "dining").any(value::contains) -> "food"
            listOf("shampoo", "soap", "skincare", "cream").any(value::contains) -> "personal care"
            listOf("notebook", "pen", "stationery", "book").any(value::contains) -> "education"
            else -> null
        }
    }

    private val MONEY = Regex("""(?:₹|Rs\.?|INR)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\b""", RegexOption.IGNORE_CASE)
    private val STANDALONE_AMOUNT = Regex("""(?i)^\s*(?:₹|Rs\.?|INR)?\s*([0-9][0-9,]*(?:\.[0-9]{1,2})?)\s*$""")
    private val QUANTITY = Regex("""(?i)\b(\d+(?:\.\d+)?)\s*(?:pcs?|nos?|n|kg|g|l|ml)?\s*(?:x|×|@)\s*(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d{1,2})?)""")
    private val INDIAN_ITEM_COLUMNS = Regex("""(?i)^(.+?)\s+(\d+(?:\.\d+)?)\s*(?:pcs?|nos?|n|kg|g|l|ml)?\s+(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d{1,2})?)$""")
    private val LEADING_CODES = Regex("""(?i)^\s*(?:\d{4,}|[A-Z]{1,4}\d{3,}|HSN\s*\d+)\s+""")
    private val TRAILING_ITEM_CODES = Regex("""(?i)\s+(?:HSN|SAC|MRP|BATCH)\s*[:#-]?\s*[A-Z0-9.-]+\s*$""")
    private val TOTAL_LABELS = Regex("""(?i)\b(?:grand\s*total|net\s*(?:amount|payable)|amount\s*(?:paid|payable|due)|bill\s*total|total)\b""")
    private val STRONG_TOTAL_LABELS = Regex("""(?i)\b(?:grand\s*total|net\s*(?:amount|payable)|amount\s*(?:paid|payable|due)|bill\s*total|order\s*total|total\s*(?:payable|due))\b""")
    private val SUBTOTAL_LABELS = Regex("""(?i)\b(?:sub\s*total|item\s*total|gross\s*amount|taxable\s*value)\b""")
    private val DISCOUNT_LABELS = Regex("""(?i)\b(?:discount|coupon|promo|offer)\b""")
    private val SAVINGS_LABELS = Regex("""(?i)\b(?:you\s*)?sav(?:e|ed|ing|ings)\b""")
    private val TAX_LABELS = Regex("""(?i)\b(?:gst|c(?:g|6|e)?s(?:t|1)|s(?:g|6|e)?s(?:t|1)|igst|vat|tax)\b""")
    private val FEE_LABELS = Regex("""(?i)\b(?:service|delivery|packaging|packing|handling|convenience)\s*(?:charge|fee)?\b""")
    private val TIP_LABELS = Regex("""(?i)\b(?:tip|gratuity)\b""")
    private val ROUNDING_LABELS = Regex("""(?i)\b(?:round(?:ing)?\s*off|roundoff)\b""")
    private val NON_ITEM_LABELS = Regex("""(?i)\b(?:sub\s*total|item\s*total|grand\s*total|net\s*amount|amount\s*(?:paid|payable|due)|discount|coupon|sav(?:e|ed|ing|ings)|gst|c(?:g|6|e)?s(?:t|1)|s(?:g|6|e)?s(?:t|1)|igst|vat|tax|service\s*charge|delivery\s*fee|packaging|packing|tip|gratuity|round(?:ing)?\s*off|balance|change|cash|card|upi|gstin|invoice|receipt|phone|fssai|hsn|sac|mrp|batch|expiry|thank\s*you)\b""")
    private val HEADER_NOISE = Regex("""(?i)\b(?:tax\s*invoice|receipt|bill|gstin|phone|mobile|fssai|date|time|cashier|thank|welcome|address)\b""")
    private val ITEM_TABLE_HEADER = Regex("""(?i)(?:\bparticulars?\b|\bdescription\b|\bitem\b).*(?:\bqty\b|quantity).*(?:\brate\b|price|value|amount)""")
    private val ITEM_SUMMARY = Regex("""(?i)\bitems?\s*[:.-]?\s*\d+.*\bqty\s*[:.-]?\s*\d+""")
    private val GST_BREAKUP_LABEL = Regex("""(?i)\bgst\s*(?:break\s*up|breakup|summary|details?)\b""")
    private val GST_TOTAL_ROW = Regex("""(?i)^\s*(?:t|total)\s*[:;.-]?\s+""")
    private val GST_BAND_ROW = Regex("""^\s*\d{1,2}\s+""")
    private val PAYMENT_SECTION_LABEL = Regex("""(?i)\b(?:amount\s+received|payment\s+details?|tender|paid\s+(?:by|in)|balance\s+paid)\b""")
    private val TENDER_LABELS = Regex("""(?i)\b(?:cash|tender(?:ed)?|amount\s+received|balance|change|paid\s+(?:by|in))\b""")
    private val DATE = Regex("""\b(?:\d{1,2}[/.-]\d{1,2}[/.-]\d{2,4}|\d{1,2}[- ](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[- ]\d{2,4})\b""", RegexOption.IGNORE_CASE)
    private val DATE_FORMATS = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "dd/MM/yy", "dd-MM-yy", "dd.MM.yy", "dd MMM yyyy", "dd-MMM-yyyy", "dd MMM yy", "dd-MMM-yy")
}
