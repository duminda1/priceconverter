package com.priceconverter.data.repository

import com.priceconverter.data.api.ExchangeRateApi
import com.priceconverter.data.api.RetrofitInstance
import com.priceconverter.data.model.CurrencyRate
import com.priceconverter.domain.model.RateSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ApiRateResult(
    val rate: CurrencyRate?,
    val warningMessage: String?,
    val errorMessage: String?
)

class ExchangeRateRepository(private val settingsRepository: SettingsRepository) {

    private val api: ExchangeRateApi = RetrofitInstance.api

    /**
     * Fetch the live conversion rate from exchangeratesapi.io API.
     * Returns CurrencyRate or null if request fails.
     */
    suspend fun getExchangeRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double = 1.0
    ): ApiRateResult {
        return withContext(Dispatchers.IO) {
            try {
                val apiKey = settingsRepository.getApiKey()
                if (apiKey.isNullOrBlank()) {
                    return@withContext ApiRateResult(
                        rate = null,
                        warningMessage = null,
                        errorMessage = "API key not set. Add it in Settings."
                    )
                }

                val response = api.convert(
                    accessKey = apiKey,
                    from = fromCurrency.uppercase(),
                    to = toCurrency.uppercase(),
                    amount = amount
                )
                val warning = settingsRepository.recordApiRequest()
                val rateValue = response.info?.rate ?: response.result
                if (response.success && rateValue != null) {
                    val timestampMillis =
                        response.info?.timestamp?.times(1000) ?: System.currentTimeMillis()
                    ApiRateResult(
                        rate = CurrencyRate(
                            rate = rateValue,
                            lastUpdatedMillis = timestampMillis,
                            source = RateSource.REALTIME
                        ),
                        warningMessage = warning,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = warning,
                        errorMessage = response.error?.info ?: "API request failed"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = e.message ?: "API request failed"
                )
            }
        }
    }

    suspend fun verifyApiKey(apiKey: String): ApiRateResult {
        return withContext(Dispatchers.IO) {
            try {
                if (apiKey.isBlank()) {
                    return@withContext ApiRateResult(
                        rate = null,
                        warningMessage = null,
                        errorMessage = "API key is empty."
                    )
                }

                val response = api.convert(
                    accessKey = apiKey,
                    from = "USD",
                    to = "EUR",
                    amount = 1.0
                )
                val warning = settingsRepository.recordApiRequest()
                val rateValue = response.info?.rate ?: response.result
                if (response.success && rateValue != null) {
                    val timestampMillis =
                        response.info?.timestamp?.times(1000) ?: System.currentTimeMillis()
                    ApiRateResult(
                        rate = CurrencyRate(
                            rate = rateValue,
                            lastUpdatedMillis = timestampMillis,
                            source = RateSource.REALTIME
                        ),
                        warningMessage = warning,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = warning,
                        errorMessage = response.error?.info ?: "API key verification failed"
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = e.message ?: "API key verification failed"
                )
            }
        }
    }
}
