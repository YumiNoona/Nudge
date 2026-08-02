package com.nudge.android.service

import android.content.Context
import com.nudge.android.data.AccountEntity
import com.nudge.android.data.MerchantAliasEntity
import com.nudge.android.data.NudgeDatabase
import com.nudge.android.data.TransactionEntity
import com.nudge.android.data.SavedSourceMessageEntity
import com.nudge.android.data.SourceMessageCrypto
import com.nudge.android.data.SourceMessagePolicy
import com.nudge.android.data.CaptureLearning
import com.nudge.engine.DefaultSmsParserEngine
import com.nudge.model.TransactionType
import com.nudge.util.IdGenerator
import com.nudge.android.widget.NudgeWidget
import androidx.glance.appwidget.updateAll
import kotlin.math.abs

/** One deterministic ingestion path for SMS and notification transactions. */
class TransactionCaptureProcessor(context: Context) {
    private val appContext = context.applicationContext
    private val db = NudgeDatabase.getInstance(appContext)
    private val parser = DefaultSmsParserEngine()
    private val prefs = appContext.getSharedPreferences("nudge_prefs", Context.MODE_PRIVATE)
    private val sourceCrypto by lazy { SourceMessageCrypto() }

    data class SourceMetadata(
        val sender: String? = null,
        val packageName: String? = null,
        val originalMessageId: String? = null,
        val originalMessageUri: String? = null
    )

    sealed interface Outcome {
        data class Added(val transaction: TransactionEntity) : Outcome
        data class Duplicate(val existingId: String) : Outcome
        data class Ignored(val reason: String) : Outcome
    }

    suspend fun process(
        rawText: String,
        sourceId: String,
        source: String,
        receivedAt: Long = System.currentTimeMillis(),
        sourceMetadata: SourceMetadata = SourceMetadata()
    ): Outcome {
        if (!prefs.getBoolean("auto_capture_enabled", true)) return Outcome.Ignored("Capture disabled")
        sourceMetadata.originalMessageUri?.takeIf { it.isNotBlank() }?.let { uri ->
            db.savedSourceMessageDao().getByOriginalMessageUri(uri)?.let { existing ->
                return Outcome.Duplicate(existing.transactionId)
            }
        }
        if (looksFailedOrPending(rawText)) return Outcome.Ignored("Pending or failed event")
        if (!looksLikeCompletedMovement(rawText)) return Outcome.Ignored("No completed money movement")

        val parsed = parser.parse(rawText, sourceId) ?: return Outcome.Ignored("Not a transaction")
        if (parsed.amount <= 0L) return Outcome.Ignored("No valid amount")

        val aliases = db.captureRuleDao().getAliases()
        val alias = findAlias(sourceId, rawText, parsed.merchantNormalized, aliases)
        if (alias != null && CaptureLearning.isRejectedSuggestion(alias.suggestedCategoryId)) {
            return Outcome.Ignored("Matches a learned rejection")
        }
        val merchant = alias?.normalizedName
            ?: parsed.merchantNormalized?.takeUnless { it.equals("unknown", true) || it.equals("unknown merchant", true) }
            ?: "Unknown merchant"

        val accounts = db.accountDao().getAllOnce().toMutableList()
        val account = resolveAccount(rawText, accounts) ?: createFallbackAccount()
        val categories = db.categoryDao().getAllOnce()

        val categoryHint = alias?.suggestedCategoryId
            ?: parser.autoCategorize(merchant, parsed.amount).categoryId
        var categoryId = categoryHint?.let { hint ->
            categories.firstOrNull { it.id == hint }?.id
                ?: categories.firstOrNull { categoryMatchesHint(it.name, hint) }?.id
        }

        if (parsed.type == TransactionType.REFUND && categoryId == null) {
            categoryId = db.transactionDao().getAllOnce().firstOrNull {
                it.type == "debit" && it.amountCents == parsed.amount &&
                    merchantSimilarity(it.merchantNormalized ?: it.merchantRaw, merchant) >= .55f
            }?.categoryId
        }

        val type = parsed.type.name.lowercase()
        val duplicates = db.transactionDao().findPotentialDuplicates(
            parsed.amount, type, receivedAt - 10 * 60_000L, receivedAt + 60_000L
        )
        val duplicate = duplicates.firstOrNull {
            abs(receivedAt - it.timestampEpoch) <= 90_000L ||
                merchantSimilarity(it.merchantNormalized ?: it.merchantRaw, merchant) >= .58f
        }
        if (duplicate != null) return Outcome.Duplicate(duplicate.id)

        val categoryConfidence = when {
            alias != null -> .98f
            categoryId != null -> .80f
            else -> 0f
        }
        val overallConfidence = (parsed.confidenceScore * .78f + categoryConfidence * .22f)
            .coerceIn(0f, 1f)
        val reviewed = parsed.type == TransactionType.TRANSFER ||
            (overallConfidence >= .72f && categoryId != null)

        val transaction = TransactionEntity(
            id = IdGenerator.generate(),
            amountCents = parsed.amount,
            type = type,
            merchantRaw = merchant,
            merchantNormalized = merchant,
            categoryId = categoryId,
            accountId = account.id,
            source = source,
            sourceRawText = null,
            confidenceScore = overallConfidence,
            isReviewed = reviewed,
            timestampEpoch = receivedAt
        )
        db.transactionDao().insert(transaction)
        val saveBody = SourceMessagePolicy.shouldSaveBody(
            prefs.getBoolean("save_transaction_messages", false),
            source
        )
        val encryptedBody = if (saveBody) runCatching { sourceCrypto.encrypt(rawText) }.getOrNull() else null
        db.savedSourceMessageDao().upsert(
            SavedSourceMessageEntity(
                id = "source_${transaction.id}",
                transactionId = transaction.id,
                sourceType = source,
                sender = sourceMetadata.sender ?: sourceId.takeIf { source == "sms" || source.startsWith("sms_") },
                packageName = sourceMetadata.packageName ?: sourceId.takeIf { source == "notification" },
                originalMessageId = sourceMetadata.originalMessageId,
                originalMessageUri = sourceMetadata.originalMessageUri,
                encryptedBody = encryptedBody,
                messageTimestamp = receivedAt,
                capturedAt = System.currentTimeMillis(),
                confidence = overallConfidence
            )
        )
        SourceMessagePolicy.retentionCutoff(System.currentTimeMillis(), prefs.getInt("source_retention_days", 0))?.let { cutoff ->
            db.savedSourceMessageDao().clearSavedBodiesBefore(cutoff)
        }
        runCatching { NudgeWidget().updateAll(appContext) }
        return Outcome.Added(transaction)
    }

