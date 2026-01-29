package com.pockettoolsstudio.pocketcurrency.data.model

data class CurrencyPairRate(
    val from: String,
    val to: String,
    val rate: Double,
    val lastUpdatedMillis: Long
) {
    val lastUpdatedAtMillis: Long
        get() = lastUpdatedMillis
}
