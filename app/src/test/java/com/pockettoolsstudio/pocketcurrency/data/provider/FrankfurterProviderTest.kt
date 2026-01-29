package com.pockettoolsstudio.pocketcurrency.data.provider

import com.pockettoolsstudio.pocketcurrency.data.api.FrankfurterApi
import com.pockettoolsstudio.pocketcurrency.data.model.FrankfurterResponse
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.UnknownHostException

class FrankfurterProviderTest {

    @Test
    fun fetchRate_mapsNetworkErrors() = runBlocking {
        val api = FakeFrankfurterApi(exception = UnknownHostException("offline"))
        val provider = FrankfurterProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = null)

        assertEquals("No internet connection. Please try again.", result.errorMessage)
    }

    private class FakeFrankfurterApi(
        private val response: FrankfurterResponse? = null,
        private val exception: Exception? = null
    ) : FrankfurterApi {
        override suspend fun latest(
            from: String,
            to: String,
            amount: Double
        ): FrankfurterResponse {
            exception?.let { throw it }
            return response ?: FrankfurterResponse(
                amount = amount,
                base = from,
                date = "2024-01-01",
                rates = mapOf(to to 1.0)
            )
        }
    }
}
