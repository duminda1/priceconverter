package com.pocketcurrency.data.repository

import com.pocketcurrency.data.api.ExchangeRateApi
import com.pocketcurrency.data.api.ExchangeRateErrorMapper
import com.pocketcurrency.data.api.RetrofitInstance
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.domain.model.RateSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

data class ApiRateResult(
    val rate: CurrencyRate?,
    val warningMessage: String?,
    val errorMessage: String?
)

class ExchangeRateRepository(private val settingsRepository: SettingsRepository) {

    private val api: ExchangeRateApi = RetrofitInstance.api
    private val maxRetryAttempts = 2

    /**
     * Fetch the live conversion rate from exchangerate.host API.
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
                        errorMessage = "API key missing. Add it in Settings to use Live rates."
                    )
                }

                val response = executeWithRetry {
                    api.convert(
                        accessKey = apiKey,
                        from = fromCurrency.uppercase(),
                        to = toCurrency.uppercase(),
                        amount = amount
                    )
                }
                val warning = settingsRepository.recordApiRequest()
                val rateValue = response.info?.rate ?: response.result
                if (response.success && rateValue != null) {
                    val timestampMillis =
                        response.info?.timestamp?.times(1000) ?: System.currentTimeMillis()
                    ApiRateResult(
                        rate = CurrencyRate(
                            rate = rateValue,
                            lastUpdatedMillis = timestampMillis,
                            source = RateSource.LIVE
                        ),
                        warningMessage = warning,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = warning,
                        errorMessage = ExchangeRateErrorMapper.map(response.error)
                            ?: "Service unavailable. Please try again."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = mapExceptionToMessage(e)
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

                val response = executeWithRetry {
                    api.convert(
                        accessKey = apiKey,
                        from = "USD",
                        to = "EUR",
                        amount = 1.0
                    )
                }
                val warning = settingsRepository.recordApiRequest()
                val rateValue = response.info?.rate ?: response.result
                if (response.success && rateValue != null) {
                    val timestampMillis =
                        response.info?.timestamp?.times(1000) ?: System.currentTimeMillis()
                    ApiRateResult(
                        rate = CurrencyRate(
                            rate = rateValue,
                            lastUpdatedMillis = timestampMillis,
                            source = RateSource.LIVE
                        ),
                        warningMessage = warning,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = warning,
                        errorMessage = ExchangeRateErrorMapper.map(response.error)
                            ?: "Unable to verify the API key."
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = mapExceptionToMessage(e)
                )
            }
        }
    }

    private suspend fun <T> executeWithRetry(block: suspend () -> T): T {
        var lastError: Exception? = null
        var delayMillis = 350L
        repeat(maxRetryAttempts) { attempt ->
            try {
                return block()
            } catch (e: Exception) {
                lastError = e
                val shouldRetry = shouldRetry(e) && attempt < maxRetryAttempts - 1
                if (!shouldRetry) {
                    throw e
                }
                delay(delayMillis)
                delayMillis *= 2
            }
        }
        throw lastError ?: IOException("Service unavailable.")
    }

    private fun shouldRetry(e: Exception): Boolean {
        return when (e) {
            is SocketTimeoutException,
            is UnknownHostException,
            is ConnectException,
            is IOException -> true
            is HttpException -> e.code() >= 500
            else -> false
        }
    }

    private fun mapExceptionToMessage(e: Exception): String {
        return when (e) {
            is UnknownHostException,
            is ConnectException -> "No internet connection. Please try again."
            is SocketTimeoutException -> "The service is taking too long. Please try again."
            is HttpException -> {
                if (e.code() >= 500) {
                    "Service unavailable. Please try again."
                } else {
                    "Unable to reach the service. Please try again."
                }
            }
            is IOException -> "No internet connection. Please try again."
            else -> e.message ?: "Service unavailable. Please try again."
        }
    }
}
