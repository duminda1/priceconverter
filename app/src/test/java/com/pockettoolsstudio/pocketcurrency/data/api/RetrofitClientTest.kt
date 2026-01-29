package com.pockettoolsstudio.pocketcurrency.data.api

import com.pockettoolsstudio.pocketcurrency.di.NetworkModule
import com.pockettoolsstudio.pocketcurrency.data.repository.FakeSharedPreferences
import com.pockettoolsstudio.pocketcurrency.util.Constants
import okhttp3.CertificatePinner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.converter.gson.GsonConverterFactory

class NetworkModuleTest {

    private fun createPrefs() = FakeSharedPreferences()

    @Test
    fun retrofit_usesConfiguredBaseUrls() {
        val client = NetworkModule.provideOkHttpClient(createPrefs())
        val converterFactory = NetworkModule.provideGsonConverterFactory()
        val exchangeRetrofit =
            NetworkModule.provideExchangeRateRetrofit(client, converterFactory)
        val frankfurterRetrofit =
            NetworkModule.provideFrankfurterRetrofit(client, converterFactory)

        assertEquals(Constants.EXCHANGE_API_BASE_URL, exchangeRetrofit.baseUrl().toString())
        assertEquals(Constants.FRANKFURTER_API_BASE_URL, frankfurterRetrofit.baseUrl().toString())
    }

    @Test
    fun retrofit_includesGsonConverter() {
        val client = NetworkModule.provideOkHttpClient(createPrefs())
        val converterFactory = NetworkModule.provideGsonConverterFactory()
        val retrofit = NetworkModule.provideExchangeRateRetrofit(client, converterFactory)

        val factories = retrofit.converterFactories()
        assertTrue(factories.any { it is GsonConverterFactory })
        assertTrue(factories.none { it.javaClass.name.contains("Moshi") })
    }

    @Test
    fun buildCertificatePinner_ignoresBlankPins() {
        val pinner = NetworkModule.buildCertificatePinnerForPins(" , ", "  ")

        assertNull(pinner)
    }

    @Test
    fun buildCertificatePinner_acceptsValidPins() {
        val pinner = NetworkModule.buildCertificatePinnerForPins(
            " sha256/AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA= ",
            ""
        )

        assertNotNull(pinner)
        assertTrue(pinner != CertificatePinner.DEFAULT)
    }

    @Test
    fun buildCertificatePinner_ignoresInvalidPins() {
        val pinner = NetworkModule.buildCertificatePinnerForPins(
            "not-a-pin",
            ""
        )

        assertNull(pinner)
    }

}
