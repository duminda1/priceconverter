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
}
