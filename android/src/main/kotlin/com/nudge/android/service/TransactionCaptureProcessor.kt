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
import com.nudge.android.data.CaptureAssessment
import com.nudge.android.data.CaptureAccountMatcher
import com.nudge.android.data.CaptureDuplicateMatcher
import com.nudge.android.data.CaptureIdentity
import com.nudge.android.data.CaptureDiagnostics
import androidx.room.withTransaction
import com.nudge.engine.DefaultSmsParserEngine
import com.nudge.engine.FinancialEventClassifier
import com.nudge.engine.TransactionMessageGuard
import com.nudge.model.TransactionType
import com.nudge.util.IdGenerator
import com.nudge.android.widget.NudgeWidget
import androidx.glance.appwidget.updateAll
import kotlin.math.abs

/** One deterministic ingestion path for SMS and notification transactions. */
class TransactionCaptureProcessor(context: Context, private val db: NudgeDatabase = NudgeDatabase.getInstance(context)) {
    private val appContext = context.applicationContext
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
        sourceMetadata: SourceMetadata = SourceMetadata(),
        bypassCaptureToggle: Boolean = false,
    ): Outcome {
        val outcome = db.withTransaction {
            processLocked(rawText, sourceId, source, receivedAt, sourceMetadata, bypassCaptureToggle)
        }
        if (!bypassCaptureToggle || outcome is Outcome.Added) {
            val kind = when (outcome) { is Outcome.Added -> "added"; is Outcome.Duplicate -> "duplicate"; is Outcome.Ignored -> "ignored" }
            val reason = when (outcome) {
                is Outcome.Added -> if (outcome.transaction.isReviewed) "Captured" else "Captured · needs review"
                is Outcome.Duplicate -> "Matched an existing transaction"
                is Outcome.Ignored -> outcome.reason
            }
            CaptureDiagnostics.record(appContext, kind, source, reason)
        }
        if (outcome is Outcome.Added) runCatching { NudgeWidget().updateAll(appContext) }
        return outcome
    }

    private suspend fun processLocked(rawText: String, sourceId: String, source: String, receivedAt: Long,
        sourceMetadata: SourceMetadata, bypassCaptureToggle: Boolean): Outcome {
        if (!bypassCaptureToggle && !prefs.getBoolean("auto_capture_enabled", true)) {
            return Outcome.Ignored("Capture disabled")
        }
        sourceMetadata.originalMessageUri?.takeIf { it.isNotBlank() }?.let { uri ->
            db.savedSourceMessageDao().getByOriginalMessageUri(uri)?.let { existing ->
                return Outcome.Duplicate(existing.transactionId)
            }
        }
        val fingerprint = CaptureIdentity.fingerprint(source, sourceId, rawText, receivedAt)
        db.savedSourceMessageDao().getByFingerprint(fingerprint)?.let { return Outcome.Duplicate(it.transactionId) }
        val referenceHash = CaptureIdentity.referenceHash(rawText)
        if (TransactionMessageGuard.isNonTransaction(rawText)) return Outcome.Ignored("Statement or due notice")
        val direction = FinancialEventClassifier.classify(rawText)
            ?: return Outcome.Ignored("No unambiguous completed money movement")

        val extracted = parser.parse(rawText, sourceId) ?: return Outcome.Ignored("Not a transaction")
        // Extraction rules may find the amount/merchant, but the semantic classifier
        // is the sole authority for direction. This prevents a bank regex hint from
        // turning card repayments into income or incoming transfers into expenses.
        val parsed = extracted.copy(
            type = direction.type,
            directionConfidence = direction.confidence,
        )
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
        val accountMatch = CaptureAccountMatcher.resolve(rawText, accounts)
        val account = accountMatch.account ?: createFallbackAccount()
        val categories = db.categoryDao().getAllOnce()

        val categoryHint = alias?.suggestedCategoryId
            ?: parser.autoCategorize(merchant, parsed.amount).categoryId
        var categoryId = categoryHint?.let { hint ->
            val expectedType = if (parsed.type == TransactionType.CREDIT) "income" else "expense"
            categories.firstOrNull { it.type == expectedType && it.id == hint }?.id
                ?: categories.firstOrNull { it.type == expectedType && categoryMatchesHint(it.name, hint) }?.id
        }

        val refundMatch = if (parsed.type == TransactionType.REFUND) {
            db.transactionDao().getAllOnce().filter {
                it.type == "debit" && it.currency == "INR" && it.accountId == account.id &&
                    it.amountCents >= parsed.amount && it.timestampEpoch <= receivedAt &&
                    receivedAt - it.timestampEpoch <= 120L * 24 * 60 * 60_000 &&
                    CaptureLearning.similarity(it.merchantNormalized ?: it.merchantRaw, merchant) >= .94f
            }.singleOrNull()
        } else null
        if (refundMatch != null) categoryId = refundMatch.categoryId ?: categoryId

        val type = parsed.type.name.lowercase()
        val candidates = (db.transactionDao().findPotentialDuplicates(
            parsed.amount, type, receivedAt - 10 * 60_000L, receivedAt + 60_000L
        ) + (referenceHash?.let { db.savedSourceMessageDao().getTransactionsByReference(it) } ?: emptyList()))
            .distinctBy { it.id }.filter { it.type == type && it.amountCents == parsed.amount && it.currency == "INR" }
        val incoming = CaptureDuplicateMatcher.Evidence(account.id, accountMatch.confidence, merchant, source, receivedAt, fingerprint, referenceHash)
        var possibleDuplicateId: String? = null
        for (candidate in candidates) {
            val sources = db.savedSourceMessageDao().getAllByTransaction(candidate.id)
            val knownReferences = sources.mapNotNull { it.referenceHash }.toSet()
            if (referenceHash != null && knownReferences.isNotEmpty() && referenceHash !in knownReferences) continue
            for (saved in sources.ifEmpty { listOf(null) }) {
                val previous = CaptureDuplicateMatcher.Evidence(candidate.accountId,
                    CaptureAssessment.fromJson(saved?.assessmentJson)?.account ?: .25f,
                    candidate.merchantNormalized ?: candidate.merchantRaw, saved?.sourceType ?: candidate.source,
                    saved?.messageTimestamp ?: candidate.timestampEpoch, saved?.fingerprint, saved?.referenceHash)
                when (CaptureDuplicateMatcher.compare(incoming, previous)) {
                    CaptureDuplicateMatcher.Decision.DUPLICATE -> {
                        saveSource(candidate.id, rawText, sourceId, source, receivedAt, sourceMetadata,
                            fingerprint, referenceHash, CaptureAssessment.fromJson(saved?.assessmentJson))
                        return Outcome.Duplicate(candidate.id)
                    }
                    CaptureDuplicateMatcher.Decision.POSSIBLE -> possibleDuplicateId = candidate.id
                    CaptureDuplicateMatcher.Decision.DISTINCT -> Unit
                }
            }
        }

        val categoryConfidence = when {
            categoryId != null && alias?.suggestedCategoryId == categoryId -> .98f
            categoryId != null -> .80f
            else -> 0f
        }
        val assessment = CaptureAssessment(parsed.amountConfidence, direction.confidence,
            if (merchant == "Unknown merchant") .2f else if (alias != null) .98f else parsed.merchantConfidence, accountMatch.confidence,
            if (parsed.type == TransactionType.TRANSFER) 1f else categoryConfidence, possibleDuplicateId, refundMatch?.id)
        val overallConfidence = assessment.overall
        val reviewed = !assessment.needsReview

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
        saveSource(transaction.id, rawText, sourceId, source, receivedAt, sourceMetadata, fingerprint, referenceHash, assessment)
        return Outcome.Added(transaction)
    }

    private suspend fun saveSource(transactionId: String, rawText: String, sourceId: String, source: String,
        receivedAt: Long, sourceMetadata: SourceMetadata, fingerprint: String, referenceHash: String?, assessment: CaptureAssessment?) {
        val saveBody = SourceMessagePolicy.shouldSaveBody(
            prefs.getBoolean("save_transaction_messages", false),
            source
        )
        val encryptedBody = if (saveBody) runCatching { sourceCrypto.encrypt(rawText) }.getOrNull() else null
        db.savedSourceMessageDao().upsert(
            SavedSourceMessageEntity(
                id = "source_$fingerprint",
                transactionId = transactionId,
                sourceType = source,
                sender = sourceMetadata.sender ?: sourceId.takeIf { source == "sms" || source.startsWith("sms_") },
                packageName = sourceMetadata.packageName ?: sourceId.takeIf { source == "notification" },
                originalMessageId = sourceMetadata.originalMessageId,
                originalMessageUri = sourceMetadata.originalMessageUri,
                encryptedBody = encryptedBody,
                messageTimestamp = receivedAt,
                capturedAt = System.currentTimeMillis(),
                confidence = assessment?.overall ?: .25f,
                fingerprint = fingerprint,
                referenceHash = referenceHash,
                assessmentJson = assessment?.toJson(),
            )
        )
        SourceMessagePolicy.retentionCutoff(System.currentTimeMillis(), prefs.getInt("source_retention_days", 0))?.let { cutoff ->
            db.savedSourceMessageDao().clearSavedBodiesBefore(cutoff)
        }
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

    private fun findAlias(sourceId: String, text: String, normalized: String?, aliases: List<MerchantAliasEntity>): MerchantAliasEntity? {
        val haystack = "$sourceId $text ${normalized.orEmpty()}"
        return aliases.map { alias ->
            val regexMatch = runCatching { Regex(alias.rawPattern, RegexOption.IGNORE_CASE).containsMatchIn(haystack) }
                .getOrElse { haystack.contains(alias.rawPattern, true) }
            val similarity = normalized?.let { CaptureLearning.similarity(it, alias.normalizedName) } ?: 0f
            alias to if (regexMatch) 1f else if (CaptureLearning.isRejectedSuggestion(alias.suggestedCategoryId)) 0f else similarity
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

}
