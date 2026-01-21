package com.pocketcurrency.di

import android.content.SharedPreferences
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
    fun provideOkHttpClient(
        @DefaultPrefs prefs: SharedPreferences
    ): OkHttpClient {
        // Pin rotation strategy: include current + next pins per host. Remote config can override
        // CSV lists by writing to prefs with a higher PIN_CONFIG_VERSION for zero-downtime rotation.
        val defaultExchangePins = if (BuildConfig.DEBUG) "" else BuildConfig.EXCHANGE_RATE_API_PINS
        val defaultFrankfurterPins =
            if (BuildConfig.DEBUG) "" else BuildConfig.FRANKFURTER_API_PINS
        val exchangePins = resolvePinsCsv(
            prefs = prefs,
            prefsKey = Constants.PREFS_EXCHANGE_RATE_PINS,
            buildConfigPinsCsv = defaultExchangePins
        )
        val frankfurterPins = resolvePinsCsv(
            prefs = prefs,
            prefsKey = Constants.PREFS_FRANKFURTER_PINS,
            buildConfigPinsCsv = defaultFrankfurterPins
        )
        return provideOkHttpClientWithPins(
            exchangePins,
            frankfurterPins
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

    // Remote rotation path: store CSV pins + PREFS_PIN_CONFIG_VERSION from a secure config source.
    // When the stored version >= BuildConfig.PIN_CONFIG_VERSION, the remote list overrides.
    private fun resolvePinsCsv(
        prefs: SharedPreferences,
        prefsKey: String,
        buildConfigPinsCsv: String
    ): String {
        val remoteVersion = prefs.getInt(Constants.PREFS_PIN_CONFIG_VERSION, 0)
        val useRemote = remoteVersion >= BuildConfig.PIN_CONFIG_VERSION
        val remotePins = if (useRemote) prefs.getString(prefsKey, null) else null
        val hasValidRemotePins = !remotePins.isNullOrBlank() &&
            parsePins(remotePins).isNotEmpty()
        return if (hasValidRemotePins) remotePins.orEmpty() else buildConfigPinsCsv
    }

    private fun parsePins(pinsCsv: String): List<String> {
        if (pinsCsv.isBlank()) {
            return emptyList()
        }
        return pinsCsv.split(',')
            .map { it.trim() }
            // Validate format so malformed remote config doesn't disable pinning.
            .filter { it.isNotEmpty() && isValidPin(it) }
    }

    private fun isValidPin(pin: String): Boolean {
        return when {
            pin.startsWith("sha256/") ->
                isValidBase64(pin.removePrefix("sha256/"), expectedLength = 44)
            pin.startsWith("sha1/") ->
                isValidBase64(pin.removePrefix("sha1/"), expectedLength = 28)
            else -> false
        }
    }

    private fun isValidBase64(value: String, expectedLength: Int): Boolean {
        if (value.length != expectedLength) {
            return false
        }
        return value.all { it.isLetterOrDigit() || it == '+' || it == '/' || it == '=' }
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
