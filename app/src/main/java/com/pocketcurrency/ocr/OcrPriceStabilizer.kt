package com.pocketcurrency.ocr

import java.util.ArrayDeque
import kotlin.math.abs

internal class OcrPriceStabilizer(
    private val priceExtractor: PriceExtractor = PriceExtractor(),
    private val minStableFrames: Int = DEFAULT_MIN_STABLE_FRAMES,
    private val confidenceThreshold: Double = DEFAULT_CONFIDENCE_THRESHOLD,
    private val historySize: Int = DEFAULT_HISTORY_SIZE,
    private val minHoldFrames: Int = DEFAULT_MIN_HOLD_FRAMES,
    private val maxScoreDrop: Double = DEFAULT_MAX_SCORE_DROP
) {
    private val history = ArrayDeque<List<PriceCandidate>>(historySize)
    private var lastEmitted: EmittedPrice? = null
    private var framesSinceLastEmit = 0

    fun reset() {
        history.clear()
        lastEmitted = null
        framesSinceLastEmit = 0
    }

    fun onFrame(text: String, context: CurrencyContext): DetectedPrice? {
        framesSinceLastEmit++
        val candidates = priceExtractor.extractCandidates(text, context)
        val scoredCandidates = candidates.map { candidate ->
            val stableFrames = countConsecutiveMatches(candidate)
            val score = scoreCandidate(candidate, stableFrames)
            ScoredCandidate(candidate, score, stableFrames)
        }
        val lastAmountSeen = lastEmitted?.let { emitted ->
            candidates.any { amountsMatch(it.amount, emitted.amount) }
        } ?: true

        val best = scoredCandidates
            .filter { scored ->
                scored.score >= confidenceThreshold &&
                    (scored.stableFrames >= minStableFrames || canFastTrack(scored, lastAmountSeen))
            }
            .maxWithOrNull(
                compareBy<ScoredCandidate> { it.score }
                    .thenBy { -it.candidate.currencyConfidenceLevel.ordinal }
                    .thenBy { it.candidate.currencyConfidence }
            )

        addToHistory(candidates)

        if (best == null) {
            return null
        }

        val resolvedCurrency = resolveCurrency(best.candidate)
        if (!shouldEmit(best, resolvedCurrency, lastAmountSeen)) {
            return null
        }

        framesSinceLastEmit = 0
        lastEmitted = EmittedPrice(
            amount = best.candidate.amount,
            score = best.score,
            currencyCode = resolvedCurrency.code,
            currencyConfidence = resolvedCurrency.confidence,
            currencySource = resolvedCurrency.source
        )

        return DetectedPrice(
            amount = best.candidate.amount,
            currencyCode = resolvedCurrency.code,
            currencyConfidence = resolvedCurrency.confidence,
            currencySource = resolvedCurrency.source
        )
    }

    private fun countConsecutiveMatches(candidate: PriceCandidate): Int {
        var count = 1
        val iterator = history.descendingIterator()
        while (iterator.hasNext()) {
            val frame = iterator.next()
            if (frame.any { amountsMatch(it.amount, candidate.amount) }) {
                count++
            } else {
                break
            }
        }
        return count
    }

    private fun addToHistory(candidates: List<PriceCandidate>) {
        history.addLast(candidates)
        while (history.size > historySize) {
            history.removeFirst()
        }
    }

    private fun scoreCandidate(candidate: PriceCandidate, stableFrames: Int): Double {
        val consistencyBoost = consistencyBoost(stableFrames)
        val currencyBoost = when {
            candidate.currencyConfidenceLevel == CurrencyConfidence.HIGH -> CURRENCY_CONFIDENT_BONUS
            candidate.currencyCode != null -> CURRENCY_PRESENT_BONUS
            else -> 0.0
        }
        val currencyConfidenceBoost = candidate.currencyConfidence * CURRENCY_CONFIDENCE_WEIGHT
        return (candidate.baseScore + consistencyBoost + currencyBoost + currencyConfidenceBoost)
            .coerceIn(0.0, 1.0)
    }

    private fun canFastTrack(candidate: ScoredCandidate, lastAmountSeen: Boolean): Boolean {
        if (lastEmitted == null || lastAmountSeen) {
            return false
        }
        return candidate.candidate.currencyConfidence >= FAST_TRACK_CURRENCY_CONFIDENCE &&
            candidate.candidate.baseScore >= FAST_TRACK_BASE_SCORE
    }

    private fun consistencyBoost(stableFrames: Int): Double {
        if (stableFrames <= 1) {
            return 0.0
        }
        return (CONSISTENCY_BONUS_PER_FRAME * (stableFrames - 1))
            .coerceAtMost(MAX_CONSISTENCY_BONUS)
    }

    private fun resolveCurrency(candidate: PriceCandidate): ResolvedCurrency {
        if (!candidate.currencyCode.isNullOrBlank()) {
            return ResolvedCurrency(
                code = candidate.currencyCode,
                confidence = candidate.currencyConfidenceLevel,
                source = candidate.currencySource
            )
        }
        val last = lastEmitted
        return ResolvedCurrency(
            code = last?.currencyCode,
            confidence = last?.currencyConfidence ?: CurrencyConfidence.LOW,
            source = last?.currencySource ?: CurrencySource.LOCALE_DEFAULT
        )
    }

    private fun shouldEmit(
        candidate: ScoredCandidate,
        resolvedCurrency: ResolvedCurrency,
        lastAmountSeen: Boolean
    ): Boolean {
        val last = lastEmitted ?: return true
        val sameAmount = amountsMatch(last.amount, candidate.candidate.amount)
        val sameCurrency =
            last.currencyCode == resolvedCurrency.code &&
                last.currencyConfidence == resolvedCurrency.confidence &&
                last.currencySource == resolvedCurrency.source
        if (sameAmount && sameCurrency) {
            return false
        }
        if (sameAmount) {
            return true
        }
        val scoreDrop = last.score - candidate.score
        if (!lastAmountSeen) {
            if (scoreDrop > 0.0 &&
                candidate.stableFrames < minStableFrames + 1 &&
                framesSinceLastEmit < minHoldFrames
            ) {
                return false
            }
            return true
        }

        return scoreDrop <= maxScoreDrop ||
            candidate.stableFrames >= minStableFrames + 1 ||
            framesSinceLastEmit >= minHoldFrames
    }

    private fun amountsMatch(first: Double, second: Double): Boolean {
        return abs(first - second) <= AMOUNT_MATCH_TOLERANCE
    }

    private data class ScoredCandidate(
        val candidate: PriceCandidate,
        val score: Double,
        val stableFrames: Int
    )

    private data class ResolvedCurrency(
        val code: String?,
        val confidence: CurrencyConfidence,
        val source: CurrencySource
    )

    private data class EmittedPrice(
        val amount: Double,
        val score: Double,
        val currencyCode: String?,
        val currencyConfidence: CurrencyConfidence,
        val currencySource: CurrencySource
    )

    private companion object {
        private const val DEFAULT_MIN_STABLE_FRAMES = 2
        private const val DEFAULT_CONFIDENCE_THRESHOLD = 0.65
        private const val DEFAULT_HISTORY_SIZE = 4
        private const val DEFAULT_MIN_HOLD_FRAMES = 3
        private const val DEFAULT_MAX_SCORE_DROP = 0.12
        private const val CONSISTENCY_BONUS_PER_FRAME = 0.15
        private const val MAX_CONSISTENCY_BONUS = 0.3
        private const val CURRENCY_CONFIDENT_BONUS = 0.05
        private const val CURRENCY_PRESENT_BONUS = 0.02
        private const val CURRENCY_CONFIDENCE_WEIGHT = 0.2
        private const val FAST_TRACK_CURRENCY_CONFIDENCE = 0.75
        private const val FAST_TRACK_BASE_SCORE = 0.55
        private const val AMOUNT_MATCH_TOLERANCE = 0.01
    }
}
