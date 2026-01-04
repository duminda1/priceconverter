package com.pocketcurrency.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.pocketcurrency.utils.Constants

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
                val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
                ks.deleteEntry(masterKeyAlias)
            }

            // 3. Recreate fresh prefs
            createInternal(context)
        }
    }

    private fun createInternal(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

        return EncryptedSharedPreferences.create(
            Constants.PREFS_SECURE_NAME,
            masterKeyAlias,
            context,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}
