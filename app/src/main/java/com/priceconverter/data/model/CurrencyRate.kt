package com.priceconverter.data.model

import com.priceconverter.domain.model.RateSource

data class CurrencyRate(
    val rate: Double,
    val lastUpdatedMillis: Long,
    val source: RateSource
)
