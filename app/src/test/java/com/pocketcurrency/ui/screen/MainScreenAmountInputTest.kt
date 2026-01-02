package com.pocketcurrency.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.util.Locale

class MainScreenAmountInputTest {

    private val mainScreenKtClass = Class.forName("com.pocketcurrency.ui.screen.MainScreenKt")
    private val filterMethod = mainScreenKtClass.getDeclaredMethod(
        "filterAmountInput",
        String::class.java
    ).apply { isAccessible = true }
    private val formatMethod = mainScreenKtClass.getDeclaredMethod(
        "formatAmountInput",
        java.lang.Double.TYPE
    ).apply { isAccessible = true }
    private val parseMethod = mainScreenKtClass.getDeclaredMethod(
        "parseAmountInput",
        String::class.java
    ).apply { isAccessible = true }

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
        return filterMethod.invoke(null, input) as String
    }

    private fun invokeFormat(amount: Double): String {
        return formatMethod.invoke(null, amount) as String
    }

    private fun invokeParse(input: String): Double? {
        return parseMethod.invoke(null, input) as Double?
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
