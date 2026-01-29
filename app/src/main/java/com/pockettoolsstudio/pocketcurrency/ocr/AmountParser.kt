package com.pockettoolsstudio.pocketcurrency.ocr

import com.pockettoolsstudio.pocketcurrency.util.CurrencySymbols
import java.math.BigDecimal
import java.util.Currency
import java.util.Locale
import java.util.regex.Pattern

data class DetectedPrice(
    val amount: Double,
    val currencyCode: String?,
    val currencyConfidence: CurrencyConfidence,
    val currencySource: CurrencySource
)

data class AmountCandidate(
    val amount: Double,
    val currencyCode: String?,
    val currencyConfidence: Double,
    val currencyConfidenceLevel: CurrencyConfidence,
    val currencySource: CurrencySource,
    val hasDecimalSeparator: Boolean,
    val decimalDigits: Int,
    val integerDigits: Int
)

class AmountParser {

    private val numberPattern = Pattern.compile(
        "\\d(?:[\\d., ]*\\d)?"
    )
    private val codePattern = Pattern.compile("(?<![A-Z])[A-Z]{3}(?![A-Z])")
    private val safeSymbolToCode = CurrencySymbols.safeSymbolToCode
    private val fallbackSymbolToCode: Map<String, String>
        get() = CurrencySymbols.fallbackSymbolToCode
    private val symbolPattern = run {
        val symbols = (safeSymbolToCode.keys + fallbackSymbolToCode.keys)
            .sortedWith(compareByDescending<String> { it.length }.thenBy { it })
            .joinToString("|") { Pattern.quote(it) }
        Pattern.compile(symbols)
    }
    private val currencyCodes = Currency.getAvailableCurrencies()
        .map { it.currencyCode }
        .toSet()

    /**
     * Extracts the most likely price and optional currency information from OCR text.
     */
    fun parse(text: String, context: CurrencyContext = CurrencyContext()): DetectedPrice? {
        val candidates = extractCandidates(text, context)
        val best = candidates.maxWithOrNull(
            compareBy<AmountCandidate> { it.currencyConfidence }
                .thenBy { -it.currencyConfidenceLevel.ordinal }
        ) ?: return null
        return DetectedPrice(
            amount = best.amount,
            currencyCode = best.currencyCode,
            currencyConfidence = best.currencyConfidenceLevel,
            currencySource = best.currencySource
        )
    }

    fun extractCandidates(
        text: String,
        context: CurrencyContext = CurrencyContext()
    ): List<AmountCandidate> {
        val normalized = normalizeText(text)
        val currencyTokens = findCurrencyTokens(normalized)
        val matcher = numberPattern.matcher(normalized)
        val candidates = mutableListOf<AmountCandidate>()

        while (matcher.find()) {
            val match = matcher.group()
            val normalizedNumber = normalizeNumberToken(match) ?: continue
            val amount = parseAmountFromNormalized(normalizedNumber) ?: continue
            val matchStart = matcher.start()
            val matchEnd = matcher.end()
            val currencyMatch = resolveCurrencyMatch(
                currencyTokens,
                matchStart,
                matchEnd,
                context
            )
            candidates.add(
                AmountCandidate(
                    amount = amount,
                    currencyCode = currencyMatch.code,
                    currencyConfidence = currencyMatch.numericConfidence,
                    currencyConfidenceLevel = currencyMatch.confidenceLevel,
                    currencySource = currencyMatch.source,
                    hasDecimalSeparator = normalizedNumber.hasDecimalSeparator,
                    decimalDigits = normalizedNumber.decimalDigits,
                    integerDigits = normalizedNumber.integerDigits
                )
            )
        }

        return candidates
    }

    private fun normalizeText(text: String): String {
        return text
            .replace('\u00A0', ' ')
            .replace('\u2009', ' ')
            .replace('\u202F', ' ')
            .uppercase(Locale.ROOT)
    }

