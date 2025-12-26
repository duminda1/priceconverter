package com.priceconverter.domain.model

import com.priceconverter.data.model.CurrencyRate

data class ConversionResult(
    val from: Price,                 // original amount + currency
    val toCurrency: String,          // target currency
    val convertedAmount: Double,     // result
    val rate: CurrencyRate           // include lastUpdated
)
