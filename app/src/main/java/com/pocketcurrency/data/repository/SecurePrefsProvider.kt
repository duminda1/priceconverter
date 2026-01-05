@file:Suppress("DEPRECATION")

package com.pocketcurrency.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pocketcurrency.util.Constants

internal object SecurePrefsProvider {
    fun create(context: Context): SharedPreferences {
        return try {
            createInternal(context)
        } catch (e: Exception) {
            // Covers AEADBadTagException, KeyStoreException, etc.
            // This happens when prefs are restored but keystore key is missing.

            // 1. Delete encrypted SharedPreferences
            runCatching {
                context.deleteSharedPreferences(Constants.PREFS_SECURE_NAME)
            }

            // 2. Delete master key entry (important!)
            runCatching {
                val ks = java.security.KeyStore.getInstance("AndroidKeyStore")
                ks.load(null)
                ks.deleteEntry(MasterKey.DEFAULT_MASTER_KEY_ALIAS)
            }

            // 3. Recreate fresh prefs
            createInternal(context)
        }
    }

    private fun createInternal(context: Context): SharedPreferences {
        return EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_SECURE_NAME,
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build(),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
