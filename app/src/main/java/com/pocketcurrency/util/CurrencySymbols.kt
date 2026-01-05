package com.pocketcurrency.util

import java.util.Currency
import java.util.Locale

object CurrencySymbols {
    // Unambiguous symbols only.
    val safeSymbolToCode: Map<String, String> = mapOf(
        "A$" to "AUD",
        "NZ$" to "NZD",
        "HK$" to "HKD",
        "S$" to "SGD",
        "€" to "EUR",
        "₹" to "INR",
        "₽" to "RUB",
        "₺" to "TRY",
        "₩" to "KRW",
        "฿" to "THB",
        "₫" to "VND",
        "₪" to "ILS"
    )

    // Heuristic defaults for ambiguous symbols.
    val fallbackSymbolToCode: Map<String, String>
        get() = mapOf(
            "$" to (localDollarCurrencyCode() ?: "USD"),
            "¥" to "JPY",
            "£" to "GBP"
        )

    private fun localDollarCurrencyCode(locale: Locale = Locale.getDefault()): String? {
        return try {
            val currency = Currency.getInstance(locale)
            val symbol = currency.getSymbol(locale)
            if (symbol.contains("$")) currency.currencyCode else null
        } catch (e: Exception) {
            null
        }
    }
}
