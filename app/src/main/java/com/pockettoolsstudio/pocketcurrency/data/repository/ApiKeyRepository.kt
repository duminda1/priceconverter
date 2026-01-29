package com.pockettoolsstudio.pocketcurrency.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.pockettoolsstudio.pocketcurrency.di.SecurePrefs
import com.pockettoolsstudio.pocketcurrency.util.Constants
import javax.inject.Inject

class ApiKeyRepository @Inject constructor(
    @SecurePrefs private val prefs: SharedPreferences
) {
    fun getApiKey(): String? = prefs.getString(Constants.PREFS_API_KEY, null)

    fun setApiKey(apiKey: String) {
        prefs.edit {
            putString(Constants.PREFS_API_KEY, apiKey)
        }
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()
}
