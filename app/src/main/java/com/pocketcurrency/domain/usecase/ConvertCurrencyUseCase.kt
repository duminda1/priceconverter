package com.pocketcurrency.domain.usecase

import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.domain.model.Price
import com.pocketcurrency.domain.model.ConversionResult

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
