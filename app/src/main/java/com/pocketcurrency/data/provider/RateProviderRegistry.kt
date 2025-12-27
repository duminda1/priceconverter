package com.pocketcurrency.data.provider

import com.pocketcurrency.domain.model.RateProvider
import com.pocketcurrency.utils.Constants

class RateProviderRegistry(
    providers: List<RateProviderClient>
) {
    private val providerList = providers
    private val providerMap = providers.associateBy { it.config.id }

    fun getProvider(id: String): RateProviderClient {
        return providerMap[id] ?: providerMap.getValue(Constants.DEFAULT_RATE_PROVIDER)
    }

    fun getProviders(): List<RateProvider> {
        return RateProvider.values().toList()
    }
}
