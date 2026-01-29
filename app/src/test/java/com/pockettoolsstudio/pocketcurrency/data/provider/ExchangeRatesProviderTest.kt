package com.pockettoolsstudio.pocketcurrency.data.provider

import com.pockettoolsstudio.pocketcurrency.data.api.ExchangeRateApi
import com.pockettoolsstudio.pocketcurrency.data.model.ApiError
import com.pockettoolsstudio.pocketcurrency.data.model.ConvertResponse
import com.pockettoolsstudio.pocketcurrency.data.model.Info
import com.pockettoolsstudio.pocketcurrency.data.model.Query
import com.google.gson.JsonParseException
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
    fun fetchRate_mapsApiErrorToMonthlyLimitMessage() = runBlocking {
        val response = ConvertResponse(
            success = false,
            query = Query(from = "USD", to = "AUD", amount = 1.0),
            info = Info(rate = null, timestamp = null),
            result = null,
            error = ApiError(code = 104, info = "Monthly limit reached.")
        )
        val api = FakeExchangeRateApi(response = response)
        val provider = ExchangeRatesProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = "KEY")

        assertEquals(
            "Monthly API request limit reached. Upgrade your plan or wait for the next cycle.",
            result.errorMessage
        )
    }

    @Test
    fun fetchRate_mapsApiErrorToInvalidCurrencyMessage() = runBlocking {
        val response = ConvertResponse(
            success = false,
            query = Query(from = "USD", to = "AUD", amount = 1.0),
            info = Info(rate = null, timestamp = null),
            result = null,
            error = ApiError(code = 202, info = "Invalid currency code.")
        )
        val api = FakeExchangeRateApi(response = response)
        val provider = ExchangeRatesProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = "KEY")

        assertEquals("Invalid currency code(s). Check your selection.", result.errorMessage)
    }


    @Test
    fun fetchRate_usesTimestampSecondsToMillis() = runBlocking {
        val timestampSeconds = 1_700_000_000L
        val response = ConvertResponse(
            success = true,
            query = Query(from = "USD", to = "AUD", amount = 1.0),
            info = Info(rate = 1.5, timestamp = timestampSeconds),
            result = 1.5,
            error = null
        )
        val api = FakeExchangeRateApi(response = response)
        val provider = ExchangeRatesProvider(api)

        val result = provider.fetchRate("USD", "AUD", 1.0, apiKey = "KEY")

        val rate = result.rate ?: throw AssertionError("Expected rate")
        assertEquals(timestampSeconds * 1000L, rate.lastUpdatedMillis)
    }

    @Test
    fun fetchRate_returnsExceptionMessageForInvalidJson() = runBlocking {
        val api = FakeExchangeRateApi(exception = JsonParseException("Malformed JSON"))
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
