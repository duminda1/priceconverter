package com.priceconverter.data.model

data class ExchangeRateResponse(
    val success: Boolean,
    val info: Info
) {
    data class Info(
        val timestamp: Long,
        val quote: Double
    )
}
