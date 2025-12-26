package com.example.priceconverter.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.priceconverter.data.model.CurrencyRate
import com.example.priceconverter.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class CurrencyRepository(private val context: Context? = null) {

    private val prefs: SharedPreferences? =
        context?.getSharedPreferences(Constants.PREFS_NAME, Context.MODE_PRIVATE)

    private val cacheKey = "cached_rates"
    private val cacheTimeKey = "cached_rates_time"

    // Fetch from API (or return cached if offline)
    suspend fun getExchangeRate(from: String, to: String): CurrencyRate? {
        return withContext(Dispatchers.IO) {
            // Check cached first
            prefs?.let {
                val cachedJson = it.getString(cacheKey, null)
                val cachedTime = it.getLong(cacheTimeKey, 0L)
                if (cachedJson != null) {
                    val jsonObj = JSONObject(cachedJson)
                    val rateValue = jsonObj.optDouble(to, -1.0)
                    if (rateValue != -1.0) {
                        return@withContext CurrencyRate(rateValue, cachedTime)
                    }
                }
            }

            // TODO: call API (e.g., exchangerate.host) if not cached
            val rateFromApi = 1.0 // dummy, replace with actual API call
            val timestamp = System.currentTimeMillis()

            // Save to cache
            prefs?.edit()?.apply {
                putString(cacheKey, JSONObject().apply { put(to, rateFromApi) }.toString())
                putLong(cacheTimeKey, timestamp)
                apply()
            }

            return@withContext CurrencyRate(rateFromApi, timestamp)
        }
    }
}

