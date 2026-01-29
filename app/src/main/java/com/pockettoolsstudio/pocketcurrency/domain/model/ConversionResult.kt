package com.pockettoolsstudio.pocketcurrency.domain.model

import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyRate

data class ConversionResult(
    val from: Price,                 // original amount + currency
    val toCurrency: String,          // target currency
    val convertedAmount: Double,     // result
    val rate: CurrencyRate           // include lastUpdated
)
