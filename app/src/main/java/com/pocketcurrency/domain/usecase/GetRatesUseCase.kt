package com.pocketcurrency.domain.usecase

import com.pocketcurrency.data.repository.ExchangeRateRepository
import com.pocketcurrency.data.repository.ApiRateResult

class GetRatesUseCase(private val repository: ExchangeRateRepository) {

    /**
     * Fetches the live conversion rate from API.
     * @param fromCurrency source currency
     * @param toCurrency target currency
     * @param amount amount to convert (default 1.0)
     */
    suspend fun execute(fromCurrency: String, toCurrency: String, amount: Double = 1.0): ApiRateResult {
        return repository.getExchangeRate(fromCurrency, toCurrency, amount)
    }
}
