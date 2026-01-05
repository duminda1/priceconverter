package com.pocketcurrency.data.provider

import com.pocketcurrency.data.api.FrankfurterApi
import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.domain.model.RateProvider
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.utils.FrankfurterSchedule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FrankfurterProvider @Inject constructor(
    private val api: FrankfurterApi
) : RateProviderClient {
    override val config = RateProvider.FRANKFURTER

    override suspend fun fetchRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        apiKey: String?
    ): ApiRateResult {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.latest(
                    from = fromCurrency.uppercase(),
                    to = toCurrency.uppercase(),
                    amount = amount
                )
                val rateValue = response.rates[toCurrency.uppercase()]
                if (rateValue != null) {
                    val updatedMillis =
                        FrankfurterSchedule.updateMillisForDate(response.date.trim())
                            ?: FrankfurterSchedule.lastScheduledUpdateMillis(System.currentTimeMillis())
                    ApiRateResult(
                        rate = CurrencyRate(
                            rate = rateValue,
                            lastUpdatedMillis = updatedMillis,
                            source = RateSource.LIVE
                        ),
                        warningMessage = null,
                        errorMessage = null
                    )
                } else {
                    ApiRateResult(
                        rate = null,
                        warningMessage = null,
                        errorMessage = "Service unavailable. Please try again."
                    )
                }
            } catch (e: Exception) {
                ApiRateResult(
                    rate = null,
                    warningMessage = null,
                    errorMessage = mapExceptionToMessage(e)
                )
            }
        }
    }

    override suspend fun verifyApiKey(apiKey: String): ApiRateResult {
        return ApiRateResult(
            rate = null,
            warningMessage = null,
            errorMessage = "API key not required for ${config.displayName}."
        )
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
