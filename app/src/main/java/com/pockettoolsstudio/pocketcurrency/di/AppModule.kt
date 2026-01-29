package com.pockettoolsstudio.pocketcurrency.di

import com.pockettoolsstudio.pocketcurrency.data.provider.ExchangeRatesProvider
import com.pockettoolsstudio.pocketcurrency.data.provider.FrankfurterProvider
import com.pockettoolsstudio.pocketcurrency.data.provider.RateProviderRegistry
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideRateProviderRegistry(
        exchangeRatesProvider: ExchangeRatesProvider,
        frankfurterProvider: FrankfurterProvider
    ): RateProviderRegistry {
        return RateProviderRegistry(
            listOf(
                frankfurterProvider,
                exchangeRatesProvider
            )
        )
    }
}
