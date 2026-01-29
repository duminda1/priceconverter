package com.pockettoolsstudio.pocketcurrency.data.model

import com.pockettoolsstudio.pocketcurrency.domain.model.RateSource

data class CurrencyRate(
    val rate: Double,
    val lastUpdatedMillis: Long,
    val source: RateSource
) {
    val lastUpdatedAtMillis: Long
        get() = lastUpdatedMillis
}
