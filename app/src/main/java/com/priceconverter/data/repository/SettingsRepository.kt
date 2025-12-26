package com.priceconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.priceconverter.data.model.CurrencyPairRate
import com.priceconverter.utils.Constants
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

data class ApiUsageState(
    val monthKey: String,
    val count: Int,
    val warn50: Boolean,
    val warn75: Boolean,
    val warn90: Boolean
)

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences = createEncryptedPrefs(context)

    fun getService(): String {
        return prefs.getString(Constants.PREFS_SERVICE, Constants.EXCHANGE_API_SERVICE)
            ?: Constants.EXCHANGE_API_SERVICE
    }

    fun setService(service: String) {
        prefs.edit().putString(Constants.PREFS_SERVICE, service).apply()
    }

    fun getApiKey(): String? = prefs.getString(Constants.PREFS_API_KEY, null)

    fun setApiKey(apiKey: String) {
        prefs.edit().putString(Constants.PREFS_API_KEY, apiKey).apply()
    }

    fun hasApiKey(): Boolean = !getApiKey().isNullOrBlank()

    fun isFreePlan(): Boolean = prefs.getBoolean(Constants.PREFS_FREE_PLAN, true)

    fun setFreePlan(isFree: Boolean) {
        prefs.edit().putBoolean(Constants.PREFS_FREE_PLAN, isFree).apply()
    }

    fun isRealtimeEnabled(): Boolean = prefs.getBoolean(Constants.PREFS_REALTIME_ENABLED, false)

    fun setRealtimeEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREFS_REALTIME_ENABLED, enabled).apply()
    }

    fun isLiveScanEnabled(): Boolean = prefs.getBoolean(Constants.PREFS_LIVE_SCAN_ENABLED, true)

    fun setLiveScanEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(Constants.PREFS_LIVE_SCAN_ENABLED, enabled).apply()
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
        prefs.edit().putInt(Constants.PREFS_USAGE_COUNT, newCount).apply()

        if (!isFreePlan()) {
            return null
        }

        val warning = when {
            newCount >= 90 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_90, false) -> {
                prefs.edit().putBoolean(Constants.PREFS_USAGE_WARN_90, true).apply()
                "90% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 75 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_75, false) -> {
                prefs.edit().putBoolean(Constants.PREFS_USAGE_WARN_75, true).apply()
                "75% of your monthly API requests are used ($newCount/${Constants.FREE_PLAN_LIMIT})."
            }
            newCount >= 50 && !prefs.getBoolean(Constants.PREFS_USAGE_WARN_50, false) -> {
                prefs.edit().putBoolean(Constants.PREFS_USAGE_WARN_50, true).apply()
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
        prefs.edit().putString(Constants.PREFS_SAVED_RATES, encodeRates(updated)).apply()
    }

    fun removeSavedRate(from: String, to: String) {
        val normalized = normalizePair(from, to)
        val updated = getSavedRates().filterNot {
            it.from == normalized.first && it.to == normalized.second
        }
        prefs.edit().putString(Constants.PREFS_SAVED_RATES, encodeRates(updated)).apply()
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
        prefs.edit().putString(Constants.PREFS_MANUAL_RATES, encodeRates(updated)).apply()
    }

    fun removeManualRate(from: String, to: String) {
        val normalized = normalizePair(from, to)
        val updated = getManualRates().filterNot {
            it.from == normalized.first && it.to == normalized.second
        }
        prefs.edit().putString(Constants.PREFS_MANUAL_RATES, encodeRates(updated)).apply()
    }

    fun findManualRate(from: String, to: String): CurrencyPairRate? {
        val normalized = normalizePair(from, to)
        return getManualRates().firstOrNull {
            it.from == normalized.first && it.to == normalized.second
        }
    }

    private fun resetUsage(currentMonth: String) {
        prefs.edit()
            .putString(Constants.PREFS_USAGE_MONTH, currentMonth)
            .putInt(Constants.PREFS_USAGE_COUNT, 0)
            .putBoolean(Constants.PREFS_USAGE_WARN_50, false)
            .putBoolean(Constants.PREFS_USAGE_WARN_75, false)
            .putBoolean(Constants.PREFS_USAGE_WARN_90, false)
            .apply()
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
        return String.format("%04d-%02d", year, month)
    }

    private fun createEncryptedPrefs(context: Context): SharedPreferences {
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
}