    private fun parseAmount(token: String): Double? {
        val normalized = normalizeNumberToken(token) ?: return null
        return parseAmountFromNormalized(normalized)
    }

    private fun parseAmountFromNormalized(normalized: NormalizedNumber): Double? {
        return try {
            BigDecimal(normalized.normalized).toDouble()
        } catch (e: NumberFormatException) {
            null
        }
    }

    private fun normalizeNumberToken(token: String): NormalizedNumber? {
        val decimalIndex = findDecimalSeparatorIndex(token)
        if (!isValidGrouping(token, decimalIndex)) {
            return null
        }
        val sb = StringBuilder(token.length)
        var decimalDigits = 0
        var integerDigits = 0

        for (i in token.indices) {
            val ch = token[i]
            when {
                ch.isDigit() -> {
                    sb.append(ch)
                    if (decimalIndex >= 0 && i > decimalIndex) {
                        decimalDigits++
                    } else {
                        integerDigits++
                    }
                }
                i == decimalIndex && (ch == '.' || ch == ',') -> {
                    // Rightmost dot/comma is the decimal separator.
                    sb.append('.')
                }
                isGroupingSeparator(ch) -> {
                    // Skip grouping separators (comma/dot/space variants).
                }
            }
        }

        if (sb.isEmpty()) {
            return null
        }

        val hasDecimalSeparator = decimalIndex >= 0 && decimalDigits > 0
        return NormalizedNumber(
            normalized = sb.toString(),
            hasDecimalSeparator = hasDecimalSeparator,
            decimalDigits = if (hasDecimalSeparator) decimalDigits else 0,
            integerDigits = integerDigits
        )
    }

    private fun findDecimalSeparatorIndex(token: String): Int {
        val lastDot = token.lastIndexOf('.')
        val lastComma = token.lastIndexOf(',')
        if (lastDot < 0 && lastComma < 0) return -1
        if (lastDot >= 0 && lastComma >= 0) return maxOf(lastDot, lastComma)

        val lastIndex = if (lastDot >= 0) lastDot else lastComma
        val digitsAfter = countDigitsAfter(token, lastIndex)
        if (digitsAfter <= 0) return -1

        val sepChar = if (lastDot >= 0) '.' else ','
        if (digitsAfter == 3 && isGroupedThousandsOnly(token, sepChar)) {
            // Only grouping separators found; treat as integer.
            return -1
        }
        return lastIndex
    }

