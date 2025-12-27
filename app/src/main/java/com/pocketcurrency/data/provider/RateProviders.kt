package com.pocketcurrency.data.provider

import com.pocketcurrency.data.api.FrankfurterRetrofitInstance
import com.pocketcurrency.data.api.RetrofitInstance
import com.pocketcurrency.domain.model.RateProvider

object RateProviders {
    val registry: RateProviderRegistry by lazy {
        val providerClients: List<RateProviderClient> = RateProvider.values().map { provider ->
            when (provider) {
                RateProvider.FRANKFURTER ->
                    FrankfurterProvider(FrankfurterRetrofitInstance.api)
                RateProvider.EXCHANGE_RATES ->
                    ExchangeRatesProvider(RetrofitInstance.api)
            }
        }
        RateProviderRegistry(providerClients)
    }
}
