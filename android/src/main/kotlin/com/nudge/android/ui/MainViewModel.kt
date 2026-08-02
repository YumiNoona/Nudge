package com.nudge.android.ui

import android.app.Application
import android.content.ContentUris
import android.net.Uri
import android.provider.Telephony
import android.util.Base64
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nudge.android.NudgeApp
import com.nudge.android.data.*
import com.nudge.model.*
import com.nudge.util.DesignTokens
import com.nudge.util.IdGenerator
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import java.security.SecureRandom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import com.nudge.android.service.TransactionCaptureProcessor
import com.nudge.engine.MerchantNormalizer
import com.nudge.android.widget.NudgeWidget
import androidx.glance.appwidget.updateAll

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db: NudgeDatabase

    // --- Flows ---
    val transactions: StateFlow<List<TransactionEntity>>
    val needsReviewCount: StateFlow<Int>
    val categories: StateFlow<List<CategoryEntity>>
    val accounts: StateFlow<List<AccountEntity>>
    val budgets: StateFlow<List<BudgetEntity>>
    val gamificationProfile: StateFlow<GamificationProfileEntity?>
    val sourceMessages: StateFlow<List<SavedSourceMessageEntity>>
    val savedSourceCount: StateFlow<Int>
    val savedSourceBytes: StateFlow<Long>
    private val _captureScanState = MutableStateFlow<String?>(null)
    val captureScanState: StateFlow<String?> = _captureScanState.asStateFlow()
    private val sourceCrypto by lazy { SourceMessageCrypto() }

    init {
        val app = application as NudgeApp
        if (app.encryptedPrefs.getString(NudgeApp.PREFS_KEY_DB_PASSPHRASE, null) == null) {
            val keyBytes = ByteArray(32)
            SecureRandom().nextBytes(keyBytes)
            val encoded = Base64.encodeToString(keyBytes, Base64.NO_WRAP)
            app.encryptedPrefs.edit().putString(NudgeApp.PREFS_KEY_DB_PASSPHRASE, encoded).apply()
        }
        db = NudgeDatabase.getInstance(application)

        // Seed defaults on first run
        viewModelScope.launch {
            DefaultsSeeder.seedIfEmpty(db)
            repairCapturedMerchantNames()
        }

        transactions = db.transactionDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        needsReviewCount = db.transactionDao().getNeedsReviewCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        categories = db.categoryDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        accounts = db.accountDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        budgets = db.budgetDao().getAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        gamificationProfile = db.gamificationDao().getProfile()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

        sourceMessages = db.savedSourceMessageDao().observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        savedSourceCount = db.savedSourceMessageDao().observeSavedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

        savedSourceBytes = db.savedSourceMessageDao().observeSavedBytes()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)
    }

    fun addTransaction(
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String? = null,
        note: String? = null
    ) {
        viewModelScope.launch {
            val now = Clock.System.now()
            val txn = TransactionEntity(
                id = IdGenerator.generate(),
                amountCents = amountCents,
                type = type.name.lowercase(),
                merchantRaw = merchantRaw,
                merchantNormalized = null,
                categoryId = categoryId,
                accountId = accountId,
                source = TransactionSource.MANUAL.name.lowercase(),
                confidenceScore = 1f,
                isReviewed = true,
                note = note,
                timestampEpoch = now.toEpochMilliseconds()
            )
            db.transactionDao().insert(txn)
            awardXp(com.nudge.engine.GamificationMath.XP_MANUAL_ENTRY_SAME_DAY)
            refreshWidget()
        }
    }

    fun reviewTransaction(id: String, categoryId: String) {
        viewModelScope.launch {
            val txn = db.transactionDao().getById(id) ?: return@launch
            db.transactionDao().update(txn.copy(categoryId = categoryId, isReviewed = true))
            learnFromCorrection(txn, categoryId)
            awardXp(com.nudge.engine.GamificationMath.XP_REVIEW_TRANSACTION)
            refreshWidget()
        }
    }

    fun createCategoryForTransaction(
        transactionId: String,
        name: String,
        type: CategoryType,
        icon: String?,
        color: String?
    ) {
        viewModelScope.launch {
            val category = CategoryEntity(
                id = IdGenerator.generate(),
                name = name,
                type = type.name.lowercase(),
                icon = icon,
                color = color ?: DesignTokens.Colors.categoryColor(categories.value.size),
                sortOrder = categories.value.size
            )
            db.categoryDao().insert(category)
            val transaction = db.transactionDao().getById(transactionId) ?: return@launch
            db.transactionDao().update(transaction.copy(categoryId = category.id, isReviewed = true, updatedAt = System.currentTimeMillis()))
            learnFromCorrection(transaction, category.id)
            awardXp(com.nudge.engine.GamificationMath.XP_REVIEW_TRANSACTION)
            refreshWidget()
        }
    }

    fun decryptSourceBody(source: SavedSourceMessageEntity?): String? =
        source?.encryptedBody?.let(sourceCrypto::decrypt)

    fun deleteSavedSourceBody(id: String) {
        viewModelScope.launch { db.savedSourceMessageDao().clearSavedBody(id) }
    }

    fun clearAllSavedSourceBodies() {
        viewModelScope.launch { db.savedSourceMessageDao().clearAllSavedBodies() }
    }

    fun applySourceRetention(days: Int?) {
        if (days == null) return
        viewModelScope.launch {
            val cutoff = System.currentTimeMillis() - days * 24L * 60L * 60L * 1000L
            db.savedSourceMessageDao().clearSavedBodiesBefore(cutoff)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            db.transactionDao().update(transaction.copy(updatedAt = System.currentTimeMillis(), isReviewed = true))
            val merchant = transaction.merchantNormalized ?: transaction.merchantRaw
            transaction.categoryId?.takeIf { it.isNotBlank() && !merchant.equals("Unknown merchant", true) }?.let { categoryId ->
                learnFromCorrection(transaction, categoryId)
            }
            refreshWidget()
        }
    }

    fun scanHistoricalSms() {
        viewModelScope.launch {
            _captureScanState.value = "Scanning all accessible SMS & MMS…"
            val result = withContext(Dispatchers.IO) { scanAccessibleMessageHistory() }
            _captureScanState.value = buildString {
                append("Checked ${result.checked} messages · added ${result.added}")
                if (result.unreadable > 0) append(" · ${result.unreadable} unreadable")
            }
        }
    }

    private data class MessageScanResult(var checked: Int = 0, var added: Int = 0, var unreadable: Int = 0)

    private suspend fun scanAccessibleMessageHistory(): MessageScanResult {
        val result = MessageScanResult()
        val resolver = getApplication<Application>().contentResolver
        val processor = TransactionCaptureProcessor(getApplication())
        val smsProjection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE,
            Telephony.Sms.TYPE,
        )

        runCatching {
            resolver.query(
                Telephony.Sms.CONTENT_URI,
                smsProjection,
                "${Telephony.Sms.TYPE} = ?",
                arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString()),
                "${Telephony.Sms.DATE} DESC",
            )
        }.getOrNull()?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
            val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
            val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
            while (cursor.moveToNext()) {
                result.checked++
                publishScanProgress(result.checked)
                val body = cursor.getString(bodyIndex)?.takeIf { it.isNotBlank() } ?: continue
                try {
                    val id = cursor.getLong(idIndex)
                    val sender = cursor.getString(addressIndex).orEmpty()
                    val outcome = processor.process(
                        rawText = body,
                        sourceId = sender,
                        source = "sms_import",
                        receivedAt = cursor.getLong(dateIndex),
                        sourceMetadata = TransactionCaptureProcessor.SourceMetadata(
                            sender = sender,
                            originalMessageId = id.toString(),
                            originalMessageUri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id).toString(),
                        ),
                    )
                    if (outcome is TransactionCaptureProcessor.Outcome.Added) result.added++
                } catch (_: Exception) {
                    result.unreadable++
                }
            }
        } ?: run { result.unreadable++ }

        val mmsProjection = arrayOf(Telephony.Mms._ID, Telephony.Mms.DATE, Telephony.Mms.MESSAGE_BOX)
        runCatching {
            resolver.query(
                Telephony.Mms.CONTENT_URI,
                mmsProjection,
                "${Telephony.Mms.MESSAGE_BOX} = ?",
                arrayOf(Telephony.Mms.MESSAGE_BOX_INBOX.toString()),
                "${Telephony.Mms.DATE} DESC",
            )
        }.getOrNull()?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow(Telephony.Mms._ID)
            val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Mms.DATE)
            while (cursor.moveToNext()) {
                result.checked++
                publishScanProgress(result.checked)
                val id = cursor.getLong(idIndex)
                val body = readMmsText(id)
                if (body.isBlank()) continue
                try {
                    val outcome = processor.process(
                        rawText = body,
                        sourceId = "mms",
                        source = "mms_import",
                        receivedAt = cursor.getLong(dateIndex).let { if (it < 10_000_000_000L) it * 1_000L else it },
                        sourceMetadata = TransactionCaptureProcessor.SourceMetadata(
                            originalMessageId = id.toString(),
                            originalMessageUri = ContentUris.withAppendedId(Telephony.Mms.CONTENT_URI, id).toString(),
                        ),
                    )
                    if (outcome is TransactionCaptureProcessor.Outcome.Added) result.added++
                } catch (_: Exception) {
                    result.unreadable++
                }
            }
        }
        return result
    }

    private fun publishScanProgress(checked: Int) {
        if (checked % 100 == 0) _captureScanState.value = "Checked $checked messages…"
    }

    private fun readMmsText(messageId: Long): String {
        val resolver = getApplication<Application>().contentResolver
        val partsUri = Uri.parse("content://mms/part")
        val parts = mutableListOf<String>()
        runCatching {
            resolver.query(
                partsUri,
                arrayOf("_id", "ct", "text", "_data"),
                "mid = ?",
                arrayOf(messageId.toString()),
                "_id ASC",
            )
        }.getOrNull()?.use { cursor ->
            val idIndex = cursor.getColumnIndexOrThrow("_id")
            val typeIndex = cursor.getColumnIndexOrThrow("ct")
            val textIndex = cursor.getColumnIndexOrThrow("text")
            val dataIndex = cursor.getColumnIndexOrThrow("_data")
            while (cursor.moveToNext()) {
                if (cursor.getString(typeIndex) != "text/plain") continue
                val directText = cursor.getString(textIndex)
                if (!directText.isNullOrBlank()) {
                    parts += directText
                } else if (!cursor.getString(dataIndex).isNullOrBlank()) {
                    val partUri = ContentUris.withAppendedId(partsUri, cursor.getLong(idIndex))
                    runCatching { resolver.openInputStream(partUri)?.bufferedReader()?.use { it.readText() } }
                        .getOrNull()?.takeIf { it.isNotBlank() }?.let(parts::add)
                }
            }
        }
        return parts.joinToString(" ")
    }

    private suspend fun learnFromCorrection(transaction: TransactionEntity, categoryId: String) {
        val merchant = (transaction.merchantNormalized ?: transaction.merchantRaw).trim()
        val canonical = CaptureLearning.canonicalMerchant(merchant)
        if (canonical.isBlank() || canonical == "unknown merchant" || categoryId.isBlank()) return

        db.captureRuleDao().getAliases()
            .filter { CaptureLearning.isRejectedSuggestion(it.suggestedCategoryId) }
            .filter { CaptureLearning.sameMerchant(it.normalizedName, merchant) }
            .forEach { db.captureRuleDao().deleteAlias(it.id) }

        db.captureRuleDao().upsertAlias(
            MerchantAliasEntity(
                id = CaptureLearning.learnedRuleId(merchant),
                rawPattern = CaptureLearning.tolerantPattern(merchant),
                normalizedName = merchant,
                suggestedCategoryId = categoryId,
            ),
        )

        // Apply one correction to matching pending captures immediately. This keeps
        // the review queue from asking the same merchant question repeatedly.
        db.transactionDao().getAllOnce()
            .asSequence()
            .filter { !it.isReviewed && it.id != transaction.id && it.type == transaction.type }
            .filter { CaptureLearning.sameMerchant(it.merchantNormalized ?: it.merchantRaw, merchant) }
            .forEach { pending ->
                db.transactionDao().update(
                    pending.copy(
                        merchantNormalized = merchant,
                        categoryId = categoryId,
                        confidenceScore = maxOf(pending.confidenceScore, .96f),
                        isReviewed = true,
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
    }

    fun addCategory(name: String, type: CategoryType, icon: String? = null, color: String? = null) {
        viewModelScope.launch {
            val existingCount = categories.value.size
            val cat = CategoryEntity(
                id = IdGenerator.generate(),
                name = name,
                type = type.name.lowercase(),
                icon = icon,
                color = color ?: DesignTokens.Colors.categoryColor(existingCount),
                sortOrder = existingCount
            )
            db.categoryDao().insert(cat)
        }
    }

    fun archiveCategory(id: String) {
        viewModelScope.launch { db.categoryDao().archive(id) }
    }

    fun updateCategory(category: CategoryEntity) {
        viewModelScope.launch { db.categoryDao().update(category) }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            db.runInTransaction {
                db.openHelper.writableDatabase.execSQL(
                    "UPDATE transactions SET category_id = NULL WHERE category_id = ?",
                    arrayOf(id)
                )
                db.openHelper.writableDatabase.execSQL("DELETE FROM categories WHERE id = ?", arrayOf(id))
            }
        }
    }

    fun addAccount(name: String, type: AccountType, bankName: String? = null, last4Digits: String? = null) {
        viewModelScope.launch {
            val account = AccountEntity(
                id = IdGenerator.generate(),
                name = name,
                accountType = type.name.lowercase(),
                bankName = bankName,
                last4Digits = last4Digits,
                color = null,
                icon = null
            )
            db.accountDao().insert(account)
        }
    }

    fun saveAccount(account: AccountEntity) {
        viewModelScope.launch {
            if (account.isDefault) db.accountDao().clearDefault()
            db.accountDao().insert(account)
        }
    }

    fun deleteAccount(id: String) {
        viewModelScope.launch {
            db.accountDao().getById(id)?.let { db.accountDao().delete(it) }
        }
    }

    fun setDefaultAccount(id: String) {
        viewModelScope.launch {
            db.accountDao().clearDefault()
            db.accountDao().setDefault(id)
        }
    }

    fun archiveAccount(id: String) {
        viewModelScope.launch { db.accountDao().archive(id) }
    }

    fun restoreAccount(id: String) {
        viewModelScope.launch { db.accountDao().restore(id) }
    }

    fun deleteTransaction(id: String) {
        viewModelScope.launch { db.transactionDao().deleteById(id); refreshWidget() }
    }

    fun rejectTransaction(id: String) {
        viewModelScope.launch {
            val transaction = db.transactionDao().getById(id) ?: return@launch
            if (transaction.source == "manual") {
                db.transactionDao().deleteById(id)
                refreshWidget()
                return@launch
            }

            val source = db.savedSourceMessageDao().getByTransaction(id)
            val sourceText = source?.encryptedBody?.let { runCatching { sourceCrypto.decrypt(it) }.getOrNull() }
                ?: withContext(Dispatchers.IO) { readOriginalSmsBody(source?.originalMessageUri) }
            val sender = source?.sender ?: source?.packageName
            val merchant = (transaction.merchantNormalized ?: transaction.merchantRaw).trim()
            val canonical = CaptureLearning.canonicalMerchant(merchant)

            if (canonical.isNotBlank() && canonical != "unknown merchant") {
                db.captureRuleDao().upsertAlias(
                    MerchantAliasEntity(
                        id = CaptureLearning.rejectedRuleId(sender, merchant),
                        rawPattern = CaptureLearning.rejectionPattern(sender, sourceText, merchant),
                        normalizedName = merchant,
                        suggestedCategoryId = CaptureLearning.REJECTED_SUGGESTION,
                    ),
                )

                // A rejection is feedback, so clear the same repeated pending noise now.
                db.transactionDao().getAllOnce()
                    .filter { !it.isReviewed && it.source != "manual" }
                    .filter { CaptureLearning.sameMerchant(it.merchantNormalized ?: it.merchantRaw, merchant) }
                    .forEach { db.transactionDao().deleteById(it.id) }
            } else {
                db.transactionDao().deleteById(id)
            }
            refreshWidget()
        }
    }

    private fun readOriginalSmsBody(originalUri: String?): String? {
        if (originalUri.isNullOrBlank()) return null
        return runCatching {
            getApplication<Application>().contentResolver.query(
                Uri.parse(originalUri),
                arrayOf(Telephony.Sms.BODY),
                null,
                null,
                null,
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)) else null
            }
        }.getOrNull()
    }

    fun importTransaction(txn: TransactionEntity) {
        viewModelScope.launch { db.transactionDao().insert(txn) }
    }

    fun importCategory(cat: CategoryEntity) {
        viewModelScope.launch { db.categoryDao().insert(cat) }
    }

    fun importAccount(account: AccountEntity) {
        viewModelScope.launch { db.accountDao().insert(account) }
    }

    fun importBudget(budget: BudgetEntity) {
        viewModelScope.launch { db.budgetDao().insert(budget) }
    }

    fun saveBudget(id: String?, categoryId: String?, amountCents: Long, period: String, rolloverEnabled: Boolean, startDateEpoch: Long) {
        viewModelScope.launch {
            val budget = com.nudge.android.data.BudgetEntity(
                id = id ?: java.util.UUID.randomUUID().toString(),
                categoryId = categoryId ?: "",
                amountCents = amountCents,
                period = period,
                rolloverEnabled = rolloverEnabled,
                startDateEpoch = startDateEpoch
            )
            db.budgetDao().insert(budget)
        }
    }

    fun deleteBudget(id: String) {
        viewModelScope.launch {
            val budget = budgets.value.find { it.id == id }
            if (budget != null) db.budgetDao().delete(budget)
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            db.transactionDao().let { dao ->
                // Clear all tables
                db.openHelper.writableDatabase.apply {
                    execSQL("DELETE FROM transactions")
                    execSQL("DELETE FROM categories")
                    execSQL("DELETE FROM subcategories")
                    execSQL("DELETE FROM accounts")
                    execSQL("DELETE FROM budgets")
                    execSQL("DELETE FROM recurring_rules")
                    execSQL("DELETE FROM gamification_profile")
                    execSQL("DELETE FROM parser_rules")
                    execSQL("DELETE FROM merchant_aliases")
                    execSQL("DELETE FROM sender_whitelist")
                    execSQL("DELETE FROM saved_source_messages")
                }
            }
        }
    }

    // --- Gamification ---

    private suspend fun awardXp(xp: Long) {
        val userId = getUserId()
        val profile = db.gamificationDao().getProfile().first()
        if (profile != null) {
            db.gamificationDao().addXp(userId, xp)
        } else {
            db.gamificationDao().upsert(
                GamificationProfileEntity(
                    userId = userId,
                    xpTotal = xp,
                    level = 1,
                    lastActivityDate = Clock.System.now().toEpochMilliseconds()
                )
            )
        }
    }

    private suspend fun refreshWidget() {
        runCatching { NudgeWidget().updateAll(getApplication()) }
    }

    private suspend fun repairCapturedMerchantNames() {
        db.transactionDao().getAllOnce().filter { it.source != "manual" }.forEach { transaction ->
            val normalized = MerchantNormalizer.normalize(transaction.merchantRaw).normalized
            if (normalized.isNotBlank() && normalized != transaction.merchantRaw) {
                db.transactionDao().update(
                    transaction.copy(
                        merchantRaw = normalized,
                        merchantNormalized = normalized,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    private fun getUserId(): String {
        val app = getApplication<NudgeApp>()
        var userId = app.encryptedPrefs.getString(NudgeApp.PREFS_KEY_USER_ID, null)
        if (userId == null) {
            userId = IdGenerator.generate()
            app.encryptedPrefs.edit().putString(NudgeApp.PREFS_KEY_USER_ID, userId).apply()
        }
        return userId
    }
}
