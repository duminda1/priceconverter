package com.pocketcurrency.domain.usecase

import com.pocketcurrency.data.repository.ScanRepository
import com.pocketcurrency.ocr.CurrencyConfidence
import com.pocketcurrency.ocr.CurrencySource
import com.pocketcurrency.util.TimeProvider
import dagger.hilt.android.scopes.ActivityRetainedScoped
import javax.inject.Inject
import kotlin.math.abs

@ActivityRetainedScoped
class ProcessScanResultUseCase @Inject constructor(
    private val scanRepository: ScanRepository,
    private val timeProvider: TimeProvider
) {

    private companion object {
        private const val SCAN_DEBOUNCE_DELAY_MS = 900L
        private const val SCAN_MIN_INTERVAL_MS = 350L
    }

    private var lastScanAt = 0L
    private var lastScanAmount: Double? = null
    private var lastScanCurrency: String? = null
    private var lastScanCurrencyConfidence: CurrencyConfidence? = null
    private var lastScanCurrencySource: CurrencySource? = null

    fun execute(
        amount: Double,
        currencyCode: String?,
        currencyConfidence: CurrencyConfidence,
        currencySource: CurrencySource
    ) {
        val now = timeProvider.nowMillis()
        val amountChanged = lastScanAmount == null || abs(lastScanAmount!! - amount) > 0.01
        val currencyChanged = lastScanCurrency != currencyCode ||
            lastScanCurrencyConfidence != currencyConfidence ||
            lastScanCurrencySource != currencySource
        if (!amountChanged && !currencyChanged && now - lastScanAt < SCAN_DEBOUNCE_DELAY_MS) {
            return
        }
        if (now - lastScanAt < SCAN_MIN_INTERVAL_MS) {
            return
        }

        lastScanAt = now
        lastScanAmount = amount
        lastScanCurrency = currencyCode
        lastScanCurrencyConfidence = currencyConfidence
        lastScanCurrencySource = currencySource

        scanRepository.updateScan(amount, currencyCode, currencyConfidence, currencySource)
    }
}
