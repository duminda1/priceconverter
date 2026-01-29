package com.pockettoolsstudio.pocketcurrency.domain.usecase

import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyRate
import com.pockettoolsstudio.pocketcurrency.domain.model.ConversionResult
import com.pockettoolsstudio.pocketcurrency.domain.model.Price
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor() {

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
