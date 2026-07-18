package com.nudge.android

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class NudgeApp : Application() {

    lateinit var encryptedPrefs: SharedPreferences
        private set

    override fun onCreate() {
        super.onCreate()

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        encryptedPrefs = EncryptedSharedPreferences.create(
            this,
            "nudge_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    companion object {
        const val PREFS_KEY_DB_PASSPHRASE = "db_passphrase"
        const val PREFS_KEY_USER_ID = "user_id"
    }
}
