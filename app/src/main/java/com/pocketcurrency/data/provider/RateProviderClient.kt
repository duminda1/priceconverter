package com.pocketcurrency.data.provider

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.domain.model.RateProvider

interface RateProviderClient {
    val config: RateProvider

    suspend fun fetchRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double,
        apiKey: String?
    ): ApiRateResult

    suspend fun verifyApiKey(apiKey: String): ApiRateResult
}
