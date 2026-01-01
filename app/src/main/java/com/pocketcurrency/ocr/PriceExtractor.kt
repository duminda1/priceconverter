package com.pocketcurrency.ocr

import java.util.Currency
import java.util.Locale
import java.util.regex.Pattern
import java.math.BigDecimal
import com.pocketcurrency.utils.CurrencySymbols

data class DetectedPrice(
    val amount: Double,
    val currencyCode: String?,
    val isConfident: Boolean
)

class PriceExtractor {

    private val numberPattern = Pattern.compile(
        """(?:\d{1,3}(?:[., ]\d{3})+(?:[.,]\d+)?|\d+(?:[.,]\d+)?)"""
    )
    private val codePattern = Pattern.compile("""\b[A-Z]{3}\b""")
    private val symbolPattern = Pattern.compile("""[€$£¥₹₩₱₫฿₴₦₪₺]""")
    private val currencyCodes = Currency.getAvailableCurrencies()
        .map { it.currencyCode }
        .toSet()
    private val symbolToCode = CurrencySymbols.symbolToCode

    /**
     * Extracts the most likely price and optional currency information from OCR text.
     */
    fun extract(text: String): DetectedPrice? {
        val normalized = text.uppercase(Locale.getDefault())
        val currencyTokens = findCurrencyTokens(normalized)
        val matcher = numberPattern.matcher(normalized)
        var best: DetectedPrice? = null
        var bestScore = 0.0

        while (matcher.find()) {
            val match = matcher.group()
            val amount = parseAmount(match) ?: continue
            val matchStart = matcher.start()
            val matchEnd = matcher.end()
            val currencyMatch = findNearestCurrency(currencyTokens, matchStart, matchEnd)
            val score = currencyMatch?.confidence ?: 0.0
            val candidate = DetectedPrice(
                amount = amount,
                currencyCode = currencyMatch?.code,
                isConfident = currencyMatch?.isConfident == true
            )
            if (best == null || score > bestScore) {
                best = candidate
                bestScore = score
            }
        }

        return best
    }

    private fun parseAmount(token: String): Double? {
        val normalized = normalizeNumberToken(token) ?: return null
        return try {
            BigDecimal(normalized).toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun normalizeNumberToken(token: String): String? {
        val lastDot = token.lastIndexOf('.')
        val lastComma = token.lastIndexOf(',')
        val decimalIndex = maxOf(lastDot, lastComma)
        val sb = StringBuilder(token.length)

        for (i in token.indices) {
            val ch = token[i]
            when {
                ch.isDigit() -> sb.append(ch)
                decimalIndex >= 0 && i == decimalIndex && (ch == '.' || ch == ',') -> sb.append('.')
                ch == '.' || ch == ',' || ch == ' ' -> {
                    // Skip grouping separators.
                }
            }
        }

        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun findCurrencyTokens(text: String): List<CurrencyToken> {
        val tokens = mutableListOf<CurrencyToken>()
        val symbolMatcher = symbolPattern.matcher(text)
        while (symbolMatcher.find()) {
            val symbol = symbolMatcher.group()
            val code = symbolToCode[symbol]
            if (code != null) {
                tokens.add(
                    CurrencyToken(
                        code = code,
                        start = symbolMatcher.start(),
                        end = symbolMatcher.end(),
                        baseConfidence = 0.9
                    )
                )
            }
        }

        val codeMatcher = codePattern.matcher(text)
        while (codeMatcher.find()) {
            val code = codeMatcher.group()
            if (currencyCodes.contains(code)) {
                tokens.add(
                    CurrencyToken(
                        code = code,
                        start = codeMatcher.start(),
                        end = codeMatcher.end(),
                        baseConfidence = 0.8
                    )
                )
            }
        }
        return tokens
    }

    private fun findNearestCurrency(
        tokens: List<CurrencyToken>,
        numberStart: Int,
        numberEnd: Int
    ): CurrencyMatch? {
        var best: CurrencyMatch? = null
        tokens.forEach { token ->
            val distance = when {
                token.end <= numberStart -> numberStart - token.end
                token.start >= numberEnd -> token.start - numberEnd
                else -> 0
            }
            val proximity = proximityScore(distance)
            val confidence = token.baseConfidence * proximity
            if (best == null || confidence > best!!.confidence) {
                best = CurrencyMatch(
                    code = token.code,
                    confidence = confidence,
                    isConfident = confidence >= 0.75
                )
            }
        }
        return best
    }

    private fun proximityScore(distance: Int): Double {
        return when {
            distance <= 1 -> 1.0
            distance <= 3 -> 0.9
            distance <= 6 -> 0.7
            distance <= 10 -> 0.5
            else -> 0.3
        }
    }

    private data class CurrencyToken(
        val code: String,
        val start: Int,
        val end: Int,
        val baseConfidence: Double
    )

    private data class CurrencyMatch(
        val code: String,
        val confidence: Double,
        val isConfident: Boolean
    )
}
