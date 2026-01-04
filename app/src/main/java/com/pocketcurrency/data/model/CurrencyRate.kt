package com.pocketcurrency.data.model

import com.pocketcurrency.domain.model.RateSource

data class CurrencyRate(
    val rate: Double,
    val lastUpdatedMillis: Long,
    val source: RateSource
) {
    val lastUpdatedAtMillis: Long
        get() = lastUpdatedMillis
}
