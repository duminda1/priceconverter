package com.pocketcurrency.domain.usecase

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.repository.RateUpdateRepository

class GetRatesUseCase(private val repository: RateUpdateRepository) {

    /**
     * Fetches the live conversion rate from API.
     * @param fromCurrency source currency
     * @param toCurrency target currency
     * @param amount amount to convert (default 1.0)
     */
    suspend fun execute(fromCurrency: String, toCurrency: String, amount: Double = 1.0): ApiRateResult {
        return repository.fetchRate(
            fromCurrency = fromCurrency,
            toCurrency = toCurrency,
            amount = amount,
            saveOnSuccess = false
        )
    }
}
