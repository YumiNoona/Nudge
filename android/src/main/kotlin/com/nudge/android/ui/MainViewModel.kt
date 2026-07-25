package com.nudge.android.ui

import android.app.Application
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

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db: NudgeDatabase

    // --- Flows ---
    val transactions: StateFlow<List<TransactionEntity>>
    val needsReviewCount: StateFlow<Int>
    val categories: StateFlow<List<CategoryEntity>>
    val accounts: StateFlow<List<AccountEntity>>
    val budgets: StateFlow<List<BudgetEntity>>
    val gamificationProfile: StateFlow<GamificationProfileEntity?>

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
        viewModelScope.launch { DefaultsSeeder.seedIfEmpty(db) }

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
        }
    }

    fun reviewTransaction(id: String, categoryId: String) {
        viewModelScope.launch {
            val txn = db.transactionDao().getById(id) ?: return@launch
            db.transactionDao().update(txn.copy(categoryId = categoryId, isReviewed = true))
            awardXp(com.nudge.engine.GamificationMath.XP_REVIEW_TRANSACTION)
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

    fun deleteTransaction(id: String) {
        viewModelScope.launch { db.transactionDao().deleteById(id) }
    }

    fun importTransaction(txn: TransactionEntity) {
        viewModelScope.launch { db.transactionDao().insert(txn) }
    }

    fun importCategory(cat: CategoryEntity) {
        viewModelScope.launch { db.categoryDao().insert(cat) }
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
