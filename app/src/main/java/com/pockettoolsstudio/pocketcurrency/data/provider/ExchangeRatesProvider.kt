package com.pockettoolsstudio.pocketcurrency.data.provider

import com.pockettoolsstudio.pocketcurrency.data.api.ExchangeRateApi
import com.pockettoolsstudio.pocketcurrency.data.api.ExchangeRateErrorMapper
import com.pockettoolsstudio.pocketcurrency.data.model.ApiRateResult
import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyRate
import com.pockettoolsstudio.pocketcurrency.domain.model.RateProvider
import com.pockettoolsstudio.pocketcurrency.domain.model.RateSource
import com.pockettoolsstudio.pocketcurrency.util.executeWithRetry
import com.pockettoolsstudio.pocketcurrency.util.mapNetworkExceptionToMessage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExchangeRatesProvider @Inject constructor(
    private val api: ExchangeRateApi
) : RateProviderClient {
    override val config = RateProvider.EXCHANGE_RATES

    override suspend fun fetchRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        apiKey: String?
    ): ApiRateResult {
        return withContext(Dispatchers.IO) {
            try {
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
                        warningMessage = null,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = null,
                        errorMessage = ExchangeRateErrorMapper.map(response.error)
                            ?: "Service unavailable. Please try again."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = mapNetworkExceptionToMessage(e)
                )
            }
        }
    }

    override suspend fun verifyApiKey(apiKey: String): ApiRateResult {
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
                        warningMessage = null,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = null,
                        errorMessage = ExchangeRateErrorMapper.map(response.error)
                            ?: "Unable to verify the API key."
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = mapNetworkExceptionToMessage(e)
                )
            }
        }
    }
}
