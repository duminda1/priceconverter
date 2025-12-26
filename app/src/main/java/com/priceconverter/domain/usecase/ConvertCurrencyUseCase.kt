package com.priceconverter.domain.usecase

import com.priceconverter.data.model.CurrencyRate
import com.priceconverter.domain.model.Price
import com.priceconverter.domain.model.ConversionResult

class ConvertCurrencyUseCase {

    fun execute(price: Price, rate: CurrencyRate, toCurrency: String): ConversionResult {
        val convertedAmount = price.amount * rate.rate
        return ConversionResult(
            from = price,
            toCurrency = toCurrency,
            convertedAmount = convertedAmount,
            rate = rate
        )
    }
}
