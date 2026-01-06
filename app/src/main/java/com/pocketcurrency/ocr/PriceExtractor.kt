package com.pocketcurrency.ocr

class PriceExtractor(
    private val amountParser: AmountParser = AmountParser()
) {

    /**
     * Extracts the most likely price and optional currency information from OCR text.
     */
    fun extract(
        text: String,
        context: CurrencyContext = CurrencyContext()
    ): DetectedPrice? {
        val candidates = extractCandidates(text, context)
        val best = candidates.maxWithOrNull(
            compareBy<PriceCandidate> { it.baseScore + (it.currencyConfidence * 0.1) }
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
    ): List<PriceCandidate> {
        return amountParser.extractCandidates(text, context).map { candidate ->
            PriceCandidate(
                amount = candidate.amount,
                currencyCode = candidate.currencyCode,
                currencyConfidence = candidate.currencyConfidence,
                currencyConfidenceLevel = candidate.currencyConfidenceLevel,
                currencySource = candidate.currencySource,
                baseScore = scoreCandidate(candidate)
            )
        }
    }

    private fun scoreCandidate(candidate: AmountCandidate): Double {
        var score = 0.2
        if (candidate.hasDecimalSeparator) {
            score += 0.2
        }
        if (candidate.decimalDigits == 2) {
            score += 0.3
        }
        if (candidate.integerDigits >= 2) {
            score += if (candidate.hasDecimalSeparator) 0.2 else 0.35
        }
        return score.coerceIn(0.0, 1.0)
    }
}

data class PriceCandidate(
    val amount: Double,
    val currencyCode: String?,
    val currencyConfidence: Double,
    val currencyConfidenceLevel: CurrencyConfidence,
    val currencySource: CurrencySource,
    val baseScore: Double
)
