package com.pocketcurrency.di

import com.pocketcurrency.data.api.ExchangeRateApi
import com.pocketcurrency.data.api.FrankfurterApi
import com.pocketcurrency.utils.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ExchangeRateRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class FrankfurterRetrofit

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    @Provides
    @Singleton
    fun provideGsonConverterFactory(): GsonConverterFactory {
        return GsonConverterFactory.create()
    }

    @Provides
    @Singleton
    @ExchangeRateRetrofit
    fun provideExchangeRateRetrofit(
        client: OkHttpClient,
        converterFactory: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.EXCHANGE_API_BASE_URL)
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
    }

    @Provides
    @Singleton
    @FrankfurterRetrofit
    fun provideFrankfurterRetrofit(
        client: OkHttpClient,
        converterFactory: GsonConverterFactory
    ): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Constants.FRANKFURTER_API_BASE_URL)
            .client(client)
            .addConverterFactory(converterFactory)
            .build()
    }

    @Provides
    @Singleton
    fun provideExchangeRateApi(
        @ExchangeRateRetrofit retrofit: Retrofit
    ): ExchangeRateApi {
        return retrofit.create(ExchangeRateApi::class.java)
    }

    @Provides
    @Singleton
    fun provideFrankfurterApi(
        @FrankfurterRetrofit retrofit: Retrofit
    ): FrankfurterApi {
        return retrofit.create(FrankfurterApi::class.java)
    }
}