    private fun countDigitsAfter(token: String, index: Int): Int {
        var count = 0
        for (i in index + 1 until token.length) {
            val ch = token[i]
            if (ch.isDigit()) {
                count++
            }
        }
        return count
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

    private fun isValidGrouping(token: String, decimalIndex: Int): Boolean {
        if (decimalIndex < 0) {
            return true
        }
        val decimalChar = token[decimalIndex]
        var digitsSinceSeparator = 0
        for (i in token.length - 1 downTo 0) {
            val ch = token[i]
            if (i == decimalIndex) {
                if (digitsSinceSeparator == 0) {
                    return false
                }
                digitsSinceSeparator = 0
                continue
            }
            when {
                ch.isDigit() -> digitsSinceSeparator++
                isGroupingSpace(ch) -> {
                    // Allow grouping spaces.
                }
                ch == '.' || ch == ',' -> {
                    if (digitsSinceSeparator != 3) {
                        return false
                    }
                    digitsSinceSeparator = 0
                }
                else -> return false
            }
        }
        return true
    }

    private fun findCurrencyTokens(text: String): List<CurrencyToken> {
        val tokens = mutableListOf<CurrencyToken>()
        val symbolMatcher = symbolPattern.matcher(text)
        while (symbolMatcher.find()) {
            val symbol = symbolMatcher.group()
            val normalizedSymbol = symbol.uppercase(Locale.ROOT)
            if (normalizedSymbol == "$") {
                tokens.add(
                    CurrencyToken(
                        code = null,
                        start = symbolMatcher.start(),
                        end = symbolMatcher.end(),
                        baseConfidence = AMBIGUOUS_SYMBOL_CONFIDENCE,
                        type = CurrencyTokenType.AMBIGUOUS_DOLLAR
                    )
                )
                continue
            }
            val safeCode = safeSymbolToCode[normalizedSymbol]
            val fallbackCode = fallbackSymbolToCode[normalizedSymbol]
            val code = safeCode ?: fallbackCode ?: continue
            val baseConfidence = when {
                safeCode != null -> 0.85
                else -> 0.6
            }
            tokens.add(
                CurrencyToken(
                    code = code,
                    start = symbolMatcher.start(),
                    end = symbolMatcher.end(),
                    baseConfidence = baseConfidence,
                    type = CurrencyTokenType.EXPLICIT_SYMBOL
                )
            )
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
                        baseConfidence = 1.0,
                        type = CurrencyTokenType.ISO_CODE
                    )
                )
            }
        }
        return tokens
    }

    private fun resolveCurrencyMatch(
        tokens: List<CurrencyToken>,
        numberStart: Int,
        numberEnd: Int,
        context: CurrencyContext
    ): CurrencyMatch {
        val explicitCode = bestTokenMatch(
            tokens,
            numberStart,
            numberEnd,
            CurrencyTokenType.ISO_CODE
        )
        if (explicitCode != null) {
            return CurrencyMatch(
                code = explicitCode.token.code,
                confidenceLevel = CurrencyConfidence.HIGH,
                source = CurrencySource.OCR_EXPLICIT,
                numericConfidence = explicitCode.confidence
            )
        }

        val explicitSymbol = bestTokenMatch(
            tokens,
            numberStart,
            numberEnd,
            CurrencyTokenType.EXPLICIT_SYMBOL
        )
        if (explicitSymbol != null) {
            return CurrencyMatch(
                code = explicitSymbol.token.code,
                confidenceLevel = CurrencyConfidence.HIGH,
                source = CurrencySource.OCR_SYMBOL,
                numericConfidence = explicitSymbol.confidence
            )
        }

        val ambiguousDollar = bestTokenMatch(
            tokens,
            numberStart,
            numberEnd,
            CurrencyTokenType.AMBIGUOUS_DOLLAR
        )
        if (ambiguousDollar != null) {
            val resolved = resolveDollarCurrency(context)
            return CurrencyMatch(
                code = resolved.code,
                confidenceLevel = resolved.confidenceLevel,
                source = resolved.source,
                numericConfidence = ambiguousDollar.confidence
            )
        }

        val fallback = resolveInferredCurrency(context)
        return CurrencyMatch(
            code = fallback.code,
            confidenceLevel = fallback.confidenceLevel,
            source = fallback.source,
            numericConfidence = INFERRED_CURRENCY_CONFIDENCE
        )
    }

    private fun bestTokenMatch(
        tokens: List<CurrencyToken>,
        numberStart: Int,
        numberEnd: Int,
        type: CurrencyTokenType
    ): TokenMatch? {
        var best: TokenMatch? = null
        tokens.filter { it.type == type }.forEach { token ->
            val distance = when {
                token.end <= numberStart -> numberStart - token.end
                token.start >= numberEnd -> token.start - numberEnd
                else -> 0
            }
            val proximity = proximityScore(distance)
            val confidence = token.baseConfidence * proximity
            if (best == null || confidence > best!!.confidence) {
                best = TokenMatch(token, confidence)
            }
        }
        return best
    }

    private fun resolveDollarCurrency(context: CurrencyContext): CurrencyResolution {
        val selectedCurrency = normalizeCurrencyCode(context.selectedFromCurrency)
        if (selectedCurrency != null && DOLLAR_CURRENCIES.contains(selectedCurrency)) {
            return CurrencyResolution(
                code = selectedCurrency,
                confidenceLevel = CurrencyConfidence.MEDIUM,
                source = CurrencySource.USER_SELECTION
            )
        }
        val localeCurrency = resolveDollarCurrencyForLocale(context.locale)
        val code = localeCurrency ?: "USD"
        return CurrencyResolution(
            code = code,
            confidenceLevel = CurrencyConfidence.MEDIUM,
            source = CurrencySource.LOCALE_DEFAULT
        )
    }

    private fun resolveInferredCurrency(context: CurrencyContext): CurrencyResolution {
        val selectedCurrency = normalizeCurrencyCode(context.selectedFromCurrency)
        if (selectedCurrency != null) {
            return CurrencyResolution(
                code = selectedCurrency,
                confidenceLevel = CurrencyConfidence.LOW,
                source = CurrencySource.USER_SELECTION
            )
        }
        val localeCurrency = resolveLocaleCurrency(context.locale)
        return CurrencyResolution(
            code = localeCurrency ?: "USD",
            confidenceLevel = CurrencyConfidence.LOW,
            source = CurrencySource.LOCALE_DEFAULT
        )
    }

    private fun resolveDollarCurrencyForLocale(locale: Locale): String? {
        val region = locale.country.uppercase(Locale.ROOT)
        return DOLLAR_LOCALE_BIAS[region]
    }

    private fun resolveLocaleCurrency(locale: Locale): String? {
        val region = locale.country.uppercase(Locale.ROOT)
        if (EURO_REGIONS.contains(region)) {
            return "EUR"
        }
        val mapped = LOCALE_CURRENCY_BIAS[region]
        if (mapped != null) {
            return mapped
        }
        return try {
            Currency.getInstance(locale).currencyCode
        } catch (e: Exception) {
            null
        }
    }

    private fun normalizeCurrencyCode(code: String?): String? {
        return code
            ?.trim()
            ?.uppercase(Locale.ROOT)
            ?.takeIf { it.isNotBlank() }
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

    private enum class CurrencyTokenType {
        ISO_CODE,
        EXPLICIT_SYMBOL,
        AMBIGUOUS_DOLLAR
    }

    private data class CurrencyToken(
        val code: String?,
        val start: Int,
        val end: Int,
        val baseConfidence: Double,
        val type: CurrencyTokenType
    )

    private data class TokenMatch(
        val token: CurrencyToken,
        val confidence: Double
    )

    private data class CurrencyMatch(
        val code: String?,
        val confidenceLevel: CurrencyConfidence,
        val source: CurrencySource,
        val numericConfidence: Double
    )

    private data class CurrencyResolution(
        val code: String?,
        val confidenceLevel: CurrencyConfidence,
        val source: CurrencySource
    )

    private data class NormalizedNumber(
        val normalized: String,
        val hasDecimalSeparator: Boolean,
        val decimalDigits: Int,
        val integerDigits: Int
    )

    private companion object {
        private const val AMBIGUOUS_SYMBOL_CONFIDENCE = 0.6
        private const val INFERRED_CURRENCY_CONFIDENCE = 0.2

        private val DOLLAR_CURRENCIES = setOf("AUD", "SGD", "NZD", "USD", "HKD", "CAD")
        private val DOLLAR_LOCALE_BIAS = mapOf(
            "AU" to "AUD",
            "SG" to "SGD",
            "NZ" to "NZD",
            "US" to "USD",
            "CA" to "CAD"
        )
        private val LOCALE_CURRENCY_BIAS = mapOf(
            "AU" to "AUD",
            "SG" to "SGD",
            "NZ" to "NZD",
            "JP" to "JPY",
            "US" to "USD",
            "CA" to "CAD"
        )
        private val EURO_REGIONS = setOf(
            "AT",
            "BE",
            "CY",
            "EE",
            "FI",
            "FR",
            "DE",
            "GR",
            "IE",
            "IT",
            "LV",
            "LT",
            "LU",
            "MT",
            "NL",
            "PT",
            "SK",
            "SI",
            "ES"
        )
    }
}
