package com.pocketcurrency.di

import com.pocketcurrency.data.provider.RateProviderRegistry
import com.pocketcurrency.data.provider.RateProviders
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    fun provideRateProviderRegistry(): RateProviderRegistry {
        return RateProviders.registry
    }
}
