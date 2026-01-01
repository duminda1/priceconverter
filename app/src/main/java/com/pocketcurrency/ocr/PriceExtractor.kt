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
        "\\d(?:[\\d., ]*\\d)?"
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
        val normalized = text
            .replace('\u00A0', ' ')
            .replace('\u2009', ' ')
            .replace('\u202F', ' ')
            .uppercase(Locale.ROOT)
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
        val decimalIndex = findDecimalSeparatorIndex(token)
        val sb = StringBuilder(token.length)

        for (i in token.indices) {
            val ch = token[i]
            when {
                ch.isDigit() -> sb.append(ch)
                i == decimalIndex && (ch == '.' || ch == ',') -> {
                    // Rightmost dot/comma is the decimal separator.
                    sb.append('.')
                }
                isGroupingSeparator(ch) -> {
                    // Skip grouping separators (comma/dot/space variants).
                }
            }
        }

        return if (sb.isNotEmpty()) sb.toString() else null
    }

    private fun findDecimalSeparatorIndex(token: String): Int {
        val lastDot = token.lastIndexOf('.')
        val lastComma = token.lastIndexOf(',')
        if (lastDot < 0 && lastComma < 0) return -1
        if (lastDot >= 0 && lastComma >= 0) return maxOf(lastDot, lastComma)

        val lastIndex = if (lastDot >= 0) lastDot else lastComma
        val digitsAfter = token.length - lastIndex - 1
        if (digitsAfter <= 0) return -1

        val sepChar = if (lastDot >= 0) '.' else ','
        if (digitsAfter == 3 && isGroupedThousandsOnly(token, sepChar)) {
            // Only grouping separators found; treat as integer.
            return -1
        }
        return lastIndex
    }

    private fun isGroupedThousandsOnly(token: String, separator: Char): Boolean {
        var digitsSinceSeparator = 0
        var sawSeparator = false
        for (i in token.length - 1 downTo 0) {
            val ch = token[i]
            when {
                ch.isDigit() -> digitsSinceSeparator++
                ch == separator -> {
                    sawSeparator = true
                    if (digitsSinceSeparator != 3) return false
                    digitsSinceSeparator = 0
                }
                isGroupingSpace(ch) -> {
                    // Ignore spacing inside grouped numbers.
                }
                else -> return false
            }
        }
        return sawSeparator && digitsSinceSeparator in 1..3
    }

    private fun isGroupingSeparator(ch: Char): Boolean {
        return ch == '.' || ch == ',' || isGroupingSpace(ch)
    }

    private fun isGroupingSpace(ch: Char): Boolean {
        return ch == ' ' || ch == '\u00A0' || ch == '\u2009' || ch == '\u202F'
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
