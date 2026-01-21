package com.pocketcurrency.data.model

import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Test

class ExchangeRateJsonParsingTest {

    @Test
    fun parseConvertResponse_withTimestampAndResult() {
        val json = """
            {
              "success": true,
              "query": {"from": "USD", "to": "EUR", "amount": 1},
              "info": {"timestamp": 1700000000, "rate": 0.91},
              "result": 0.91
            }
        """.trimIndent()

        val response = Gson().fromJson(json, ConvertResponse::class.java)
        val query = response.query ?: throw AssertionError("Expected query")
        val info = response.info ?: throw AssertionError("Expected info")
        val result = response.result ?: throw AssertionError("Expected result")
        val rate = info.rate ?: throw AssertionError("Expected rate")
        val timestamp = info.timestamp ?: throw AssertionError("Expected timestamp")

        assertEquals(true, response.success)
        assertEquals("USD", query.from)
        assertEquals("EUR", query.to)
        assertEquals(1.0, query.amount, 0.0)
        assertEquals(1700000000L, timestamp)
        assertEquals(0.91, rate, 0.0)
        assertEquals(0.91, result, 0.0)
    }

    @Test
    fun parseConvertResponse_withError() {
        val json = """
            {
              "success": false,
              "error": {"code": 104, "info": "Monthly limit reached."}
            }
        """.trimIndent()

        val response = Gson().fromJson(json, ConvertResponse::class.java)
        val error = response.error ?: throw AssertionError("Expected error")
        val code = error.code ?: throw AssertionError("Expected error code")
        val info = error.info ?: throw AssertionError("Expected error info")

        assertEquals(false, response.success)
        assertEquals(104, code)
        assertEquals("Monthly limit reached.", info)
    }
}
