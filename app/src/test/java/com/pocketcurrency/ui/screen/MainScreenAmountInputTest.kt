package com.pocketcurrency.ui.screen

import com.pocketcurrency.util.AmountInputFormatter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MainScreenAmountInputTest {

    @Test
    fun filterAmountInput_preservesUserSeparator() {
        assertEquals("3,20", invokeFilter("3,20"))
        assertEquals("3.20", invokeFilter("3.20"))
    }

    @Test
    fun filterAmountInput_usesLastSeparatorAsDecimal() {
        assertEquals("1234.56", invokeFilter("1,234.56"))
        assertEquals("1234,56", invokeFilter("1.234,56"))
    }

    @Test
    fun parseAmountInput_acceptsCommaAndDot() {
        assertEquals(3.2, invokeParse("3,20") ?: error("Expected parsed value"), 0.0001)
        assertEquals(3.2, invokeParse("3.20") ?: error("Expected parsed value"), 0.0001)
    }

    @Test
    fun parseAmountInput_handlesMixedSeparators() {
        assertEquals(1234.56, invokeParse("1,234.56") ?: error("Expected parsed value"), 0.0001)
        assertEquals(1234.56, invokeParse("1.234,56") ?: error("Expected parsed value"), 0.0001)
    }

    @Test
    fun parseAmountInput_returnsNullOnInvalid() {
        assertNull(invokeParse(","))
        assertNull(invokeParse("."))
        assertNull(invokeParse("abc"))
    }

    @Test
    fun formatAmountInput_usesLocale() {
        withLocale(Locale.GERMANY) {
            assertEquals("3,2", invokeFormat(3.2))
        }
        withLocale(Locale.US) {
            assertEquals("3.2", invokeFormat(3.2))
        }
    }

    private fun invokeFilter(input: String): String {
        return AmountInputFormatter.filterInput(input)
    }

    private fun invokeFormat(amount: Double): String {
        return AmountInputFormatter.formatAmount(amount)
    }

    private fun invokeParse(input: String): Double? {
        return AmountInputFormatter.parseInput(input)
    }

    private fun <T> withLocale(locale: Locale, block: () -> T): T {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(locale)
        return try {
            block()
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
