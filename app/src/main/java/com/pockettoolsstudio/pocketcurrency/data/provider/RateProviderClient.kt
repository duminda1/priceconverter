package com.pockettoolsstudio.pocketcurrency.data.provider

import com.pockettoolsstudio.pocketcurrency.data.model.ApiRateResult
import com.pockettoolsstudio.pocketcurrency.domain.model.RateProvider

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
