package com.example.priceconverter.domain.usecase

import com.example.priceconverter.data.model.CurrencyRate
import com.example.priceconverter.domain.model.Price
import com.example.priceconverter.domain.model.ConversionResult

class ConvertCurrencyUseCase {

    fun execute(price: Price, rate: CurrencyRate, toCurrency: String, lastUpdated: String): ConversionResult {
        val convertedAmount = price.amount * rate.rate
        val updatedRate = rate.copy(lastUpdatedMillis = System.currentTimeMillis()) // update timestamp
        return ConversionResult(
            from = price,
            toCurrency = toCurrency,
            convertedAmount = convertedAmount,
            rate = updatedRate
        )
    }
}
