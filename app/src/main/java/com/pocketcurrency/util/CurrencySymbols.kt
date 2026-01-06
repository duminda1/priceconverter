package com.pocketcurrency.util

object CurrencySymbols {
    // Unambiguous symbols only.
    val safeSymbolToCode: Map<String, String> = mapOf(
        "A$" to "AUD",
        "US$" to "USD",
        "NZ$" to "NZD",
        "HK$" to "HKD",
        "S$" to "SGD",
        "CN¥" to "CNY",
        "NT$" to "TWD",
        "RP" to "IDR",
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
            "$" to "USD",
            "¥" to "JPY",
            "£" to "GBP"
        )
}
