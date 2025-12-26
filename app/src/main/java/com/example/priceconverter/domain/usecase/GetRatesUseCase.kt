package com.example.priceconverter.domain.usecase

import com.example.priceconverter.data.model.CurrencyRate
import com.example.priceconverter.data.repository.ExchangeRateRepository

class GetRatesUseCase(private val repository: ExchangeRateRepository) {

    /**
     * Fetches the live conversion rate from API.
     * @param fromCurrency source currency
     * @param toCurrency target currency
     * @param amount amount to convert (default 1.0)
     */
    suspend fun execute(fromCurrency: String, toCurrency: String, amount: Double = 1.0): CurrencyRate? {
        return repository.getExchangeRate(fromCurrency, toCurrency, amount)
    }
}
