package com.pocketcurrency.data.api

import com.pocketcurrency.di.NetworkModule
import com.pocketcurrency.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.converter.gson.GsonConverterFactory

class NetworkModuleTest {

    @Test
    fun retrofit_usesConfiguredBaseUrls() {
        val client = NetworkModule.provideOkHttpClient()
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
        val client = NetworkModule.provideOkHttpClient()
        val converterFactory = NetworkModule.provideGsonConverterFactory()
        val retrofit = NetworkModule.provideExchangeRateRetrofit(client, converterFactory)

        val factories = retrofit.converterFactories()
        assertTrue(factories.any { it is GsonConverterFactory })
        assertTrue(factories.none { it.javaClass.name.contains("Moshi") })
    }
}