    private suspend fun createFallbackAccount(): AccountEntity {
        val account = AccountEntity(
            id = IdGenerator.generate(),
            name = "Primary",
            bankName = null,
            accountType = "upi",
            last4Digits = null,
            color = "#365244",
            isDefault = true
        )
        db.accountDao().insert(account)
        return account
    }

    private fun resolveAccount(text: String, accounts: List<AccountEntity>): AccountEntity? {
        val last4 = Regex("(?:xx+|ending|a/c|acct|account)[^0-9]{0,8}([0-9]{4})", RegexOption.IGNORE_CASE)
            .find(text)?.groupValues?.getOrNull(1)
        if (last4 != null) accounts.firstOrNull { it.last4Digits == last4 }?.let { return it }
        accounts.firstOrNull { !it.bankName.isNullOrBlank() && text.contains(it.bankName!!, true) }?.let { return it }
        return accounts.firstOrNull { it.isDefault } ?: accounts.firstOrNull()
    }

    private fun findAlias(sourceId: String, text: String, normalized: String?, aliases: List<MerchantAliasEntity>): MerchantAliasEntity? {
        val haystack = "$sourceId $text ${normalized.orEmpty()}"
        return aliases.map { alias ->
            val regexMatch = runCatching { Regex(alias.rawPattern, RegexOption.IGNORE_CASE).containsMatchIn(haystack) }
                .getOrElse { haystack.contains(alias.rawPattern, true) }
            val similarity = normalized?.let { CaptureLearning.similarity(it, alias.normalizedName) } ?: 0f
            alias to if (regexMatch) 1f else similarity
        }.filter { it.second >= .72f }
            .maxWithOrNull(
                compareBy<Pair<MerchantAliasEntity, Float>> {
                    if (CaptureLearning.isRejectedSuggestion(it.first.suggestedCategoryId)) 1 else 0
                }.thenBy { it.second }
            )
            ?.first
    }

    private fun categoryMatchesHint(name: String, hint: String): Boolean {
        val normalized = name.lowercase().replace("&", " ")
        return when (hint.lowercase()) {
            "food" -> listOf("food", "dining").any(normalized::contains)
            "transport" -> listOf("transport", "travel").any(normalized::contains)
            "groceries" -> listOf("grocer", "daily").any(normalized::contains)
            "shopping" -> normalized.contains("shopping")
            "entertainment" -> normalized.contains("entertain")
            "utilities" -> listOf("utilit", "bill").any(normalized::contains)
            "rent" -> listOf("rent", "housing").any(normalized::contains)
            "healthcare" -> listOf("health", "medical").any(normalized::contains)
            "education" -> normalized.contains("education")
            "subscriptions" -> normalized.contains("subscription")
            "investments" -> normalized.contains("invest")
            else -> normalized.contains(hint.lowercase())
        }
    }

    private fun looksFailedOrPending(text: String): Boolean {
        val lower = text.lowercase()
        return listOf("failed", "declined", "pending", "processing", "cancelled", "canceled", "request money", "payment due", "due date", "statement generated", "reminder")
            .any(lower::contains)
    }

    private fun looksLikeCompletedMovement(text: String): Boolean {
        val lower = text.lowercase()
        return listOf(
            "debited", "credited", "paid", "spent", "sent", "received", "deposited", "deposit",
            "withdrawn", "withdrawal", "refund", "reversal", "purchase", "transferred", "added",
            "loaded", "money in", "money out"
        )
            .any(lower::contains)
    }

    private fun merchantSimilarity(a: String, b: String): Float {
        val left = a.lowercase().filter { it.isLetterOrDigit() }
        val right = b.lowercase().filter { it.isLetterOrDigit() }
        if (left.isBlank() || right.isBlank()) return 0f
        if (left.contains(right) || right.contains(left)) return 1f
        val common = left.toSet().intersect(right.toSet()).size.toFloat()
        return common / maxOf(left.toSet().size, right.toSet().size).coerceAtLeast(1)
    }

}
