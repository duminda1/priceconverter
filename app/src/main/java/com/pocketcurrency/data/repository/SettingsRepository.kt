package com.pocketcurrency.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.utils.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Currency
import java.util.Locale

data class ApiUsageState(
    val monthKey: String,
    val count: Int,
    val warn50: Boolean,
    val warn75: Boolean,
    val warn90: Boolean
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        createEncryptedPrefs(context)
    }

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

    fun getApiKey(): String? = prefs.getString(Constants.PREFS_API_KEY, null)

    fun setApiKey(apiKey: String) {
        prefs.edit {
            putString(Constants.PREFS_API_KEY, apiKey)
        }
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

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

    fun getUsageState(): ApiUsageState {
        val currentMonth = currentMonthKey()
        val storedMonth = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth
        if (storedMonth != currentMonth) {
            resetUsage(currentMonth)
        }
        return ApiUsageState(
            monthKey = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth,
            count = prefs.getInt(Constants.PREFS_USAGE_COUNT, 0),
            warn50 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_50, false),
            warn75 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_75, false),
            warn90 = prefs.getBoolean(Constants.PREFS_USAGE_WARN_90, false)
        )
    }

    fun recordApiRequest(): String? {
        val currentMonth = currentMonthKey()
        val storedMonth = prefs.getString(Constants.PREFS_USAGE_MONTH, currentMonth) ?: currentMonth
        if (storedMonth != currentMonth) {
            resetUsage(currentMonth)
        }

        val newCount = prefs.getInt(Constants.PREFS_USAGE_COUNT, 0) + 1
        prefs.edit {
            putInt(Constants.PREFS_USAGE_COUNT, newCount)
        }

        if (!isFreePlan()) {
            return null
        }

        val warning = when {
            newCount >= 90 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_90, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_90, true)
                }
                "90% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 75 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_75, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_75, true)
                }
                "75% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 50 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_50, false) -> {
                prefs.edit {
                    putBoolean(Constants.PREFS_USAGE_WARN_50, true)
                }
                "50% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            else -> null
        }

        return warning
    }

    fun getSavedRates(): List<CurrencyPairRate> {
        return decodeRates(prefs.getString(Constants.PREFS_SAVED_RATES, null))
    }

    fun upsertSavedRate(rate: CurrencyPairRate) {
        val updated = upsertRate(getSavedRates(), rate)
        prefs.edit {
            putString(Constants.PREFS_SAVED_RATES, encodeRates(updated))
        }
    }

    fun removeSavedRate(from: String, to: String) {
        val normalized = normalizePair(from, to)
        val updated = getSavedRates().filterNot {
            it.from == normalized.first && it.to == normalized.second
        }
        prefs.edit {
            putString(Constants.PREFS_SAVED_RATES, encodeRates(updated))
        }
    }

    fun findSavedRate(from: String, to: String): CurrencyPairRate? {
        val normalized = normalizePair(from, to)
        return getSavedRates().firstOrNull {
            it.from == normalized.first && it.to == normalized.second
        }
    }

    fun getManualRates(): List<CurrencyPairRate> {
        return decodeRates(prefs.getString(Constants.PREFS_MANUAL_RATES, null))
    }

    fun upsertManualRate(rate: CurrencyPairRate) {
        val updated = upsertRate(getManualRates(), rate)
        prefs.edit {
            putString(Constants.PREFS_MANUAL_RATES, encodeRates(updated))
        }
    }

    fun removeManualRate(from: String, to: String) {
        val normalized = normalizePair(from, to)
        val updated = getManualRates().filterNot {
            it.from == normalized.first && it.to == normalized.second
        }
        prefs.edit {
            putString(Constants.PREFS_MANUAL_RATES, encodeRates(updated))
        }
    }

    fun findManualRate(from: String, to: String): CurrencyPairRate? {
        val normalized = normalizePair(from, to)
        return getManualRates().firstOrNull {
            it.from == normalized.first && it.to == normalized.second
        }
    }

    private fun resetUsage(currentMonth: String) {
        prefs.edit {
            putString(Constants.PREFS_USAGE_MONTH, currentMonth)
            putInt(Constants.PREFS_USAGE_COUNT, 0)
            putBoolean(Constants.PREFS_USAGE_WARN_50, false)
            putBoolean(Constants.PREFS_USAGE_WARN_75, false)
            putBoolean(Constants.PREFS_USAGE_WARN_90, false)
        }
    }

    private fun normalizePair(from: String, to: String): Pair<String, String> {
        return from.trim().uppercase() to to.trim().uppercase()
    }

    private fun upsertRate(
        existing: List<CurrencyPairRate>,
        rate: CurrencyPairRate
    ): List<CurrencyPairRate> {
        val normalized = normalizePair(rate.from, rate.to)
        val normalizedRate = rate.copy(from = normalized.first, to = normalized.second)
        return existing.filterNot {
            it.from == normalized.first && it.to == normalized.second
        } + normalizedRate
    }

    private fun encodeRates(rates: List<CurrencyPairRate>): String {
        val array = JSONArray()
        rates.forEach { rate ->
            array.put(
                JSONObject()
                    .put("from", rate.from)
                    .put("to", rate.to)
                    .put("rate", rate.rate)
                    .put("lastUpdatedMillis", rate.lastUpdatedMillis)
            )
        }
        return array.toString()
    }

    private fun decodeRates(json: String?): List<CurrencyPairRate> {
        if (json.isNullOrBlank()) return emptyList()
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<CurrencyPairRate>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    CurrencyPairRate(
                        from = obj.optString("from"),
                        to = obj.optString("to"),
                        rate = obj.optDouble("rate"),
                        lastUpdatedMillis = obj.optLong("lastUpdatedMillis")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun currentMonthKey(): String {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        return String.format(Locale.ROOT, "%04d-%02d", year, month)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
        return try {
            createEncryptedPrefsInternal(context)
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
            createEncryptedPrefsInternal(context)
        }
    }

    private fun createEncryptedPrefsInternal(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            Constants.PREFS_SECURE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun defaultCurrencyForLocale(locale: Locale, fallback: String): String {
        return try {
            Currency.getInstance(locale).currencyCode
        } catch (e: Exception) {
            fallback
        }
    }
}
