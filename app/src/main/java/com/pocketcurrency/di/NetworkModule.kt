package com.pocketcurrency.di

import com.pocketcurrency.BuildConfig
import com.pocketcurrency.data.api.ExchangeRateApi
import com.pocketcurrency.data.api.FrankfurterApi
import com.pocketcurrency.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.URI
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
        return provideOkHttpClientWithPins(
            BuildConfig.EXCHANGE_RATE_API_PINS,
            BuildConfig.FRANKFURTER_API_PINS
        )
    }

    internal fun provideOkHttpClientWithPins(
        exchangePinsCsv: String,
        frankfurterPinsCsv: String
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
        buildCertificatePinnerForPins(exchangePinsCsv, frankfurterPinsCsv)
            ?.let { builder.certificatePinner(it) }
        return builder.build()
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

    internal fun buildCertificatePinnerForPins(
        exchangePinsCsv: String,
        frankfurterPinsCsv: String
    ): CertificatePinner? {
        val exchangeRatePins = parsePins(exchangePinsCsv)
        val frankfurterPins = parsePins(frankfurterPinsCsv)
        if (exchangeRatePins.isEmpty() && frankfurterPins.isEmpty()) {
            return null
        }

        val builder = CertificatePinner.Builder()
        addPins(builder, Constants.EXCHANGE_API_BASE_URL, exchangeRatePins)
        addPins(builder, Constants.FRANKFURTER_API_BASE_URL, frankfurterPins)
        return builder.build()
    }

    private fun parsePins(pinsCsv: String): List<String> {
        if (pinsCsv.isBlank()) {
            return emptyList()
        }
        return pinsCsv.split(',')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    private fun addPins(
        builder: CertificatePinner.Builder,
        baseUrl: String,
        pins: List<String>
    ) {
        if (pins.isEmpty()) {
            return
        }
        val host = runCatching { URI(baseUrl).host }.getOrNull()
        if (!host.isNullOrBlank()) {
            builder.add(host, *pins.toTypedArray())
        }
    }
}
