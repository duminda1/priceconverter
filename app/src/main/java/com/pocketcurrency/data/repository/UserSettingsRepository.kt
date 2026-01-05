package com.pocketcurrency.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.pocketcurrency.di.DefaultPrefs
import com.pocketcurrency.utils.Constants
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

class UserSettingsRepository @Inject constructor(
    @DefaultPrefs private val prefs: SharedPreferences
) {
    fun getService(): String {
        val stored = prefs.getString(Constants.PREFS_SERVICE, null)
        val normalized = when (stored) {
            Constants.PROVIDER_EXCHANGE_RATES,
            Constants.PROVIDER_FRANKFURTER -> stored
            else -> Constants.DEFAULT_RATE_PROVIDER
        }
        if (normalized != stored) {
            prefs.edit {
                putString(Constants.PREFS_SERVICE, normalized)
            }
        }
        return normalized ?: Constants.DEFAULT_RATE_PROVIDER
    }

    fun setService(service: String) {
        prefs.edit {
            putString(Constants.PREFS_SERVICE, service)
        }
    }

    fun isFreePlan(): Boolean = prefs.getBoolean(Constants.PREFS_FREE_PLAN, true)

    fun setFreePlan(isFree: Boolean) {
        prefs.edit {
            putBoolean(Constants.PREFS_FREE_PLAN, isFree)
        }
    }

    fun isRealtimeEnabled(): Boolean = prefs.getBoolean(Constants.PREFS_REALTIME_ENABLED, false)

    fun setRealtimeEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(Constants.PREFS_REALTIME_ENABLED, enabled)
        }
    }

    fun isLiveScanEnabled(): Boolean = prefs.getBoolean(Constants.PREFS_LIVE_SCAN_ENABLED, true)

    fun setLiveScanEnabled(enabled: Boolean) {
        prefs.edit {
            putBoolean(Constants.PREFS_LIVE_SCAN_ENABLED, enabled)
        }
    }

    fun getHomeCurrency(): String {
        val stored = prefs.getString(Constants.PREFS_HOME_CURRENCY, null)
        if (!stored.isNullOrBlank()) {
            return stored
        }
        val initial = defaultCurrencyForLocale(Locale.getDefault(), Constants.DEFAULT_TARGET_CURRENCY)
        prefs.edit {
            putString(Constants.PREFS_HOME_CURRENCY, initial)
        }
        return initial
    }

    fun setHomeCurrency(currencyCode: String) {
        val normalized = currencyCode.trim().uppercase()
        if (normalized.isNotBlank()) {
            prefs.edit {
                putString(Constants.PREFS_HOME_CURRENCY, normalized)
            }
        }
    }

    fun isDestinationAuto(): Boolean =
        prefs.getBoolean(Constants.PREFS_DESTINATION_AUTO, true)

    fun setDestinationAuto(enabled: Boolean) {
        prefs.edit {
            putBoolean(Constants.PREFS_DESTINATION_AUTO, enabled)
        }
    }

    fun getDestinationCurrency(): String {
        val auto = isDestinationAuto()
        val stored = prefs.getString(Constants.PREFS_DESTINATION_CURRENCY, null)
        val localeCurrency = defaultCurrencyForLocale(Locale.getDefault(), Constants.DEFAULT_BASE_CURRENCY)
        return if (auto) {
            localeCurrency
        } else {
            stored?.takeIf { it.isNotBlank() } ?: localeCurrency
        }
    }

    fun setDestinationCurrency(currencyCode: String) {
        val normalized = currencyCode.trim().uppercase()
        if (normalized.isNotBlank()) {
            prefs.edit {
                putString(Constants.PREFS_DESTINATION_CURRENCY, normalized)
            }
        }
    }

    private fun defaultCurrencyForLocale(locale: Locale, fallback: String): String {
        return try {
            Currency.getInstance(locale).currencyCode
        } catch (e: Exception) {
            fallback
        }
    }
}
