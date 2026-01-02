package com.pocketcurrency.data.api

import com.pocketcurrency.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.converter.gson.GsonConverterFactory

class RetrofitClientTest {

    @Test
    fun retrofit_usesConfiguredBaseUrls() {
        val exchangeRetrofit = RetrofitClient.retrofit(Constants.EXCHANGE_API_BASE_URL)
        val frankfurterRetrofit = RetrofitClient.retrofit(Constants.FRANKFURTER_API_BASE_URL)

        assertEquals(Constants.EXCHANGE_API_BASE_URL, exchangeRetrofit.baseUrl().toString())
        assertEquals(Constants.FRANKFURTER_API_BASE_URL, frankfurterRetrofit.baseUrl().toString())
    }

    @Test
    fun retrofit_includesGsonConverter() {
        val retrofit = RetrofitClient.retrofit(Constants.EXCHANGE_API_BASE_URL)

        val factories = retrofit.converterFactories()
        assertTrue(factories.any { it is GsonConverterFactory })
        assertTrue(factories.none { it.javaClass.name.contains("Moshi") })
    }
}
