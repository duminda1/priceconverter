package com.pocketcurrency.ocr

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.util.Locale

class AmountParserTest {

    private val parser = AmountParser()
    private var originalLocale: Locale? = null

    @Before
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
    }

    @After
    fun tearDown() {
        originalLocale?.let { Locale.setDefault(it) }
    }

    @Test
    fun parse_handles_required_currency_formats() {
        val cases = listOf(
            Case("3.200,45 EUR", 3200.45, "EUR", "EU thousands dot + decimal comma with suffix code"),
            Case("2,500AUD", 2500.0, "AUD", "suffix ISO code adjacent to grouped integer"),
            Case("2,344USD", 2344.0, "USD", "suffix ISO code adjacent to grouped integer"),
            Case("\$54.23", 54.23, "USD", "ambiguous symbol fallback"),
            Case("A\$54.11", 54.11, "AUD", "safe multi-character symbol prefix"),
            Case("3,400SGD", 3400.0, "SGD", "suffix ISO code with comma grouping"),
            Case("NZ\$33", 33.0, "NZD", "safe multi-character symbol with integer"),
            Case("S\$11", 11.0, "SGD", "safe multi-character symbol with integer"),
            Case("HK\$45.11", 45.11, "HKD", "safe multi-character symbol with decimal dot"),
            Case("2,300₺", 2300.0, "TRY", "suffix safe symbol with grouping comma"),
            Case("2,300TRY", 2300.0, "TRY", "suffix ISO code with grouping comma"),
            Case("€23,54", 23.54, "EUR", "prefix safe symbol with decimal comma"),
            Case("23,45€", 23.45, "EUR", "suffix safe symbol with decimal comma"),
            Case("₹1,299.50", 1299.50, "INR", "prefix safe symbol with mixed separators"),
            Case("₽2 345,10", 2345.10, "RUB", "space grouping with decimal comma"),
            Case("¥500", 500.0, "JPY", "ambiguous symbol fallback to JPY")
        )

        cases.forEach { testCase ->
            val result = parser.parse(testCase.input)
            assertNotNull("Expected parse for ${testCase.reason}: ${testCase.input}", result)
            assertEquals(testCase.amount, result!!.amount, 0.0001)
            assertEquals(testCase.currencyCode, result.currencyCode)
        }
    }

    @Test
    fun parse_returnsNullOnInvalidInput() {
        val result = parser.parse("..,,€€")
        assertNull("Malformed OCR input should return null.", result)
    }

    @Test
    fun parse_resolvesDollarToLocaleCurrency() {
        val context = CurrencyContext(locale = Locale("en", "AU"))
        val result = parser.parse("\$54.23", context)
        assertNotNull("Expected parse for dollar symbol with AU locale.", result)
        assertEquals("AUD", result!!.currencyCode)
        assertEquals(CurrencyConfidence.MEDIUM, result.currencyConfidence)
        assertEquals(CurrencySource.LOCALE_DEFAULT, result.currencySource)
    }

    @Test
    fun parse_resolvesDollarToUserSelection() {
        val context = CurrencyContext(selectedFromCurrency = "USD", locale = Locale("en", "AU"))
        val result = parser.parse("\$32.50", context)
        assertNotNull("Expected parse for dollar symbol with user selection.", result)
        assertEquals("USD", result!!.currencyCode)
        assertEquals(CurrencyConfidence.MEDIUM, result.currencyConfidence)
        assertEquals(CurrencySource.USER_SELECTION, result.currencySource)
    }

    @Test
    fun parse_infersLocaleCurrencyWhenNoSymbol() {
        val context = CurrencyContext(locale = Locale("en", "AU"))
        val result = parser.parse("32.50", context)
        assertNotNull("Expected parse for no-symbol amount with locale fallback.", result)
        assertEquals("AUD", result!!.currencyCode)
        assertEquals(CurrencyConfidence.LOW, result.currencyConfidence)
        assertEquals(CurrencySource.LOCALE_DEFAULT, result.currencySource)
    }

    @Test
    fun parse_infersEuroForEuLocale() {
        val context = CurrencyContext(locale = Locale("fr", "FR"))
        val result = parser.parse("32,50", context)
        assertNotNull("Expected parse for EU locale amount.", result)
        assertEquals("EUR", result!!.currencyCode)
        assertEquals(CurrencyConfidence.LOW, result.currencyConfidence)
        assertEquals(CurrencySource.LOCALE_DEFAULT, result.currencySource)
    }

    @Test
    fun parse_detectsExplicitDollarSymbol() {
        val context = CurrencyContext(locale = Locale("en", "AU"))
        val result = parser.parse("A\$32.50", context)
        assertNotNull("Expected parse for explicit dollar symbol.", result)
        assertEquals("AUD", result!!.currencyCode)
        assertEquals(CurrencyConfidence.HIGH, result.currencyConfidence)
        assertEquals(CurrencySource.OCR_SYMBOL, result.currencySource)
    }

    private data class Case(
        val input: String,
        val amount: Double,
        val currencyCode: String,
        val reason: String
    )
}
