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
import kotlinx.coroutines.delay
import com.nudge.android.service.TransactionCaptureProcessor
import com.nudge.engine.MerchantNormalizer
import com.nudge.engine.DefaultCategorizationEngine
import com.nudge.engine.DefaultSmsParserEngine
import com.nudge.engine.TransactionMessageGuard
import com.nudge.android.importer.StatementDraft
import com.nudge.android.importer.DetailedReceiptDraft
import com.nudge.android.importer.ReceiptIntelligence
import com.nudge.android.widget.NudgeWidget
import androidx.room.withTransaction
import androidx.glance.appwidget.updateAll
import java.io.File
import java.util.Calendar

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
    val friends: StateFlow<List<FriendEntity>>
    val transactionSplits: StateFlow<List<TransactionSplitEntity>>
    val recurringTransactions: StateFlow<List<RecurringTransactionEntity>>
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
            removeInvalidCapturedStatements()
            repairCapturedSemantics()
            repairCapturedMerchantNames()
            materializeDueRecurringTransactions()
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

        friends = db.sharedExpenseDao().observeFriends()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        transactionSplits = db.sharedExpenseDao().observeAllSplits()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

        recurringTransactions = db.recurringTransactionDao().observeActive()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    }

    fun addTransaction(
        amountCents: Long,
        type: TransactionType,
        merchantRaw: String,
        accountId: String,
        categoryId: String? = null,
        note: String? = null,
        timestampEpoch: Long = System.currentTimeMillis(),
        split: SplitDraft? = null,
        recurrence: RecurrenceDraft? = null,
    ) {
        viewModelScope.launch {
            val recurringId = recurrence?.let { IdGenerator.generate() }
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
                isRecurring = recurrence != null,
                recurringGroupId = recurringId,
                note = note,
                timestampEpoch = timestampEpoch,
            )
            db.transactionDao().insert(txn)
            replaceSplits(txn.id, split)
            recurrence?.let {
                db.recurringTransactionDao().upsert(
                    RecurringTransactionEntity(
                        id = recurringId!!,
                        templateTransactionId = txn.id,
                        interval = it.interval,
                        nextRunEpoch = advanceRecurringDate(timestampEpoch, it.interval),
                        endEpoch = it.endEpoch,
                    ),
                )
                materializeDueRecurringTransactions()
            }
            awardXp(com.nudge.engine.GamificationMath.XP_MANUAL_ENTRY_SAME_DAY)
            refreshWidget()
        }
    }

    fun saveReceipt(
        receipt: DetailedReceiptDraft,
        accountId: String,
        categoryId: String?,
        itemized: Boolean,
        onComplete: (Boolean, String) -> Unit,
    ) {
        viewModelScope.launch {
            val nearbyDuplicates = db.transactionDao().findPotentialDuplicates(
                receipt.printedTotalCents,
                "debit",
                receipt.purchaseTimestamp - 36L * 60L * 60L * 1_000L,
                receipt.purchaseTimestamp + 36L * 60L * 60L * 1_000L,
            )
            val duplicate = nearbyDuplicates.firstOrNull {
                MerchantNormalizer.normalize(it.merchantRaw).normalized.equals(
                    MerchantNormalizer.normalize(receipt.merchant).normalized,
                    ignoreCase = true,
                )
            }
            if (duplicate != null) {
                withContext(Dispatchers.Main.immediate) {
                    onComplete(false, "This receipt already matches ${duplicate.merchantRaw} for ₹${receipt.printedTotalCents / 100.0}.")
                }
                return@launch
            }

            val receiptId = IdGenerator.generate()
            val activeItems = receipt.items.filter { it.lineTotalCents > 0 }
            val allocations = ReceiptIntelligence.allocateTotal(activeItems, receipt.printedTotalCents)
            val createdTransactions = mutableListOf<TransactionEntity>()
            val lineEntities = mutableListOf<ReceiptLineItemEntity>()
            db.withTransaction {
                db.receiptDao().insertReceipt(
                    ReceiptEntity(
                        id = receiptId,
                        merchant = receipt.merchant,
                        printedTotalCents = receipt.printedTotalCents,
                        calculatedTotalCents = receipt.calculatedTotalCents,
                        subtotalCents = receipt.subtotalCents,
                        discountCents = receipt.discountCents,
                        taxCents = receipt.taxCents,
                        feeCents = receipt.feeCents,
                        tipCents = receipt.tipCents,
                        roundingCents = receipt.roundingCents,
                        purchaseTimestamp = receipt.purchaseTimestamp,
                        rawText = receipt.rawText,
                        confidence = receipt.confidence,
                        saveMode = if (itemized) "itemized" else "single",
                    ),
                )
                db.receiptDao().insertPages(receipt.pages.mapIndexed { index, page ->
                    ReceiptPageEntity(IdGenerator.generate(), receiptId, index, page.localUri, page.ocrText, page.warning)
                })
                if (itemized && activeItems.isNotEmpty()) {
                    activeItems.forEachIndexed { index, item ->
                        val transaction = TransactionEntity(
                            id = IdGenerator.generate(),
                            amountCents = allocations[index],
                            type = "debit",
                            merchantRaw = item.name,
                            merchantNormalized = item.name,
                            categoryId = item.selectedCategoryId ?: categoryForReceiptHint(item.categoryHint),
                            accountId = accountId,
                            source = "receipt",
                            confidenceScore = item.confidence,
                            isReviewed = true,
                            note = "${receipt.merchant} · itemized receipt",
                            timestampEpoch = receipt.purchaseTimestamp,
                        )
                        db.transactionDao().insert(transaction)
                        createdTransactions += transaction
                        lineEntities += ReceiptLineItemEntity(
                            IdGenerator.generate(), receiptId, transaction.id, item.name, item.quantity,
                            item.unitPriceCents, item.lineTotalCents, allocations[index], transaction.categoryId,
                            item.confidence, index,
                        )
                    }
                } else {
                    val transaction = TransactionEntity(
                        id = IdGenerator.generate(),
                        amountCents = receipt.printedTotalCents,
                        type = "debit",
                        merchantRaw = receipt.merchant,
                        merchantNormalized = receipt.merchant,
                        categoryId = categoryId ?: categoryForReceiptHint(activeItems.firstNotNullOfOrNull { it.categoryHint }),
                        accountId = accountId,
                        source = "receipt",
                        confidenceScore = receipt.confidence,
                        isReviewed = true,
                        note = "Receipt · ${activeItems.size} detected items",
                        timestampEpoch = receipt.purchaseTimestamp,
                    )
                    db.transactionDao().insert(transaction)
                    createdTransactions += transaction
                    lineEntities += activeItems.mapIndexed { index, item ->
                        ReceiptLineItemEntity(
                            IdGenerator.generate(), receiptId, transaction.id, item.name, item.quantity,
                            item.unitPriceCents, item.lineTotalCents, allocations.getOrElse(index) { item.lineTotalCents },
                            item.selectedCategoryId ?: categoryForReceiptHint(item.categoryHint), item.confidence, index,
                        )
                    }
                }
                if (lineEntities.isNotEmpty()) db.receiptDao().insertItems(lineEntities)
                db.receiptDao().insertLinks(createdTransactions.map { ReceiptTransactionLinkEntity(receiptId, it.id) })
            }
            refreshWidget()
            withContext(Dispatchers.Main.immediate) {
                onComplete(true, if (itemized) "Added ${createdTransactions.size} linked receipt items" else "Receipt added with ${activeItems.size} saved line items")
            }
        }
    }

    private fun categoryForReceiptHint(hint: String?): String? {
        val value = hint?.lowercase() ?: return null
        return categories.value.firstOrNull { category ->
            category.type == "expense" && when (value) {
                "food" -> category.name.contains("food", true) || category.name.contains("dining", true)
                "groceries" -> category.name.contains("grocer", true)
                "healthcare" -> category.name.contains("health", true) || category.name.contains("medical", true)
                "personal care" -> category.name.contains("personal", true)
                "education" -> category.name.contains("education", true)
                else -> category.name.contains(value, true)
            }
        }?.id
    }

    fun importStatementTransactions(
        drafts: List<StatementDraft>,
        accountId: String,
        replaceExistingStatements: Boolean,
        onComplete: (imported: Int, skipped: Int) -> Unit,
    ) {
        viewModelScope.launch {
            if (replaceExistingStatements) {
                db.transactionDao().deleteStatementImportsForAccount(accountId)
            }
            val existing = transactions.value.toMutableList()
            if (replaceExistingStatements) {
                existing.removeAll { it.accountId == accountId && it.source == "statement" }
            }
            val categorizer = DefaultCategorizationEngine()
            var imported = 0
            var skipped = 0
            drafts.forEach { draft ->
                val normalized = MerchantNormalizer.normalize(draft.merchant).normalized
                val duplicate = existing.any { transaction ->
                    transaction.amountCents == draft.amountCents &&
                        transaction.type == draft.type.name.lowercase() &&
                        kotlin.math.abs(transaction.timestampEpoch - draft.timestampEpoch) < 36L * 60L * 60L * 1_000L &&
                        MerchantNormalizer.normalize(transaction.merchantRaw).normalized.equals(normalized, ignoreCase = true)
                }
                if (duplicate) {
                    skipped++
                    return@forEach
                }
                val hint = categorizer.autoCategorize(normalized, draft.amountCents).categoryId
                val categoryId = hint?.takeIf { it.isNotBlank() }?.let { categoryHint -> categories.value.firstOrNull { category ->
                    val expectedType = if (draft.type == TransactionType.CREDIT) "income" else "expense"
                    category.type == expectedType && category.name.lowercase().replace(" & ", " ").contains(
                        when (categoryHint) {
                            "food" -> "food"
                            "other" -> "other"
                            else -> categoryHint
                        },
                    )
                }?.id }
                val transaction = TransactionEntity(
                    id = IdGenerator.generate(),
                    amountCents = draft.amountCents,
                    type = draft.type.name.lowercase(),
                    merchantRaw = normalized,
                    merchantNormalized = normalized,
                    categoryId = categoryId,
                    accountId = accountId,
                    source = "statement",
                    confidenceScore = if (categoryId == null || normalized == "Unknown merchant") .62f else .82f,
                    isReviewed = categoryId != null && normalized != "Unknown merchant",
                    timestampEpoch = draft.timestampEpoch,
                )
                db.transactionDao().insert(transaction)
                existing += transaction
                imported++
            }
            refreshWidget()
            withContext(Dispatchers.Main.immediate) { onComplete(imported, skipped) }
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

    fun updateTransactionDetails(
        transaction: TransactionEntity,
        split: SplitDraft?,
        recurrence: RecurrenceDraft?,
    ) {
        viewModelScope.launch {
            val existingRule = transaction.recurringGroupId?.let { db.recurringTransactionDao().getById(it) }
                ?: db.recurringTransactionDao().getForTemplate(transaction.id)
            val recurringId = recurrence?.let { existingRule?.id ?: IdGenerator.generate() }
            val updated = transaction.copy(
                isReviewed = true,
                isRecurring = recurrence != null,
                recurringGroupId = recurringId,
                updatedAt = System.currentTimeMillis(),
            )
            db.transactionDao().update(updated)
            replaceSplits(updated.id, split)
            if (recurrence == null) {
                db.recurringTransactionDao().deleteForTemplate(existingRule?.templateTransactionId ?: updated.id)
            } else {
                db.recurringTransactionDao().upsert(
                    RecurringTransactionEntity(
                        id = recurringId!!,
                        templateTransactionId = existingRule?.templateTransactionId ?: updated.id,
                        interval = recurrence.interval,
                        nextRunEpoch = existingRule?.nextRunEpoch
                            ?: advanceRecurringDate(updated.timestampEpoch, recurrence.interval),
                        endEpoch = recurrence.endEpoch,
                    ),
                )
            }
            refreshWidget()
        }
    }

    fun saveFriend(name: String): FriendEntity {
        val friend = FriendEntity(id = IdGenerator.generate(), name = name.trim())
        viewModelScope.launch { db.sharedExpenseDao().upsertFriend(friend) }
        return friend
    }

    fun settleSplit(splitId: String, amountCents: Long) {
        viewModelScope.launch { db.sharedExpenseDao().settle(splitId, amountCents) }
    }

    private suspend fun replaceSplits(transactionId: String, split: SplitDraft?) {
        db.sharedExpenseDao().deleteSplits(transactionId)
        val shares = split?.members.orEmpty().filter { it.shareCents > 0 || it.paidCents > 0 }.map { member ->
            TransactionSplitEntity(
                id = IdGenerator.generate(),
                transactionId = transactionId,
                friendId = member.friendId,
                participantName = member.name,
                shareCents = member.shareCents,
                paidCents = member.paidCents,
                splitMethod = split!!.method,
            )
        }
        if (shares.isNotEmpty()) db.sharedExpenseDao().insertSplits(shares)
    }

    private suspend fun materializeDueRecurringTransactions() {
        val now = System.currentTimeMillis()
        db.recurringTransactionDao().getDue(now).forEach { rule ->
            val template = db.transactionDao().getById(rule.templateTransactionId) ?: return@forEach
            var next = rule.nextRunEpoch
            var created = 0
            while (next <= now && (rule.endEpoch == null || next <= rule.endEpoch) && created < 120) {
                val generated = template.copy(
                    id = IdGenerator.generate(),
                    source = "recurring",
                    timestampEpoch = next,
                    createdAt = now,
                    updatedAt = now,
                    recurringGroupId = rule.id,
                )
                db.transactionDao().insert(generated)
                val templateSplits = db.sharedExpenseDao().getSplits(template.id)
                if (templateSplits.isNotEmpty()) {
                    db.sharedExpenseDao().insertSplits(templateSplits.map {
                        it.copy(id = IdGenerator.generate(), transactionId = generated.id, settledCents = 0)
                    })
                }
                next = advanceRecurringDate(next, rule.interval)
                created++
            }
            db.recurringTransactionDao().update(
                rule.copy(nextRunEpoch = next, active = rule.endEpoch == null || next <= rule.endEpoch),
            )
        }
    }

    private fun advanceRecurringDate(epoch: Long, interval: String): Long =
        Calendar.getInstance().apply {
            timeInMillis = epoch
            when (interval) {
                "weekly" -> add(Calendar.WEEK_OF_YEAR, 1)
                "yearly" -> add(Calendar.YEAR, 1)
                else -> add(Calendar.MONTH, 1)
            }
        }.timeInMillis

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

    /**
     * Quietly repairs short listener/receiver gaps whenever the app resumes. Unlike the manual
     * history scan this only examines messages since the previous sync and never shows progress UI.
     */
    fun syncRecentMessages() {
        val app = getApplication<Application>()
        val prefs = app.getSharedPreferences("nudge_prefs", android.content.Context.MODE_PRIVATE)
        if (!prefs.getBoolean("auto_capture_enabled", true)) return
        val now = System.currentTimeMillis()
        val previousSync = prefs.getLong("last_realtime_sms_sync", now - 15 * 60_000L)
        if (now - previousSync < 30_000L) return
        // Advance early so several lifecycle callbacks cannot launch the same scan concurrently.
        prefs.edit().putLong("last_realtime_sms_sync", now).apply()
        viewModelScope.launch(Dispatchers.IO) {
            val processor = TransactionCaptureProcessor(app)
            val resolver = app.contentResolver
            val since = (previousSync - 2 * 60_000L).coerceAtLeast(0L)
            runCatching {
                resolver.query(
                    Telephony.Sms.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                    "${Telephony.Sms.TYPE} = ? AND ${Telephony.Sms.DATE} >= ?",
                    arrayOf(Telephony.Sms.MESSAGE_TYPE_INBOX.toString(), since.toString()),
                    "${Telephony.Sms.DATE} ASC",
                )
            }.getOrNull()?.use { cursor ->
                val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIndex)?.takeIf(String::isNotBlank) ?: continue
                    val id = cursor.getLong(idIndex)
                    val sender = cursor.getString(addressIndex).orEmpty()
                    processor.process(
                        rawText = body,
                        sourceId = sender,
                        source = "sms_realtime_sync",
                        receivedAt = cursor.getLong(dateIndex),
                        sourceMetadata = TransactionCaptureProcessor.SourceMetadata(
                            sender = sender,
                            originalMessageId = id.toString(),
                            originalMessageUri = ContentUris.withAppendedId(Telephony.Sms.CONTENT_URI, id).toString(),
                        ),
                    )
                }
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
                        bypassCaptureToggle = true,
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
                        bypassCaptureToggle = true,
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

    fun saveCategory(category: CategoryEntity) {
        viewModelScope.launch { db.categoryDao().insert(category) }
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

    fun importFriend(friend: FriendEntity) {
        viewModelScope.launch { db.sharedExpenseDao().upsertFriend(friend) }
    }

    fun importSplit(split: TransactionSplitEntity) {
        viewModelScope.launch {
            repeat(20) {
                if (db.transactionDao().getById(split.transactionId) != null) {
                    db.sharedExpenseDao().insertSplits(listOf(split))
                    return@launch
                }
                delay(50)
            }
        }
    }

    fun importRecurrence(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repeat(20) {
                if (db.transactionDao().getById(rule.templateTransactionId) != null) {
                    db.recurringTransactionDao().upsert(rule)
                    return@launch
                }
                delay(50)
            }
        }
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

    suspend fun deleteAllData(): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            // Room clears every current table in dependency-safe order and checkpoints the WAL.
            // This is both faster and less error-prone than maintaining a manual SQL list.
            db.clearAllTables()
            // Clearing user data must leave the app usable in the same session. Recreate the
            // built-in expense/income categories and selectable accounts immediately.
            DefaultsSeeder.seedIfEmpty(db)
            File(getApplication<Application>().filesDir, "receipts").deleteRecursively()
        }
        refreshWidget()
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

    private suspend fun repairCapturedSemantics() {
        val parser = DefaultSmsParserEngine()
        val categories = db.categoryDao().getAllOnce().associateBy { it.id }
        db.transactionDao().getAllOnce().filter { it.source != "manual" }.forEach { transaction ->
            val savedSource = db.savedSourceMessageDao().getByTransaction(transaction.id)
            val sourceBody = savedSource?.encryptedBody?.let { encrypted ->
                runCatching { sourceCrypto.decrypt(encrypted) }.getOrNull()
            } ?: withContext(Dispatchers.IO) { readOriginalSmsBody(savedSource?.originalMessageUri) }
            val parsed = sourceBody?.let { parser.parse(it, savedSource?.sender.orEmpty()) }
            val legacyCardPayment = transaction.type == "credit" &&
                Regex("""(?i)^your\s+card\s+ending\s+\d+\s+on\b""").containsMatchIn(transaction.merchantRaw)
            if (parsed != null || legacyCardPayment) {
                val repairedType = if (legacyCardPayment) TransactionType.TRANSFER else parsed!!.type
                val sender = savedSource?.sender.orEmpty().uppercase()
                val bank = when {
                    "HDFC" in sender -> "HDFC"
                    "ICICI" in sender -> "ICICI"
                    "SBI" in sender -> "SBI"
                    "AXIS" in sender -> "Axis"
                    "KOTAK" in sender -> "Kotak"
                    else -> null
                }
                val merchant = when {
                    repairedType == TransactionType.TRANSFER -> parsed?.merchantNormalized
                        ?.takeUnless { it == "Unknown merchant" }
                        ?: listOfNotNull(bank, "Credit Card Payment").joinToString(" ")
                    else -> parsed?.merchantNormalized
                        ?.takeUnless { it == "Unknown merchant" }
                        ?: transaction.merchantNormalized
                        ?: transaction.merchantRaw
                }
                val category = transaction.categoryId?.let(categories::get)
                val categoryStillValid = when (repairedType) {
                    TransactionType.DEBIT, TransactionType.REFUND -> category?.type?.lowercase() != "income"
                    TransactionType.CREDIT -> category?.type?.lowercase() != "expense"
                    TransactionType.TRANSFER -> false
                }
                val semanticsChanged = transaction.type != repairedType.name.lowercase()
                if (!semanticsChanged && merchant == transaction.merchantRaw) return@forEach
                db.transactionDao().update(
                    transaction.copy(
                        type = repairedType.name.lowercase(),
                        merchantRaw = merchant,
                        merchantNormalized = merchant,
                        categoryId = transaction.categoryId.takeIf { categoryStillValid },
                        isReviewed = if (semanticsChanged) false else transaction.isReviewed,
                        confidenceScore = maxOf(transaction.confidenceScore, parsed?.confidenceScore ?: .99f),
                        updatedAt = System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    private suspend fun removeInvalidCapturedStatements() {
        db.transactionDao().getAllOnce()
            .filter { transaction ->
                transaction.source != "manual" && !transaction.isReviewed && transaction.confidenceScore < .72f
            }
            .forEach { transaction ->
                val savedSource = db.savedSourceMessageDao().getByTransaction(transaction.id)
                val sourceText = savedSource?.encryptedBody?.let { encrypted ->
                    runCatching { sourceCrypto.decrypt(encrypted) }.getOrNull()
                }
                val isStatement = sourceText?.let(TransactionMessageGuard::isNonTransaction) == true ||
                    TransactionMessageGuard.isStatementExtractionArtifact(transaction.merchantRaw)
                if (isStatement) db.transactionDao().deleteById(transaction.id)
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
