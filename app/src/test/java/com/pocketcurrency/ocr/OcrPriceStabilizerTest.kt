package com.pocketcurrency.ocr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class OcrPriceStabilizerTest {

    @Test
    fun onFrame_requiresConsecutiveFrames_andInfersCurrency() {
        val stabilizer = OcrPriceStabilizer(
            priceExtractor = PriceExtractor(),
            minStableFrames = 2,
            confidenceThreshold = 0.65
        )
        val context = CurrencyContext(selectedFromCurrency = "USD", locale = Locale.US)

        val first = stabilizer.onFrame("Total 12.50", context)
        val second = stabilizer.onFrame("Total 12.50", context)

        assertNull(first)
        assertNotNull(second)
        assertEquals(12.50, second!!.amount, 0.001)
        assertEquals("USD", second.currencyCode)
        assertEquals(CurrencyConfidence.LOW, second.currencyConfidence)
        assertEquals(CurrencySource.USER_SELECTION, second.currencySource)
    }


    @Test
    fun onFrame_allowsIntegerPricesWithStability() {
        val stabilizer = OcrPriceStabilizer(
            priceExtractor = PriceExtractor(),
            minStableFrames = 2,
            confidenceThreshold = 0.65
        )
        val context = CurrencyContext(selectedFromCurrency = "USD", locale = Locale.US)

        val first = stabilizer.onFrame("Total 12", context)
        val second = stabilizer.onFrame("Total 12", context)

        assertNull(first)
        assertNotNull(second)
        assertEquals(12.0, second!!.amount, 0.001)
        assertEquals("USD", second.currencyCode)
    }

    @Test
    fun onFrame_usesUserSelectionForAmbiguousDollarAcrossFrames() {
        val stabilizer = OcrPriceStabilizer(
            priceExtractor = PriceExtractor(),
            minStableFrames = 2,
            confidenceThreshold = 0.65
        )
        val context = CurrencyContext(selectedFromCurrency = "USD", locale = Locale("en", "AU"))

        val first = stabilizer.onFrame("\$32.50", context)
        val second = stabilizer.onFrame("\$32.50", context)

        assertNull(first)
        assertNotNull(second)
        assertEquals("USD", second!!.currencyCode)
        assertEquals(CurrencyConfidence.MEDIUM, second.currencyConfidence)
        assertEquals(CurrencySource.USER_SELECTION, second.currencySource)
    }
    @Test
    fun onFrame_doesNotOverwriteHigherConfidenceValues() {
        val stabilizer = OcrPriceStabilizer(
            priceExtractor = PriceExtractor(),
            minStableFrames = 2,
            confidenceThreshold = 0.65
        )
        val context = CurrencyContext(selectedFromCurrency = "USD", locale = Locale.US)

        stabilizer.onFrame("58.75", context)
        val initial = stabilizer.onFrame("58.75", context)
        val partialFirst = stabilizer.onFrame("8.75", context)
        val partialSecond = stabilizer.onFrame("8.75", context)

        assertNotNull(initial)
        assertNull(partialFirst)
        assertNull(partialSecond)
    }
}
