package com.pockettoolsstudio.pocketcurrency.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pockettoolsstudio.pocketcurrency.util.Constants

internal object DefaultPrefsProvider {
    fun create(context: Context, securePrefs: SharedPreferences): SharedPreferences {
        val prefs = context.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)
        PrefsMigration.migrateNonSecure(prefs, securePrefs)
        return prefs
    }
}

internal object PrefsMigration {
    internal const val PREFS_MIGRATION_KEY = "prefs_migrated_to_plain_v1"

    private val nonSecureKeys = setOf(
        Constants.PREFS_SELECTED_CURRENCIES,
        Constants.PREFS_SERVICE,
        Constants.PREFS_FREE_PLAN,
        Constants.PREFS_REALTIME_ENABLED,
        Constants.PREFS_LIVE_SCAN_ENABLED,
        Constants.PREFS_USAGE_MONTH,
        Constants.PREFS_USAGE_COUNT,
        Constants.PREFS_USAGE_WARN_50,
        Constants.PREFS_USAGE_WARN_75,
        Constants.PREFS_USAGE_WARN_90,
        Constants.PREFS_SAVED_RATES,
        Constants.PREFS_MANUAL_RATES,
        Constants.PREFS_HOME_CURRENCY,
        Constants.PREFS_DESTINATION_CURRENCY,
        Constants.PREFS_DESTINATION_AUTO
    )

    fun migrateNonSecure(defaultPrefs: SharedPreferences, securePrefs: SharedPreferences) {
        if (defaultPrefs.getBoolean(PREFS_MIGRATION_KEY, false)) {
            return
        }

        val secureValues = securePrefs.all
        if (secureValues.isEmpty()) {
            defaultPrefs.edit().putBoolean(PREFS_MIGRATION_KEY, true).apply()
            return
        }

        val keysToMigrate = buildSet {
            addAll(nonSecureKeys)
            secureValues.keys
                .filter { it.startsWith(Constants.PREFS_RATE_REFRESH_CHECK_PREFIX) }
                .forEach { add(it) }
        }

        val defaultEditor = defaultPrefs.edit()
        val secureEditor = securePrefs.edit()
        var removedFromSecure = false

        keysToMigrate.forEach { key ->
            if (!secureValues.containsKey(key)) return@forEach

            val wrote = copyValue(defaultEditor, key, secureValues[key])
            if (wrote) {
                secureEditor.remove(key)
                removedFromSecure = true
            }
        }

        defaultEditor.putBoolean(PREFS_MIGRATION_KEY, true).apply()
        if (removedFromSecure) {
            secureEditor.apply()
        }
    }

    private fun copyValue(
        editor: SharedPreferences.Editor,
        key: String,
        value: Any?
    ): Boolean {
        return when (value) {
            is String -> {
                editor.putString(key, value)
                true
            }
            is Boolean -> {
                editor.putBoolean(key, value)
                true
            }
            is Int -> {
                editor.putInt(key, value)
                true
            }
            is Long -> {
                editor.putLong(key, value)
                true
            }
            is Float -> {
                editor.putFloat(key, value)
                true
            }
            is Set<*> -> {
                @Suppress("UNCHECKED_CAST")
                val strings = value.filterIsInstance<String>().toSet()
                editor.putStringSet(key, strings)
                true
            }
            else -> false
        }
    }
}
