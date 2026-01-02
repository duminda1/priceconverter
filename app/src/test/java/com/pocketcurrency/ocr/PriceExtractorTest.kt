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

    @Test
    fun parses_eu_format_with_thousands_dot_and_decimal_comma() {
        val extractor = PriceExtractor()
        val result = extractor.extract("3.200,48 EUR")
        assertNotNull(result)
        assertEquals(3200.48, result!!.amount, 0.001)
    }

    @Test
    fun parses_large_eu_format() {
        val extractor = PriceExtractor()
        val result = extractor.extract("₺1.000.000,00")
        assertNotNull(result)
        assertEquals(1_000_000.00, result!!.amount, 0.001)
    }

    @Test
    fun parses_us_format_with_thousands_comma() {
        val extractor = PriceExtractor()
        val result = extractor.extract("3,200.48 USD")
        assertNotNull(result)
        assertEquals(3200.48, result!!.amount, 0.001)
    }

    @Test
    fun parses_plain_us_format() {
        val extractor = PriceExtractor()
        val result = extractor.extract("3200.48")
        assertNotNull(result)
        assertEquals(3200.48, result!!.amount, 0.001)
    }

    @Test
    fun parses_mixed_separators_using_rightmost_decimal() {
        val extractor = PriceExtractor()
        val result = extractor.extract("3.200.48")
        assertNotNull(result)
        assertEquals(3200.48, result!!.amount, 0.001)
    }

    @Test
    fun parses_comma_comma_artifact() {
        val extractor = PriceExtractor()
        val result = extractor.extract("3,200,48")
        assertNotNull(result)
        assertEquals(3200.48, result!!.amount, 0.001)
    }

    @Test
    fun parses_integer_with_grouping() {
        val extractor = PriceExtractor()
        val result = extractor.extract("1.000")
        assertNotNull(result)
        assertEquals(1000.0, result!!.amount, 0.001)
    }

    @Test
    fun parses_integer_without_separators() {
        val extractor = PriceExtractor()
        val result = extractor.extract("5000")
        assertNotNull(result)
        assertEquals(5000.0, result!!.amount, 0.001)
    }

    @Test
    fun ignores_currency_symbols() {
        val extractor = PriceExtractor()
        val result = extractor.extract("€48,99")
        assertNotNull(result)
        assertEquals(48.99, result!!.amount, 0.001)
    }

    @Test
    fun ignores_prefix_and_suffix_text() {
        val extractor = PriceExtractor()
        val result = extractor.extract("Total: $19.95 incl tax")
        assertNotNull(result)
        assertEquals(19.95, result!!.amount, 0.001)
    }

    @Test
    fun detects_multi_character_currency_symbols() {
        val extractor = PriceExtractor()

        val cases = listOf(
            "A$12.50" to Expected(12.50, "AUD"),
            "NZ$19.95" to Expected(19.95, "NZD"),
            "HK$7.00" to Expected(7.00, "HKD"),
            "S$3.25" to Expected(3.25, "SGD")
        )

        cases.forEach { (text, expected) ->
            val result = extractor.extract(text)
            assertNotNull(result)
            assertEquals(expected.amount, result!!.amount, 0.001)
            assertEquals(expected.currencyCode, result.currencyCode)
        }
    }

    @Test
    fun returns_null_when_no_number_found() {
        val extractor = PriceExtractor()
        val result = extractor.extract("No price here")
        assertNull(result)
    }

    @Test
    fun returns_null_on_garbage_input() {
        val extractor = PriceExtractor()
        val result = extractor.extract("..,,€€")
        assertNull(result)
    }

    private data class Expected(
        val amount: Double,
        val currencyCode: String?
    )
}
