package com.pocketcurrency.data.repository

import android.os.SystemClock
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.math.abs

@ActivityRetainedScoped
class ScanRepository @Inject constructor() {

    private companion object {
        private const val SCAN_DEBOUNCE_DELAY_MS = 900L
        private const val SCAN_MIN_INTERVAL_MS = 350L
    }

    private val _scanAmount = MutableStateFlow<Double?>(null)
    val scanAmount: StateFlow<Double?> = _scanAmount

    private val _scanCurrency = MutableStateFlow<String?>(null)
    val scanCurrency: StateFlow<String?> = _scanCurrency

    private val _scanCurrencyConfident = MutableStateFlow(false)
    val scanCurrencyConfident: StateFlow<Boolean> = _scanCurrencyConfident

    private var lastScanAt = 0L
    private var lastScanAmount: Double? = null
    private var lastScanCurrency: String? = null
    private var lastScanConfident = false

    fun onScanResult(amount: Double, currencyCode: String?, isConfident: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val amountChanged = lastScanAmount == null || abs(lastScanAmount!! - amount) > 0.01
        val currencyChanged = lastScanCurrency != currencyCode || lastScanConfident != isConfident
        if (!amountChanged && !currencyChanged && now - lastScanAt < SCAN_DEBOUNCE_DELAY_MS) {
            return
        }
        if (now - lastScanAt < SCAN_MIN_INTERVAL_MS) {
            return
        }

        lastScanAt = now
        lastScanAmount = amount
        lastScanCurrency = currencyCode
        lastScanConfident = isConfident

        _scanAmount.value = amount
        _scanCurrency.value = currencyCode
        _scanCurrencyConfident.value = isConfident
    }
}
