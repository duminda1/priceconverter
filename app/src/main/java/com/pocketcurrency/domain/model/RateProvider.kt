package com.pocketcurrency.domain.model

import com.pocketcurrency.util.Constants

enum class RateProvider(
    val id: String,
    val displayName: String,
    val requiresApiKey: Boolean,
    val supportsRealtime: Boolean,
    val supportsUsageTracking: Boolean
) {
    FRANKFURTER(
        id = Constants.PROVIDER_FRANKFURTER,
        displayName = Constants.PROVIDER_FRANKFURTER,
        requiresApiKey = false,
        supportsRealtime = false,
        supportsUsageTracking = false
    ),
    EXCHANGE_RATES(
        id = Constants.PROVIDER_EXCHANGE_RATES,
        displayName = Constants.PROVIDER_EXCHANGE_RATES,
        requiresApiKey = true,
        supportsRealtime = true,
        supportsUsageTracking = true
    )
}
