package com.pocketcurrency.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class PriceExtractorTest {

    @Test
    fun extract_detectsCurrencyCodeAndAmount() {
        val extractor = PriceExtractor()

        val result = extractor.extract("Total: 12.50 AUD")

        assertNotNull(result)
        assertEquals(12.50, result!!.amount, 0.0001)
        assertEquals("AUD", result.currencyCode)
    }

    @Test
    fun extract_returnsNullWhenNoPriceFound() {
        val extractor = PriceExtractor()

        val result = extractor.extract("Hello World")

        assertNull(result)
    }

    @Test
    fun extract_parsesGroupedNumbersWithMixedSeparators() {
        val extractor = PriceExtractor()

        val cases = listOf(
            "Total: 123,456.78 USD" to Expected(123456.78, "USD"),
            "Total: 123.456,78 EUR" to Expected(123456.78, "EUR"),
            "₺1.000.000,00" to Expected(1_000_000.00, "TRY"),
            "1.000.000,00" to Expected(1_000_000.00, null),
            "1 234 567,89" to Expected(1_234_567.89, null)
        )

        cases.forEach { (text, expected) ->
            val result = extractor.extract(text)
            assertNotNull(result)
            assertEquals(expected.amount, result!!.amount, 0.0001)
            assertEquals(expected.currencyCode, result.currencyCode)
        }
    }

    private data class Expected(
        val amount: Double,
        val currencyCode: String?
    )
}
