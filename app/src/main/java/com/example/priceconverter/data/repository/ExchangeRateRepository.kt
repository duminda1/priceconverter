package com.example.priceconverter.data.repository

import com.example.priceconverter.data.api.ExchangeRateApi
import com.example.priceconverter.data.api.RetrofitInstance
import com.example.priceconverter.data.model.CurrencyRate
import com.example.priceconverter.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class ExchangeRateRepository {

    private val api: ExchangeRateApi = RetrofitInstance.api

    /**
     * Fetch the live conversion rate from exchangerate.host API.
     * Returns CurrencyRate or null if request fails.
     */
    suspend fun getExchangeRate(fromCurrency: String, toCurrency: String, amount: Double = 1.0): CurrencyRate? {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.convert(
                    accessKey = Constants.EXCHANGE_API_KEY,
                    from = fromCurrency.uppercase(),
                    to = toCurrency.uppercase(),
                    amount = amount
                )
                if (response.success) {
                    val rate = response.info.quote
                    val timestampMillis = response.info.timestamp * 1000 // seconds → ms
                    CurrencyRate(rate = rate, lastUpdatedMillis = timestampMillis)
                } else {
                    null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }
}
/*

 accessKey = Constants.EXCHANGE_API_KEY,
            from = from.uppercase(),
            to = to.uppercase(),
            amount = amount
 */