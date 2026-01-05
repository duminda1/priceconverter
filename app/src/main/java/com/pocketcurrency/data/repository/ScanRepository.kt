package com.pocketcurrency.data.repository

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

    private val _scanCurrencyConfident = MutableStateFlow(false)
    val scanCurrencyConfident: StateFlow<Boolean> = _scanCurrencyConfident

    fun updateScan(amount: Double, currencyCode: String?, isConfident: Boolean) {
        _scanAmount.value = amount
        _scanCurrency.value = currencyCode
        _scanCurrencyConfident.value = isConfident
    }
}
