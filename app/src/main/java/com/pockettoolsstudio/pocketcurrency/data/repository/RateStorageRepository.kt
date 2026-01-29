package com.pockettoolsstudio.pocketcurrency.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyPairRate
import com.pockettoolsstudio.pocketcurrency.di.DefaultPrefs
import com.pockettoolsstudio.pocketcurrency.util.Constants
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject

class RateStorageRepository @Inject constructor(
    @DefaultPrefs private val prefs: SharedPreferences
) {
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
}
