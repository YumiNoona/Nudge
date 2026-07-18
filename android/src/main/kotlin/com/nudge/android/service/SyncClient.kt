package com.nudge.android.service

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import androidx.work.*
import com.nudge.android.NudgeApp
import com.nudge.android.data.*
import com.nudge.sync.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.security.SecureRandom
import java.security.spec.KeySpec
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

private const val PREFS_KEY_SYNC_DEVICE_ID = "sync_device_id"
private const val PREFS_KEY_SYNC_PAIRING_CODE = "sync_pairing_code"
private const val PREFS_KEY_SYNC_SERVER_URL = "sync_server_url"
private const val PREFS_KEY_SYNC_LAST_PULL_SEQ = "sync_last_pull_seq"
private const val PREFS_KEY_SYNC_LAST_PUSH_SEQ = "sync_last_push_seq"
private const val PREFS_KEY_SYNC_ENABLED = "sync_enabled"
private const val PREFS_KEY_SYNC_LAST_SYNC_AT = "sync_last_sync_at"
private const val PREFS_KEY_SYNC_KNOWN_VERSIONS = "sync_known_versions"

object SyncCrypto {

    private const val ALGORITHM = "AES/GCM/NoPadding"
    private const val KEY_SIZE = 256
    private const val IV_SIZE = 12
    private const val TAG_SIZE = 128
    private const val PBKDF_ITERATIONS = 10_000
    private val SALT = "nudge_sync_salt_v1".toByteArray(Charsets.UTF_8)

    fun deriveKey(passphrase: String): SecretKey {
        val spec: KeySpec = PBEKeySpec(passphrase.toCharArray(), SALT, PBKDF_ITERATIONS, KEY_SIZE)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    fun encrypt(blob: SyncBlob, key: SecretKey): String {
        val plaintext = SyncJson.encode(blob).toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance(ALGORITHM)
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.ENCRYPT_MODE, key, spec)
        val ciphertext = cipher.doFinal(plaintext)
        val combined = iv + ciphertext
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encryptedBase64: String, key: SecretKey): SyncBlob {
        val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, IV_SIZE)
        val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)
        val cipher = Cipher.getInstance(ALGORITHM)
        val spec = GCMParameterSpec(TAG_SIZE, iv)
        cipher.init(Cipher.DECRYPT_MODE, key, spec)
        val plaintext = cipher.doFinal(ciphertext)
        return SyncJson.decode(plaintext.toString(Charsets.UTF_8))
    }
}

class SyncWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val app = applicationContext as NudgeApp
            val prefs = app.encryptedPrefs

            if (!prefs.getBoolean(PREFS_KEY_SYNC_ENABLED, false)) return@withContext Result.success()

            val deviceId = prefs.getString(PREFS_KEY_SYNC_DEVICE_ID, null) ?: return@withContext Result.success()
            val pairingCode = prefs.getString(PREFS_KEY_SYNC_PAIRING_CODE, null) ?: return@withContext Result.success()
            val serverUrl = prefs.getString(PREFS_KEY_SYNC_SERVER_URL, null) ?: return@withContext Result.success()
            val passphrase = prefs.getString(NudgeApp.PREFS_KEY_DB_PASSPHRASE, null)
                ?: return@withContext Result.success()

            val db = NudgeDatabase.getInstance(applicationContext, passphrase.toByteArray())

            val config = loadSyncConfig(prefs)
            val key = SyncCrypto.deriveKey(passphrase)

            val localRecords = gatherLocalChanges(db, config.lastSyncAt, deviceId)

            if (localRecords.isNotEmpty()) {
                val blob = SyncBlob(
                    deviceId = deviceId,
                    records = localRecords,
                    syncTimestamp = System.currentTimeMillis()
                )
                val encryptedPayload = SyncCrypto.encrypt(blob, key)
                val blobHash = hashPayload(encryptedPayload)

                val pushRequest = SyncPushRequest(
                    deviceId = deviceId,
                    pairingCode = pairingCode,
                    blobs = listOf(
                        EncryptedBlobPayload(
                            encryptedPayload = encryptedPayload,
                            blobHash = blobHash
                        )
                    )
                )

                val pushUrl = "${serverUrl.trimEnd('/')}/sync/push"
                val pushResponse = httpPost(pushUrl, SyncJson.encode(pushRequest))
                val now = System.currentTimeMillis()
                prefs.edit()
                    .putLong(PREFS_KEY_SYNC_LAST_SYNC_AT, now)
                    .putLong(PREFS_KEY_SYNC_LAST_PUSH_SEQ, config.lastPushSequence + 1)
                    .apply()
            }

            val pullSequence = prefs.getLong(PREFS_KEY_SYNC_LAST_PULL_SEQ, 0L)
            val pullUrl = "${serverUrl.trimEnd('/')}/sync/pull?since_sequence=$pullSequence"
            val pullResponseJson = httpPost(pullUrl, """{"device_id":"$deviceId","pairing_code":"$pairingCode"}""")
            val pullResponse: SyncPullResponse = SyncJson.decode(pullResponseJson)

            val incomingRecords = mutableListOf<SyncRecord>()
            for (entry in pullResponse.blobs) {
                val decrypted = SyncCrypto.decrypt(entry.encryptedPayload, key)
                incomingRecords.addAll(decrypted.records)
            }

            if (incomingRecords.isNotEmpty()) {
                applyIncomingRecords(db, incomingRecords, prefs)
            }

            val maxSequence = pullResponse.blobs.maxOfOrNull { it.sequence } ?: pullSequence
            prefs.edit()
                .putLong(PREFS_KEY_SYNC_LAST_PULL_SEQ, maxOf(pullSequence, maxSequence))
                .putLong(PREFS_KEY_SYNC_LAST_SYNC_AT, System.currentTimeMillis())
                .apply()

            Result.success()
        } catch (e: SyncHttpException) {
            if (e.code in 400..499) {
                Result.failure()
            } else {
                Result.retry()
            }
        } catch (e: java.net.ConnectException) {
            Result.retry()
        } catch (e: java.net.SocketTimeoutException) {
            Result.retry()
        } catch (e: java.io.IOException) {
            Result.retry()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun gatherLocalChanges(
        db: NudgeDatabase,
        lastSyncAt: Long,
        deviceId: String
    ): List<SyncRecord> {
        val records = mutableListOf<SyncRecord>()
        val now = System.currentTimeMillis()

        val transactions = db.transactionDao().getAll().first()
        for (txn in transactions) {
            if (lastSyncAt == 0L || txn.updatedAt > lastSyncAt) {
                records.add(
                    SyncRecord(
                        id = txn.id,
                        table = SyncTable.TRANSACTIONS,
                        action = if (txn.createdAt == txn.updatedAt) SyncAction.CREATE else SyncAction.UPDATE,
                        data = transactionToJson(txn),
                        lastModifiedAt = txn.updatedAt,
                        deviceId = deviceId,
                        version = txn.updatedAt
                    )
                )
            }
        }

        val accounts = db.accountDao().getAll().first()
        for (acct in accounts) {
            records.add(
                SyncRecord(
                    id = acct.id,
                    table = SyncTable.ACCOUNTS,
                    action = if (acct.isActive) SyncAction.CREATE else SyncAction.DELETE,
                    data = accountToJson(acct),
                    lastModifiedAt = now,
                    deviceId = deviceId,
                    version = now
                )
            )
        }

        val categories = db.categoryDao().getAll().first()
        for (cat in categories) {
            records.add(
                SyncRecord(
                    id = cat.id,
                    table = SyncTable.CATEGORIES,
                    action = if (cat.isArchived) SyncAction.DELETE else SyncAction.CREATE,
                    data = categoryToJson(cat),
                    lastModifiedAt = now,
                    deviceId = deviceId,
                    version = now
                )
            )
        }

        val budgets = db.budgetDao().getAll().first()
        for (budget in budgets) {
            records.add(
                SyncRecord(
                    id = budget.id,
                    table = SyncTable.BUDGETS,
                    action = SyncAction.CREATE,
                    data = budgetToJson(budget),
                    lastModifiedAt = now,
                    deviceId = deviceId,
                    version = now
                )
            )
        }

        val profile = db.gamificationDao().getProfile().first()
        if (profile != null) {
            records.add(
                SyncRecord(
                    id = profile.userId,
                    table = SyncTable.GAMIFICATION,
                    action = SyncAction.CREATE,
                    data = gamificationProfileToJson(profile),
                    lastModifiedAt = now,
                    deviceId = deviceId,
                    version = now
                )
            )
        }

        return records
    }

    private suspend fun applyIncomingRecords(
        db: NudgeDatabase,
        records: List<SyncRecord>,
        prefs: SharedPreferences
    ) {
        val knownVersions = loadKnownVersions(prefs)
        val newRecords = SyncMergeEngine.filterNew(records, knownVersions)

        for (record in newRecords) {
            when (record.table) {
                SyncTable.TRANSACTIONS -> {
                    val entity = parseTransactionFromJson(record.data)
                    when (record.action) {
                        SyncAction.DELETE -> db.transactionDao().deleteById(record.id)
                        else -> db.transactionDao().insert(entity)
                    }
                }
                SyncTable.ACCOUNTS -> {
                    val entity = parseAccountFromJson(record.data)
                    when (record.action) {
                        SyncAction.DELETE -> {
                            val existing = db.accountDao().getById(record.id)
                            if (existing != null) {
                                db.accountDao().delete(existing)
                            }
                        }
                        else -> db.accountDao().insert(entity)
                    }
                }
                SyncTable.CATEGORIES -> {
                    val entity = parseCategoryFromJson(record.data)
                    when (record.action) {
                        SyncAction.DELETE -> db.categoryDao().archive(record.id)
                        else -> db.categoryDao().insert(entity)
                    }
                }
                SyncTable.BUDGETS -> {
                    val entity = parseBudgetFromJson(record.data)
                    when (record.action) {
                        SyncAction.DELETE -> {
                            val budgets = db.budgetDao().getAll().first()
                            val existing = budgets.find { it.id == record.id }
                            if (existing != null) {
                                db.budgetDao().delete(existing)
                            }
                        }
                        else -> db.budgetDao().insert(entity)
                    }
                }
                SyncTable.GAMIFICATION -> {
                    val entity = parseGamificationProfileFromJson(record.data)
                    db.gamificationDao().upsert(entity)
                }
                SyncTable.RECURRING_RULES,
                SyncTable.MERCHANT_ALIASES -> {
                    // Not yet supported; skip
                }
            }
            knownVersions[record.id] = record.version
        }

        saveKnownVersions(prefs, knownVersions)
    }

    private suspend fun httpPost(url: String, body: String): String = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        try {
            connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.doOutput = true
            connection.connectTimeout = 30_000
            connection.readTimeout = 30_000

            connection.outputStream.use { os ->
                os.write(body.toByteArray(Charsets.UTF_8))
            }

            val responseCode = connection.responseCode
            val responseStream = if (responseCode in 200..299) {
                connection.inputStream
            } else {
                connection.errorStream
            }
            val responseBody = responseStream?.bufferedReader()?.readText() ?: ""

            if (responseCode !in 200..299) {
                throw SyncHttpException(responseCode, responseBody)
            }

            responseBody
        } finally {
            connection?.disconnect()
        }
    }

    private fun hashPayload(input: String): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun loadSyncConfig(prefs: SharedPreferences): SyncConfig {
        return SyncConfig(
            deviceId = prefs.getString(PREFS_KEY_SYNC_DEVICE_ID, "") ?: "",
            pairingCode = prefs.getString(PREFS_KEY_SYNC_PAIRING_CODE, "") ?: "",
            serverUrl = prefs.getString(PREFS_KEY_SYNC_SERVER_URL, "") ?: "",
            lastPullSequence = prefs.getLong(PREFS_KEY_SYNC_LAST_PULL_SEQ, 0L),
            lastPushSequence = prefs.getLong(PREFS_KEY_SYNC_LAST_PUSH_SEQ, 0L),
            isEnabled = prefs.getBoolean(PREFS_KEY_SYNC_ENABLED, false),
            lastSyncAt = prefs.getLong(PREFS_KEY_SYNC_LAST_SYNC_AT, 0L)
        )
    }

    private fun loadKnownVersions(prefs: SharedPreferences): MutableMap<String, Long> {
        val json = prefs.getString(PREFS_KEY_SYNC_KNOWN_VERSIONS, null) ?: return mutableMapOf()
        val map = mutableMapOf<String, Long>()
        try {
            val obj = JSONObject(json)
            for (key in obj.keys()) {
                map[key] = obj.getLong(key)
            }
        } catch (_: Exception) { }
        return map
    }

    private fun saveKnownVersions(prefs: SharedPreferences, versions: Map<String, Long>) {
        val obj = JSONObject()
        for ((k, v) in versions) {
            obj.put(k, v)
        }
        prefs.edit().putString(PREFS_KEY_SYNC_KNOWN_VERSIONS, obj.toString()).apply()
    }

    private fun transactionToJson(txn: TransactionEntity): String {
        return JSONObject().apply {
            put("id", txn.id)
            put("amount_cents", txn.amountCents)
            put("type", txn.type)
            put("currency", txn.currency)
            put("merchant_raw", txn.merchantRaw)
            put("merchant_normalized", txn.merchantNormalized?.let { it } ?: JSONObject.NULL)
            put("category_id", txn.categoryId?.let { it } ?: JSONObject.NULL)
            put("subcategory_id", txn.subcategoryId?.let { it } ?: JSONObject.NULL)
            put("account_id", txn.accountId)
            put("source", txn.source)
            put("confidence_score", txn.confidenceScore.toDouble())
            put("is_reviewed", txn.isReviewed)
            put("is_recurring", txn.isRecurring)
            put("recurring_group_id", txn.recurringGroupId?.let { it } ?: JSONObject.NULL)
            put("note", txn.note?.let { it } ?: JSONObject.NULL)
            put("tags_json", txn.tagsJson)
            put("timestamp_epoch", txn.timestampEpoch)
            put("created_at", txn.createdAt)
            put("updated_at", txn.updatedAt)
        }.toString()
    }

    private fun accountToJson(acct: AccountEntity): String {
        return JSONObject().apply {
            put("id", acct.id)
            put("name", acct.name)
            put("bank_name", acct.bankName?.let { it } ?: JSONObject.NULL)
            put("account_type", acct.accountType)
            put("last4_digits", acct.last4Digits?.let { it } ?: JSONObject.NULL)
            put("color", acct.color?.let { it } ?: JSONObject.NULL)
            put("icon", acct.icon?.let { it } ?: JSONObject.NULL)
            put("is_active", acct.isActive)
        }.toString()
    }

    private fun categoryToJson(cat: CategoryEntity): String {
        return JSONObject().apply {
            put("id", cat.id)
            put("name", cat.name)
            put("icon", cat.icon?.let { it } ?: JSONObject.NULL)
            put("color", cat.color?.let { it } ?: JSONObject.NULL)
            put("type", cat.type)
            put("is_default", cat.isDefault)
            put("is_archived", cat.isArchived)
            put("monthly_budget_cents", cat.monthlyBudgetCents?.let { it } ?: JSONObject.NULL)
            put("sort_order", cat.sortOrder)
        }.toString()
    }

    private fun budgetToJson(b: BudgetEntity): String {
        return JSONObject().apply {
            put("id", b.id)
            put("category_id", b.categoryId?.let { it } ?: JSONObject.NULL)
            put("amount_cents", b.amountCents)
            put("period", b.period)
            put("rollover_enabled", b.rolloverEnabled)
            put("start_date_epoch", b.startDateEpoch)
        }.toString()
    }

    private fun gamificationProfileToJson(p: GamificationProfileEntity): String {
        return JSONObject().apply {
            put("user_id", p.userId)
            put("xp_total", p.xpTotal)
            put("level", p.level)
            put("current_streak_days", p.currentStreakDays)
            put("longest_streak_days", p.longestStreakDays)
            put("last_activity_date", p.lastActivityDate?.let { it } ?: JSONObject.NULL)
            put("badges_json", p.badgesJson)
            put("challenges_json", p.challengesJson)
            put("streak_freezes", p.streakFreezes)
            put("consistent_days", p.consistentDays)
        }.toString()
    }

    private fun parseTransactionFromJson(json: String): TransactionEntity {
        val o = JSONObject(json)
        return TransactionEntity(
            id = o.getString("id"),
            amountCents = o.getLong("amount_cents"),
            type = o.getString("type"),
            currency = o.optString("currency", "INR"),
            merchantRaw = o.getString("merchant_raw"),
            merchantNormalized = o.optString("merchant_normalized", null),
            categoryId = o.optString("category_id", null),
            subcategoryId = o.optString("subcategory_id", null),
            accountId = o.optString("account_id", ""),
            source = o.optString("source", "manual"),
            sourceRawText = null,
            confidenceScore = o.optDouble("confidence_score", 1.0).toFloat(),
            isReviewed = o.optBoolean("is_reviewed", false),
            isRecurring = o.optBoolean("is_recurring", false),
            recurringGroupId = o.optString("recurring_group_id", null),
            note = o.optString("note", null),
            tagsJson = o.optString("tags_json", "[]"),
            timestampEpoch = o.getLong("timestamp_epoch"),
            createdAt = o.optLong("created_at", System.currentTimeMillis()),
            updatedAt = o.optLong("updated_at", System.currentTimeMillis())
        )
    }

    private fun parseAccountFromJson(json: String): AccountEntity {
        val o = JSONObject(json)
        return AccountEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            bankName = o.optString("bank_name", null),
            accountType = o.getString("account_type"),
            last4Digits = o.optString("last4_digits", null),
            color = o.optString("color", null),
            icon = o.optString("icon", null),
            isActive = o.optBoolean("is_active", true)
        )
    }

    private fun parseCategoryFromJson(json: String): CategoryEntity {
        val o = JSONObject(json)
        return CategoryEntity(
            id = o.getString("id"),
            name = o.getString("name"),
            icon = o.optString("icon", null),
            color = o.optString("color", null),
            type = o.getString("type"),
            isDefault = o.optBoolean("is_default", false),
            isArchived = o.optBoolean("is_archived", false),
            monthlyBudgetCents = if (o.isNull("monthly_budget_cents") || !o.has("monthly_budget_cents")) null else o.optLong("monthly_budget_cents"),
            sortOrder = o.optInt("sort_order", 0)
        )
    }

    private fun parseBudgetFromJson(json: String): BudgetEntity {
        val o = JSONObject(json)
        return BudgetEntity(
            id = o.getString("id"),
            categoryId = o.optString("category_id", null),
            amountCents = o.getLong("amount_cents"),
            period = o.getString("period"),
            rolloverEnabled = o.optBoolean("rollover_enabled", false),
            startDateEpoch = o.getLong("start_date_epoch")
        )
    }

    private fun parseGamificationProfileFromJson(json: String): GamificationProfileEntity {
        val o = JSONObject(json)
        return GamificationProfileEntity(
            userId = o.getString("user_id"),
            xpTotal = o.optLong("xp_total", 0),
            level = o.optInt("level", 1),
            currentStreakDays = o.optInt("current_streak_days", 0),
            longestStreakDays = o.optInt("longest_streak_days", 0),
            lastActivityDate = if (o.isNull("last_activity_date") || !o.has("last_activity_date")) null else o.optLong("last_activity_date"),
            badgesJson = o.optString("badges_json", "[]"),
            challengesJson = o.optString("challenges_json", "[]"),
            streakFreezes = o.optInt("streak_freezes", 0),
            consistentDays = o.optInt("consistent_days", 0)
        )
    }
}

