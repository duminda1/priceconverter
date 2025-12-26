package com.priceconverter.ocr

import java.util.regex.Pattern

class PriceExtractor {

    /**
     * Extracts the first price found in the text.
     * Supports formats like: $12.50, 12.50 USD, 12,50 €, etc.
     */
    fun extractPrice(text: String): Double? {
        // Regex matches: optional currency symbol, digits, optional decimal
        val regex = Pattern.compile("""\d+([.,]\d+)?""")
        val matcher = regex.matcher(text)

        return if (matcher.find()) {
            val match = matcher.group().replace(",", ".")
            match.toDoubleOrNull()
        } else {
            null
        }
    }
}

