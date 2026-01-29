package com.pockettoolsstudio.pocketcurrency.ui.component

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PriceCardFormatTest {

    @Test
    fun formatDisplayAmount_keepsPrecisionForSmallValues() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val value = 2400.0 * 0.000057
            val formatted = formatDisplayAmount(value)
            assertEquals("0.1368", formatted)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }

    @Test
    fun formatDisplayAmount_keepsTwoDecimalsForLargerValues() {
        val previousLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        try {
            val formatted = formatDisplayAmount(1234.5)
            assertEquals("1,234.50", formatted)
        } finally {
            Locale.setDefault(previousLocale)
        }
    }
}
