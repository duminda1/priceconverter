package com.pocketcurrency.data.provider

import com.pocketcurrency.data.api.ExchangeRateApi
import com.pocketcurrency.data.model.ApiError
import com.pocketcurrency.data.model.ConvertResponse
import com.pocketcurrency.data.model.Info
import com.pocketcurrency.data.model.Query
import com.squareup.moshi.JsonDataException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRatesProviderTest {

    @Test
    fun fetchRate_mapsApiErrorToInvalidKeyMessage() = runBlocking {
        val response = ConvertResponse(
            success = false,
            query = Query(from = "USD", to = "AUD", amount = 1.0),
            info = Info(rate = null, timestamp = null),
            result = null,
            error = ApiError(code = 101, info = "Invalid access key.")
        )
        val api = FakeExchangeRateApi(response = response)
        val provider = ExchangeRatesProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = "KEY")

        assertEquals("Invalid API key. Please check it in Settings.", result.errorMessage)
    }

    @Test
    fun fetchRate_returnsExceptionMessageForInvalidJson() = runBlocking {
        val api = FakeExchangeRateApi(exception = JsonDataException("Malformed JSON"))
        val provider = ExchangeRatesProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = "KEY")

        assertEquals("Malformed JSON", result.errorMessage)
    }

    private class FakeExchangeRateApi(
        private val response: ConvertResponse? = null,
        private val exception: Exception? = null
    ) : ExchangeRateApi {
        override suspend fun convert(
            accessKey: String,
            from: String,
            to: String,
            amount: Double
        ): ConvertResponse {
            exception?.let { throw it }
            return response ?: ConvertResponse(
                success = false,
                query = null,
                info = null,
                result = null,
                error = null
            )
        }
    }
}
