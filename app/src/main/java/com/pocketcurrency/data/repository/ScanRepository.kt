package com.pocketcurrency.data.repository

import com.pocketcurrency.ocr.CurrencyConfidence
import com.pocketcurrency.ocr.CurrencySource
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@ActivityRetainedScoped
class ScanRepository @Inject constructor() {

    private val _scanAmount = MutableStateFlow<Double?>(null)
    val scanAmount: StateFlow<Double?> = _scanAmount

    private val _scanCurrency = MutableStateFlow<String?>(null)
    val scanCurrency: StateFlow<String?> = _scanCurrency

    private val _scanCurrencyConfidence = MutableStateFlow<CurrencyConfidence?>(null)
    val scanCurrencyConfidence: StateFlow<CurrencyConfidence?> = _scanCurrencyConfidence

    private val _scanCurrencySource = MutableStateFlow<CurrencySource?>(null)
    val scanCurrencySource: StateFlow<CurrencySource?> = _scanCurrencySource

    fun updateScan(
        amount: Double,
        currencyCode: String?,
        currencyConfidence: CurrencyConfidence,
        currencySource: CurrencySource
    ) {
        _scanAmount.value = amount
        _scanCurrency.value = currencyCode
        _scanCurrencyConfidence.value = currencyConfidence
        _scanCurrencySource.value = currencySource
    }

    fun clearScan() {
        _scanAmount.value = null
        _scanCurrency.value = null
        _scanCurrencyConfidence.value = null
        _scanCurrencySource.value = null
    }
}
