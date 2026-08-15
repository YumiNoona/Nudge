package com.nudge.android.importer

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.core.graphics.createBitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.nudge.engine.FinancialEventClassifier
import com.nudge.engine.MerchantNormalizer
import com.nudge.model.TransactionType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.roundToLong

data class StatementDraft(
    val amountCents: Long,
    val type: TransactionType,
    val merchant: String,
    val timestampEpoch: Long,
)

data class ReceiptDraft(val amountCents: Long, val merchant: String)

data class DocumentReadResult(val text: String, val warning: String? = null)

class DocumentReadException(message: String, cause: Throwable? = null) : IllegalArgumentException(message, cause)

object FinancialDocumentImporter {
    suspend fun extractText(context: Context, uri: Uri): String = readDocument(context, uri).text

    suspend fun readDocument(context: Context, uri: Uri): DocumentReadResult = withContext(Dispatchers.IO) {
        val mime = context.contentResolver.getType(uri).orEmpty()
        val path = uri.toString()
        when {
            mime == "application/pdf" || path.endsWith(".pdf", true) -> {
                val text = try {
                    extractPdfText(context, uri)
                } catch (error: SecurityException) {
                    throw DocumentReadException("This PDF is password-protected. Export an unlocked copy and try again.", error)
                } catch (error: IllegalStateException) {
                    val message = error.message.orEmpty()
                    if (message.contains("password", true) || message.contains("encrypted", true)) {
                        throw DocumentReadException("This PDF is password-protected. Export an unlocked copy and try again.", error)
                    }
                    throw DocumentReadException("Nudge could not open this PDF. It may be damaged or use an unsupported format.", error)
                } catch (error: Exception) {
                    val message = error.message.orEmpty()
                    if (message.contains("password", true) || message.contains("encrypted", true)) {
                        throw DocumentReadException("This PDF is password-protected. Export an unlocked copy and try again.", error)
                    }
                    throw DocumentReadException("Nudge could not open this PDF. It may be password-protected or damaged.", error)
                }
                if (text.isBlank()) throw DocumentReadException("No readable text was found in this PDF. Try a clearer export or import page images instead.")
                DocumentReadResult(text)
            }
            mime.startsWith("image/") || IMAGE_SUFFIXES.any { path.endsWith(it, true) } -> readImage(context, uri)
            mime.startsWith("text/") || mime in TABULAR_MIMES || TEXT_SUFFIXES.any { path.endsWith(it, true) } -> {
                val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?: throw DocumentReadException("Nudge could not read this file. Choose it again from the system picker.")
                if (text.isBlank()) throw DocumentReadException("This file is empty. Choose a statement that contains transaction rows.")
                DocumentReadResult(text)
            }
            else -> throw DocumentReadException("This file type is not supported yet. Choose a PDF, CSV, TXT, JPG, PNG or WebP file.")
        }
    }

    private suspend fun readImage(context: Context, uri: Uri): DocumentReadResult {
        val quality = inspectImageQuality(context, uri)
        val text = runCatching { recognizeUriLayout(context, uri) }
            .getOrElse { throw DocumentReadException("Nudge could not open this image. Choose the original image and try again.", it) }
            .trim()
        if (text.length < 18) {
            val message = if (quality.isBlurry || quality.isLowResolution) {
                "This image is too blurry or low-resolution to read. Retake it in good light and keep the text in focus."
            } else {
                "No readable transaction text was found. Make sure the amount, merchant and date are visible."
            }
            throw DocumentReadException(message)
        }
        val warning = when {
            quality.isBlurry -> "The image looks slightly blurry. Check every detected amount before importing."
            quality.isLowResolution -> "This image is low-resolution. Check every detected amount before importing."
            else -> null
        }
        return DocumentReadResult(text, warning)
    }