class SyncManager(private val context: Context) {

    companion object {
        private const val UNIQUE_WORK_NAME = "nudge_sync"
        private const val PERIODIC_WORK_NAME = "nudge_sync_periodic"

        fun enqueueSync(context: Context) {
            val work = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(UNIQUE_WORK_NAME, ExistingWorkPolicy.REPLACE, work)
        }

        fun schedulePeriodicSync(context: Context) {
            val work = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(PERIODIC_WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, work)
        }

        fun cancelPeriodicSync(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_NAME)
        }

        fun isConfigured(context: Context): Boolean {
            val app = context.applicationContext as NudgeApp
            val prefs = app.encryptedPrefs
            return prefs.getBoolean(PREFS_KEY_SYNC_ENABLED, false) &&
                !prefs.getString(PREFS_KEY_SYNC_DEVICE_ID, null).isNullOrBlank() &&
                !prefs.getString(PREFS_KEY_SYNC_PAIRING_CODE, null).isNullOrBlank() &&
                !prefs.getString(PREFS_KEY_SYNC_SERVER_URL, null).isNullOrBlank()
        }

        suspend fun registerDevice(
            context: Context,
            serverUrl: String,
            deviceName: String
        ): Result<SyncConfig> = withContext(Dispatchers.IO) {
            try {
                val url = "${serverUrl.trimEnd('/')}/sync/register"
                val request = SyncRegisterRequest(deviceName = deviceName)
                val responseJson = httpPostSync(url, SyncJson.encode(request))
                val response: SyncRegisterResponse = SyncJson.decode(responseJson)

                val config = SyncConfig(
                    deviceId = response.deviceId,
                    pairingCode = response.pairingCode,
                    serverUrl = serverUrl
                )
                saveSyncConfig(context, config)
                Result.success(config)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun pairDevice(
            context: Context,
            serverUrl: String,
            pairingCode: String,
            deviceName: String
        ): Result<SyncConfig> = withContext(Dispatchers.IO) {
            try {
                val url = "${serverUrl.trimEnd('/')}/sync/pair"
                val request = SyncPairRequest(pairingCode = pairingCode, deviceName = deviceName)
                val responseJson = httpPostSync(url, SyncJson.encode(request))
                val response: SyncPairResponse = SyncJson.decode(responseJson)

                val config = SyncConfig(
                    deviceId = response.deviceId,
                    pairingCode = response.pairingCode,
                    serverUrl = serverUrl
                )
                saveSyncConfig(context, config)
                Result.success(config)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        suspend fun getSyncStatus(context: Context): Result<SyncStatusResponse> = withContext(Dispatchers.IO) {
            try {
                val app = context.applicationContext as NudgeApp
                val prefs = app.encryptedPrefs
                val deviceId = prefs.getString(PREFS_KEY_SYNC_DEVICE_ID, null) ?: ""
                val pairingCode = prefs.getString(PREFS_KEY_SYNC_PAIRING_CODE, null) ?: ""
                val serverUrl = prefs.getString(PREFS_KEY_SYNC_SERVER_URL, null)
                    ?: return@withContext Result.failure(IllegalStateException("Sync not configured"))

                val url = "${serverUrl.trimEnd('/')}/sync/status"
                val body = JSONObject().apply {
                    put("device_id", deviceId)
                    put("pairing_code", pairingCode)
                }.toString()
                val responseJson = httpPostSync(url, body)
                val response: SyncStatusResponse = SyncJson.decode(responseJson)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

        fun disconnect(context: Context) {
            val app = context.applicationContext as NudgeApp
            val prefs = app.encryptedPrefs
            prefs.edit()
                .putBoolean(PREFS_KEY_SYNC_ENABLED, false)
                .remove(PREFS_KEY_SYNC_DEVICE_ID)
                .remove(PREFS_KEY_SYNC_PAIRING_CODE)
                .remove(PREFS_KEY_SYNC_SERVER_URL)
                .remove(PREFS_KEY_SYNC_LAST_PULL_SEQ)
                .remove(PREFS_KEY_SYNC_LAST_PUSH_SEQ)
                .remove(PREFS_KEY_SYNC_LAST_SYNC_AT)
                .remove(PREFS_KEY_SYNC_KNOWN_VERSIONS)
                .apply()
            cancelPeriodicSync(context)
        }

        private fun saveSyncConfig(context: Context, config: SyncConfig) {
            val app = context.applicationContext as NudgeApp
            val prefs = app.encryptedPrefs
            prefs.edit()
                .putString(PREFS_KEY_SYNC_DEVICE_ID, config.deviceId)
                .putString(PREFS_KEY_SYNC_PAIRING_CODE, config.pairingCode)
                .putString(PREFS_KEY_SYNC_SERVER_URL, config.serverUrl)
                .putLong(PREFS_KEY_SYNC_LAST_PULL_SEQ, config.lastPullSequence)
                .putLong(PREFS_KEY_SYNC_LAST_PUSH_SEQ, config.lastPushSequence)
                .putBoolean(PREFS_KEY_SYNC_ENABLED, config.isEnabled)
                .putLong(PREFS_KEY_SYNC_LAST_SYNC_AT, config.lastSyncAt)
                .apply()
        }

        private fun httpPostSync(url: String, body: String): String {
            var connection: HttpURLConnection? = null
            try {
                connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.doOutput = true
                connection.connectTimeout = 30_000
                connection.readTimeout = 30_000

                connection.outputStream.use { os ->
                    os.write(body.toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                val responseStream = if (responseCode in 200..299) {
                    connection.inputStream
                } else {
                    connection.errorStream
                }
                val responseBody = responseStream?.bufferedReader()?.readText() ?: ""

                if (responseCode !in 200..299) {
                    throw SyncHttpException(responseCode, responseBody)
                }

                return responseBody
            } finally {
                connection?.disconnect()
            }
        }
    }
}

class SyncHttpException(val code: Int, message: String) : Exception("HTTP $code: $message")
