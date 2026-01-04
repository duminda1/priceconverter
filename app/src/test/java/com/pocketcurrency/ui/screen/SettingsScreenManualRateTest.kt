package com.pocketcurrency.ui.screen

import com.pocketcurrency.data.model.CurrencyPairRate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsScreenManualRateTest {

    private val invalidCurrencyMessage = "invalid currency"
    private val currencyHelpMessage = "currency help"
    private val invalidRateMessage = "invalid rate"

    @Test
    fun validateManualRateInput_blankCurrencies_setsInvalidCurrencyErrors() {
        val result = validateManualRateInput(
            from = " ",
            to = "",
            rateInput = "1.2",
            invalidCurrencyMessage = invalidCurrencyMessage,
            currencyHelpMessage = currencyHelpMessage,
            invalidManualRateMessage = invalidRateMessage
        )

        assertEquals(invalidCurrencyMessage, result.fromError)
        assertEquals(invalidCurrencyMessage, result.toError)
        assertNull(result.rateError)
        assertFalse(result.isValid)
    }

    @Test
    fun validateManualRateInput_invalidCurrencyCodes_returnsHelp() {
        val result = validateManualRateInput(
            from = "US",
            to = "EU1",
            rateInput = "1.2",
            invalidCurrencyMessage = invalidCurrencyMessage,
            currencyHelpMessage = currencyHelpMessage,
            invalidManualRateMessage = invalidRateMessage
        )

        assertEquals(currencyHelpMessage, result.fromError)
        assertEquals(currencyHelpMessage, result.toError)
        assertFalse(result.isValid)
    }

    @Test
    fun validateManualRateInput_invalidRate_returnsRateError() {
        val nonNumeric = validateManualRateInput(
            from = "USD",
            to = "EUR",
            rateInput = "abc",
            invalidCurrencyMessage = invalidCurrencyMessage,
            currencyHelpMessage = currencyHelpMessage,
            invalidManualRateMessage = invalidRateMessage
        )
        assertEquals(invalidRateMessage, nonNumeric.rateError)
        assertFalse(nonNumeric.isValid)

        val zeroRate = validateManualRateInput(
            from = "USD",
            to = "EUR",
            rateInput = "0",
            invalidCurrencyMessage = invalidCurrencyMessage,
            currencyHelpMessage = currencyHelpMessage,
            invalidManualRateMessage = invalidRateMessage
        )
        assertEquals(invalidRateMessage, zeroRate.rateError)
        assertFalse(zeroRate.isValid)
    }

    @Test
    fun validateManualRateInput_validInput_normalizesValues() {
        val result = validateManualRateInput(
            from = " usd ",
            to = " eur ",
            rateInput = " 1.25 ",
            invalidCurrencyMessage = invalidCurrencyMessage,
            currencyHelpMessage = currencyHelpMessage,
            invalidManualRateMessage = invalidRateMessage
        )

        assertTrue(result.isValid)
        assertEquals("USD", result.normalizedFrom)
        assertEquals("EUR", result.normalizedTo)
        assertEquals(1.25, result.rateValue ?: 0.0, 0.0001)
    }

    @Test
    fun shouldRemoveOriginalManualRate_detectsPairChanges() {
        val original = CurrencyPairRate("USD", "EUR", 1.0, 10L)

        assertFalse(shouldRemoveOriginalManualRate(original, "USD", "EUR"))
        assertTrue(shouldRemoveOriginalManualRate(original, "USD", "JPY"))
        assertFalse(shouldRemoveOriginalManualRate(null, "USD", "JPY"))
    }
}