    private fun inspectImageQuality(context: Context, uri: Uri): ImageQuality {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return ImageQuality(isBlurry = false, isLowResolution = true)
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / sample > 1_200) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        val bitmap = context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
            ?: return ImageQuality(isBlurry = false, isLowResolution = true)
        return try {
            val step = maxOf(2, minOf(bitmap.width, bitmap.height) / 180)
            var difference = 0L
            var samples = 0L
            var y = step
            while (y < bitmap.height - step) {
                var x = step
                while (x < bitmap.width - step) {
                    val center = bitmap.getPixel(x, y).luma()
                    difference += kotlin.math.abs(center - bitmap.getPixel(x + step, y).luma())
                    difference += kotlin.math.abs(center - bitmap.getPixel(x, y + step).luma())
                    samples += 2
                    x += step
                }
                y += step
            }
            val focusScore = if (samples == 0L) 100.0 else difference.toDouble() / samples
            ImageQuality(
                isBlurry = focusScore < 7.0,
                isLowResolution = minOf(bounds.outWidth, bounds.outHeight) < 540,
            )
        } finally {
            bitmap.recycle()
        }
    }

    private fun Int.luma(): Int {
        val red = this shr 16 and 0xFF
        val green = this shr 8 and 0xFF
        val blue = this and 0xFF
        return (red * 299 + green * 587 + blue * 114) / 1_000
    }

    fun parseStatement(text: String, now: Long = System.currentTimeMillis()): List<StatementDraft> {
        val lines = text.lineSequence().map(String::trim).filter(String::isNotBlank).toList()
        if (lines.isEmpty()) return emptyList()
        val csvRows = lines.map(::parseCsvRow)
        val headerIndex = csvRows.indexOfFirst { row -> row.any { normalizeHeader(it) in knownHeaders } }
        val parsed = if (headerIndex >= 0) {
            parseTabular(csvRows.drop(headerIndex + 1), csvRows[headerIndex], now)
        } else {
            parseLooseRows(lines, now)
        }
        return parsed
            .filter { it.amountCents > 0 && it.merchant.isNotBlank() }
            .distinctBy { Triple(it.amountCents, it.type, "${it.merchant.lowercase()}-${it.timestampEpoch / DAY_MS}") }
    }

    fun parseReceipt(text: String): ReceiptDraft? {
        val lines = text.lineSequence().map { it.trim() }.filter { it.isNotBlank() }.toList()
        val totalPatterns = listOf(
            Regex("""(?i)\b(?:grand\s+total|amount\s+paid|net\s+amount|total\s+due|total)\b\D{0,16}(?:₹|rs\.?|inr)?\s*([\d,]+(?:\.\d{1,2})?)"""),
            Regex("""(?i)(?:₹|rs\.?|inr)\s*([\d,]+(?:\.\d{1,2})?)"""),
        )
        val amount = totalPatterns.firstNotNullOfOrNull { pattern ->
            lines.asReversed().firstNotNullOfOrNull { line -> pattern.find(line)?.groupValues?.getOrNull(1)?.toCents() }
        } ?: return null
        val merchant = lines.firstOrNull { line ->
            line.length in 3..48 && line.any(Char::isLetter) &&
                !Regex("""(?i)tax\s+invoice|receipt|bill|gstin|phone|mobile|date|time|table|cashier|thank|welcome""").containsMatchIn(line) &&
                !Regex("""^[\d\W]+$""").matches(line)
        }?.let { MerchantNormalizer.normalize(it).normalized }
            ?.takeUnless { it == "Unknown merchant" }
            ?: "Receipt purchase"
        return ReceiptDraft(amount, merchant)
    }

    suspend fun recognizeBitmap(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
        } finally {
            recognizer.close()
        }
    }

    private suspend fun recognizeBitmapLayout(bitmap: Bitmap): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            layoutText(recognizer.process(InputImage.fromBitmap(bitmap, 0)).await())
        } finally {
            recognizer.close()
        }
    }

    private suspend fun recognizeUriLayout(context: Context, uri: Uri): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            layoutText(recognizer.process(InputImage.fromFilePath(context, uri)).await())
        } finally {
            recognizer.close()
        }
    }

    private fun layoutText(result: Text): String {
        // Rebuild rows from OCR elements rather than ML Kit's paragraph lines. Indian
        // supermarket receipts are narrow tables; line-level OCR frequently joins a GST
        // cell to the next product or separates quantity/rate/amount into unrelated rows.
        val lines = result.textBlocks.flatMap { block -> block.lines }
            .flatMap { line -> line.elements }
            .mapNotNull { element ->
            val box = element.boundingBox ?: return@mapNotNull null
            LayoutText(element.text, box.left, box.centerY(), box.height())
        }.sortedBy { it.centerY }
        val rows = mutableListOf<MutableList<LayoutText>>()
        lines.forEach { item ->
            val row = rows.lastOrNull()
            val rowCenter = row?.map { it.centerY }?.average()
            val tolerance = maxOf(12.0, item.height * .62)
            if (row != null && rowCenter != null && kotlin.math.abs(item.centerY - rowCenter) <= tolerance) row += item
            else rows += mutableListOf(item)
        }
        return rows.joinToString("\n") { row -> row.sortedBy { it.left }.joinToString(" ") { it.text } }
    }

    suspend fun recognizeUri(context: Context, uri: Uri): String {
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            recognizer.process(InputImage.fromFilePath(context, uri)).await().text
        } finally {
            recognizer.close()
        }
    }

    private suspend fun extractPdfText(context: Context, uri: Uri): String {
        val descriptor = context.contentResolver.openFileDescriptor(uri, "r")
            ?: error("Unable to open the PDF")
        descriptor.use { pfd ->
            PdfRenderer(pfd).use { renderer ->
                // Full-year statements often exceed 30 pages. Rendering one page at a
                // time keeps memory bounded, so every page can be processed safely.
                val pages = renderer.pageCount
                val output = StringBuilder()
                for (index in 0 until pages) {
                    renderer.openPage(index).use { page ->
                        val maxWidth = 1800
                        val scale = (maxWidth.toFloat() / page.width).coerceAtMost(2f).coerceAtLeast(1f)
                        val bitmap = createBitmap(
                            (page.width * scale).toInt(),
                            (page.height * scale).toInt(),
                            Bitmap.Config.ARGB_8888,
                        )
                        try {
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            // ML Kit's plain text can be column-major for statements. Rebuild
                            // visual rows from bounding boxes before parsing the table.
                            output.append(recognizeBitmapLayout(bitmap)).append('\n')
                        } finally {
                            bitmap.recycle()
                        }
                    }
                }
                return output.toString()
            }
        }
    }

    private fun parseTabular(rows: List<List<String>>, header: List<String>, now: Long): List<StatementDraft> {
        val normalized = header.map(::normalizeHeader)
        fun index(vararg names: String) = normalized.indexOfFirst { cell -> names.any { cell == it || cell.contains(it) } }
        val dateIndex = index("date", "transactiondate", "valuedate")
        val merchantIndex = index("description", "narration", "particulars", "remarks", "transactiondetails", "merchant")
        val debitIndex = index("debit", "withdrawal", "dramount")
        val creditIndex = index("credit", "deposit", "cramount")
        val amountIndex = index("amount", "transactionamount")
        val typeIndex = index("type", "drcr", "debitcredit")
        return rows.mapNotNull { row ->
            val debit = row.getOrNull(debitIndex)?.toCents()
            val credit = row.getOrNull(creditIndex)?.toCents()
            val rawAmountCell = row.getOrNull(amountIndex).orEmpty()
            val rawAmount = rawAmountCell.toCents()
            val typeCell = row.getOrNull(typeIndex).orEmpty().lowercase()
            val columnType = when {
                debit != null && debit > 0 -> TransactionType.DEBIT
                credit != null && credit > 0 -> TransactionType.CREDIT
                typeCell.contains("cr") || typeCell.contains("credit") -> TransactionType.CREDIT
                typeCell.contains("dr") || typeCell.contains("debit") -> TransactionType.DEBIT
                rawAmountCell.hasDebitSign() -> TransactionType.DEBIT
                rawAmountCell.hasCreditSign() -> TransactionType.CREDIT
                else -> null
            }
            val rawMerchant = row.getOrNull(merchantIndex).orEmpty()
            val type = statementSemanticType(rawMerchant, columnType) ?: return@mapNotNull null
            val amount = when (type) {
                TransactionType.CREDIT, TransactionType.REFUND -> credit ?: rawAmount
                TransactionType.TRANSFER -> credit ?: debit ?: rawAmount
                TransactionType.DEBIT -> debit ?: rawAmount
            } ?: return@mapNotNull null
            if (isSummaryLine(rawMerchant)) return@mapNotNull null
            val merchant = if (type == TransactionType.TRANSFER && rawMerchant.contains("payment received", true)) {
                "Credit Card Payment"
            } else cleanNarration(rawMerchant)
            StatementDraft(amount, type, merchant, parseDate(row.getOrNull(dateIndex), now))
        }
    }

    private fun parseLooseRows(lines: List<String>, now: Long): List<StatementDraft> {
        val drafts = mutableListOf<StatementDraft>()
        var columnHint: TransactionType? = null
        var rowHint: TransactionType? = null
        var pendingRow: StringBuilder? = null

        fun flushRow() {
            val row = pendingRow?.toString()?.trim().orEmpty()
            if (row.isNotBlank()) parseLooseLine(row, now, rowHint)?.let(drafts::add)
            pendingRow = null
            rowHint = null
        }

        lines.forEach { line ->
            val lineHint = when {
                DEBIT_ROW_MARKER.containsMatchIn(line) -> TransactionType.DEBIT
                CREDIT_ROW_MARKER.containsMatchIn(line) -> TransactionType.CREDIT
                else -> null
            }
            val startsTransaction = DATE_PREFIX.containsMatchIn(line)
            when {
                startsTransaction -> {
                    flushRow()
                    rowHint = lineHint ?: columnHint
                    columnHint = null
                    pendingRow = StringBuilder(line)
                }
                lineHint != null -> {
                    flushRow()
                    columnHint = lineHint
                }
                pendingRow != null && !isPageFurniture(line) -> pendingRow?.append(' ')?.append(line)
            }
        }
        flushRow()
        return drafts
    }

    private fun parseLooseLine(line: String, now: Long, columnHint: TransactionType? = null): StatementDraft? {
        if (isSummaryLine(line)) return null
        val dateMatches = DATE_PREFIX.findAll(line).toList()
        val valueDate = dateMatches.firstOrNull() ?: return null
        val narrationStart = dateMatches.last().range.last + 1
        val candidates = AMOUNT.findAll(line)
            .filter { match -> dateMatches.none { date -> match.range.first <= date.range.last && match.range.last >= date.range.first } }
            .filter { it.range.first >= narrationStart }
            .toList()
        // Prefer currency-shaped values. This prevents the repeated post date, UPI
        // reference, cheque number and branch code from becoming transaction amounts.
        val formatted = candidates.filter { match ->
            val numeric = match.groupValues[1]
            numeric.contains('.') || numeric.contains(',') ||
                match.value.contains("Rs", true) || match.value.contains("INR", true) || match.value.contains('₹')
        }
        val amountMatches = if (formatted.isNotEmpty()) formatted else candidates.filter { match ->
            val digits = match.groupValues[1].filter(Char::isDigit)
            digits.length in 1..7 && line.getOrNull(match.range.first - 1) !in listOf('/', ':')
        }
        if (amountMatches.isEmpty()) return null
        val lowered = line.lowercase()
        val columnType = when {
            Regex("""\bcr\b|credited|\bdep\s+tfr\b|deposit""").containsMatchIn(lowered) -> TransactionType.CREDIT
            Regex("""\bdr\b|debited|\bwdl\s+tfr\b|spent|purchase|withdraw""").containsMatchIn(lowered) -> TransactionType.DEBIT
            else -> columnHint
        }
        val type = statementSemanticType(line, columnType) ?: return null
        // The final monetary value is the running balance; the first credible value
        // following the narration is the row's debit or credit movement.
        val amount = amountMatches.first().groupValues[1].toCents() ?: return null
        val merchantPart = line.substring(narrationStart, amountMatches.first().range.first).trim(' ', ',', '|', '-')
        return StatementDraft(amount, type, cleanStatementNarration(merchantPart, line), parseDate(valueDate.value, now))
    }

    private fun cleanStatementNarration(raw: String, fullRow: String): String {
        val compactReferences = fullRow.replace(Regex("""(?<=\d)\s+(?=\d)"""), "")
        val candidate = UPI_MERCHANT.find(fullRow)?.groupValues?.getOrNull(1)
            ?: FLEX_UPI_MERCHANT.find(compactReferences)?.groupValues?.getOrNull(1)
            ?: NEFT_MERCHANT.find(fullRow)?.groupValues?.getOrNull(1)
            ?: POS_MERCHANT.find(fullRow)?.groupValues?.getOrNull(1)
            ?: REFERENCE_MERCHANT.find(compactReferences)?.groupValues?.getOrNull(1)
            ?: raw
        val cleaned = candidate
            // Anything after the first currency-shaped value belongs to amount,
            // balance or bank columns rather than the merchant/person name.
            .replace(Regex("""(?i)\s+(?=(?:₹|Rs\.?|INR)?\s*\d[\d,]*\.\d{1,2}\b).*$"""), "")
            .replace(Regex("""(?i)\s+(?:-\s*)+(?=(?:₹|Rs\.?|INR)?\s*\d).*$"""), "")
            .replace(Regex("""(?i)\s+AT\s+\d+.*$"""), "")
            .replace(Regex("""(?i)^\s*(?:(?:UPI?|UPV?)\s*[/| -]?\s*(?:DR|CR)|WDL\s+TFR|DEP\s+TFR)\b"""), "")
            .replace(Regex("""\b\d[\d,.:/-]{2,}\b"""), " ")
            .replace(Regex("""(?i)\b(?:YESB|SBIN|CBIN|HDFC|HDFCBK|ICIC|ICICI|UTIB|PUNB|PAYTM|AXIS|BANK)\b\s*$"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', '/', '|', '*')
        return cleanNarration(cleaned).ifBlank { "Unknown merchant" }
    }

    private fun isPageFurniture(line: String): Boolean = Regex(
        """(?i)^\s*(?:page\s+no\.?\s*\d+|balance|value\s+date|post\s+date|details|ref\s+no|cheque\s+no)\s*$""",
    ).matches(line)

    private fun cleanNarration(raw: String): String {
        var result = raw.replace(Regex("""(?i)\b(?:upi|pos|imps|neft|rtgs|ach|ecs)[-/ ]*[a-z0-9@._-]*"""), " ")
        result = result.replace(Regex("""(?i)\b(?:ref|utr|txn|transaction|rrn)\s*[:#-]?\s*[a-z0-9-]+.*$"""), "")
        result = result.replace(Regex("""[/|_-]+"""), " ").replace(Regex("""\s+"""), " ").trim()
        return MerchantNormalizer.normalize(result.ifBlank { raw }).normalized
    }

    private fun isSummaryLine(text: String): Boolean = Regex(
        """(?i)opening\s+balance|closing\s+balance|available\s+balance|total\s+(?:debit|credit)|amount\s+due|minimum\s+due|statement\s+summary""",
    ).containsMatchIn(text)

    private fun statementSemanticType(narration: String, columnType: TransactionType?): TransactionType? {
        val lowered = narration.lowercase()
        return when {
            lowered.contains("refund") || lowered.contains("reversal") || lowered.contains("reversed") -> TransactionType.REFUND
            lowered.contains("payment received") &&
                (lowered.contains("thank") || lowered.contains("credit card") || lowered.contains("card payment")) -> TransactionType.TRANSFER
            lowered.contains("credit card payment") || lowered.contains("card bill payment") -> TransactionType.TRANSFER
            // Debit/credit columns and explicit CR/DR markers are authoritative.
            // Only ask the sentence classifier when a statement has no direction
            // column; never silently turn an unsigned amount into an expense.
            else -> columnType ?: FinancialEventClassifier.classify(narration)?.type
        }
    }

    private fun parseDate(raw: String?, fallback: Long): Long {
        val value = raw?.trim().orEmpty()
            .replace(Regex("""\s*([./-])\s*"""), "$1")
            .replace(Regex("""\s+"""), " ")
        for (pattern in DATE_FORMATS) {
            val parsed = runCatching { SimpleDateFormat(pattern, Locale.ENGLISH).apply { isLenient = false }.parse(value) }.getOrNull()
            if (parsed != null) return parsed.time
        }
        return fallback
    }

    private fun parseCsvRow(line: String): List<String> {
        val delimiter = if (line.count { it == '\t' } > line.count { it == ',' }) '\t' else ','
        val output = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        for (char in line) {
            when {
                char == '"' -> quoted = !quoted
                char == delimiter && !quoted -> { output += cell.toString().trim(); cell.clear() }
                else -> cell.append(char)
            }
        }
        output += cell.toString().trim()
        return output
    }

    private fun normalizeHeader(value: String) = value.lowercase().replace(Regex("""[^a-z]"""), "")
    private fun String.toCents(): Long? {
        val cleaned = replace(",", "").replace(Regex("""(?i)₹|rs\.?|inr|cr|dr"""), "").trim()
        return cleaned.toDoubleOrNull()?.let { (kotlin.math.abs(it) * 100).roundToLong() }?.takeIf { it > 0 }
    }

    private fun String.hasDebitSign(): Boolean {
        val value = trim()
        return value.startsWith("-") || Regex("""(?i)\bdr\s*$""").containsMatchIn(value)
    }

    private fun String.hasCreditSign(): Boolean {
        val value = trim()
        return value.startsWith("+") || Regex("""(?i)\bcr\s*$""").containsMatchIn(value)
    }

    private suspend fun <T> Task<T>.await(): T = suspendCoroutine { continuation ->
        addOnSuccessListener { continuation.resume(it) }
        addOnFailureListener { continuation.resumeWithException(it) }
    }

    private val knownHeaders = setOf("date", "transactiondate", "valuedate", "description", "narration", "particulars", "debit", "credit", "withdrawal", "deposit", "amount")
    private val DATE_PREFIX = Regex("""\b(?:\d{1,2}\s*[./-]\s*\d{1,2}\s*[./-]\s*\d{2,4}|\d{1,2}\s+\d{1,2}\s+\d{4}|\d{1,2}[- ](?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[- ]\d{2,4}|\d{4}-\d{1,2}-\d{1,2})\b""", RegexOption.IGNORE_CASE)
    private val AMOUNT = Regex("""(?:₹|Rs\.?|INR)?\s*([\d,]+(?:\.\d{1,2})?)\s*(?:Cr|Dr)?\b""", RegexOption.IGNORE_CASE)
    private val DATE_FORMATS = listOf("dd/MM/yyyy", "dd-MM-yyyy", "dd.MM.yyyy", "dd MM yyyy", "dd/MM/yy", "dd-MM-yy", "dd.MM.yy", "dd MMM yyyy", "dd-MMM-yyyy", "dd MMM yy", "dd-MMM-yy", "yyyy-MM-dd")
    private val DEBIT_ROW_MARKER = Regex("""(?i)\b(?:WDL\s+TFR|WITHDRAWAL|DEBIT)\b""")
    private val CREDIT_ROW_MARKER = Regex("""(?i)\b(?:DEP\s+TFR|DEPOSIT|CREDIT)\b""")
    private val UPI_MERCHANT = Regex("""(?i)\bUPI/(?:DR|CR)/\d+/([^/]+)""")
    private val FLEX_UPI_MERCHANT = Regex(
        """(?i)\b(?:UPI?|UPV?)\s*[/| -]?\s*(?:DR|CR)\s*[/| -]?\s*\d{6,}\s*[/| -]?\s*([A-Z][A-Z .&']{1,48})""",
    )
    private val REFERENCE_MERCHANT = Regex(
        """(?i)\b\d{8,}\s+(?:\d{1,3}\s+)?([A-Z][A-Z .&']{1,48})""",
    )
    private val NEFT_MERCHANT = Regex("""(?i)\bNEFT\*[^*]+\*[^*]+\*([^|]+)""")
    private val POS_MERCHANT = Regex("""(?i)\bPOS[/ *-]+(?:\d+[/ *-]+)?([^/|]+)""")
    private const val DAY_MS = 86_400_000L
    private val IMAGE_SUFFIXES = setOf(".jpg", ".jpeg", ".png", ".webp", ".heic", ".heif")
    private val TEXT_SUFFIXES = setOf(".txt", ".csv", ".tsv")
    private val TABULAR_MIMES = setOf("application/csv", "text/csv", "application/vnd.ms-excel")

    private data class LayoutText(val text: String, val left: Int, val centerY: Int, val height: Int)
    private data class ImageQuality(val isBlurry: Boolean, val isLowResolution: Boolean)
}
