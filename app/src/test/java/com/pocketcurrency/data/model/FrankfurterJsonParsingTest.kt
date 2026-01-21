package com.pocketcurrency.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class FrankfurterJsonParsingTest {

    @Test
    fun parseLatestResponse_withRatesMap() {
        val json = """
            {
              "amount": 1.0,
              "base": "USD",
              "date": "2024-01-01",
              "rates": {"EUR": 0.91, "JPY": 150.1}
            }
        """.trimIndent()

        val response = Gson().fromJson(json, FrankfurterResponse::class.java)
        val eur = response.rates["EUR"] ?: throw AssertionError("Expected EUR rate")
        val jpy = response.rates["JPY"] ?: throw AssertionError("Expected JPY rate")

        assertEquals(1.0, response.amount, 0.0)
        assertEquals("USD", response.base)
        assertEquals("2024-01-01", response.date)
        assertEquals(0.91, eur, 0.0)
        assertEquals(150.1, jpy, 0.0)
    }
}
