package com.nudge.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString

/**
 * E2E Encrypted Sync Protocol — shared data structures.
 *
 * Design:
 * - Each record has a monotonic lastModifiedAt timestamp and deviceId
 * - Last-write-wins conflict resolution (simplest correct approach for personal use)
 * - All data encrypted with AES-256-GCM before leaving the device
 * - Server never sees plaintext — stores only encrypted blobs
 */

const val SYNC_PROTOCOL_VERSION = 1

// --- Sync Records ---

@Serializable
data class SyncRecord(
    val id: String,          // original entity UUID
    val table: SyncTable,    // which table this belongs to
    val action: SyncAction,  // create, update, delete
    val data: String,        // JSON-serialized entity data (empty for deletes)
    @SerialName("last_modified_at")
    val lastModifiedAt: Long, // epoch millis
    @SerialName("device_id")
    val deviceId: String,
    val version: Long = 1    // monotonically increasing version per record
)

@Serializable
enum class SyncTable {
    @SerialName("transactions") TRANSACTIONS,
    @SerialName("accounts") ACCOUNTS,
    @SerialName("categories") CATEGORIES,
    @SerialName("budgets") BUDGETS,
    @SerialName("gamification") GAMIFICATION,
    @SerialName("recurring_rules") RECURRING_RULES,
    @SerialName("merchant_aliases") MERCHANT_ALIASES
}

@Serializable
enum class SyncAction {
    @SerialName("create") CREATE,
    @SerialName("update") UPDATE,
    @SerialName("delete") DELETE
}

// --- Sync Blob (what gets encrypted and sent to server) ---

@Serializable
data class SyncBlob(
    @SerialName("device_id")
    val deviceId: String,
    val records: List<SyncRecord>,
    @SerialName("sync_timestamp")
    val syncTimestamp: Long,
    @SerialName("protocol_version")
    val protocolVersion: Int = SYNC_PROTOCOL_VERSION
)

// --- Push Request (encrypted blob + metadata) ---

@Serializable
data class SyncPushRequest(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("pairing_code")
    val pairingCode: String,
    val blobs: List<EncryptedBlobPayload>
)

@Serializable
data class EncryptedBlobPayload(
    @SerialName("encrypted_payload")
    val encryptedPayload: String, // Base64-encoded ciphertext
    @SerialName("blob_hash")
    val blobHash: String          // SHA-256 of encrypted payload
)

// --- Pull Response ---

@Serializable
data class SyncPullResponse(
    val blobs: List<SyncBlobServerEntry>,
    @SerialName("latest_sequence")
    val latestSequence: Long
)

@Serializable
data class SyncBlobServerEntry(
    val id: String,
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("encrypted_payload")
    val encryptedPayload: String,
    @SerialName("blob_hash")
    val blobHash: String,
    @SerialName("created_at")
    val createdAt: Long,
    val sequence: Long
)

// --- Device registration ---

@Serializable
data class SyncRegisterRequest(
    @SerialName("device_name")
    val deviceName: String
)

@Serializable
data class SyncRegisterResponse(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("pairing_code")
    val pairingCode: String
)

@Serializable
data class SyncPairRequest(
    @SerialName("pairing_code")
    val pairingCode: String,
    @SerialName("device_name")
    val deviceName: String
)

@Serializable
data class SyncPairResponse(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("pairing_code")
    val pairingCode: String,
    @SerialName("paired_with")
    val pairedWith: String
)

@Serializable
data class SyncStatusResponse(
    @SerialName("last_pull_at")
    val lastPullAt: Long,
    @SerialName("last_push_sequence")
    val lastPushSequence: Long,
    @SerialName("total_blobs_stored")
    val totalBlobsStored: Long,
    @SerialName("paired_devices")
    val pairedDevices: List<PairedDeviceInfo>
)

@Serializable
data class PairedDeviceInfo(
    @SerialName("device_id")
    val deviceId: String,
    val name: String,
    @SerialName("last_seen_at")
    val lastSeenAt: Long,
    @SerialName("is_current")
    val isCurrent: Boolean
)

// --- Sync Config (stored locally on each device) ---

@Serializable
data class SyncConfig(
    @SerialName("device_id")
    val deviceId: String,
    @SerialName("pairing_code")
    val pairingCode: String,
    @SerialName("server_url")
    val serverUrl: String,
    @SerialName("last_pull_sequence")
    val lastPullSequence: Long = 0,
    @SerialName("last_push_sequence")
    val lastPushSequence: Long = 0,
    @SerialName("is_enabled")
    val isEnabled: Boolean = true,
    @SerialName("last_sync_at")
    val lastSyncAt: Long = 0
)

// --- Merge Engine ---

object SyncMergeEngine {

    /**
     * Merge incoming sync records into local data.
     * Strategy: Last-write-wins by lastModifiedAt timestamp.
     * If timestamps are equal, the higher deviceId wins (deterministic tiebreaker).
     *
     * Returns: list of records to apply locally
     */
    fun merge(
        localRecords: List<SyncRecord>,
        incomingRecords: List<SyncRecord>
    ): List<SyncRecord> {
        val merged = mutableMapOf<String, SyncRecord>()

        // Index local records by ID
        for (record in localRecords) {
            merged[record.id] = record
        }

        // Apply incoming — only if newer
        for (record in incomingRecords) {
            val existing = merged[record.id]
            if (existing == null) {
                // New record
                merged[record.id] = record
            } else {
                // Resolve conflict: last-write-wins
                if (record.lastModifiedAt > existing.lastModifiedAt ||
                    (record.lastModifiedAt == existing.lastModifiedAt && record.deviceId > existing.deviceId)) {
                    merged[record.id] = record
                }
            }
        }

        return merged.values.toList()
    }

    /**
     * Filter out records that have already been applied (same or newer ID)
     */
    fun filterNew(
        incomingRecords: List<SyncRecord>,
        knownVersions: Map<String, Long> // recordId -> version
    ): List<SyncRecord> {
        return incomingRecords.filter { record ->
            val knownVersion = knownVersions[record.id] ?: 0
            record.version > knownVersion
        }
    }

    /**
     * Check if a record should be synced (has local changes not yet pushed)
     */
    fun needsSync(
        record: SyncRecord,
        lastSyncVersion: Long
    ): Boolean {
        return record.version > lastSyncVersion
    }
}

// --- JSON helpers ---

object SyncJson {
    val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        prettyPrint = false
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)

    inline fun <reified T> decode(text: String): T = json.decodeFromString(text)

    fun hashString(input: String): String {
        // Simple hash for blob dedup — in production, use SHA-256
        var hash = 0
        for (c in input) {
            hash = ((hash shl 5) - hash) + c.code
            hash = hash and hash // Convert to 32bit integer
        }
        return hash.toUInt().toString(16)
    }
}
