package com.pocketcurrency.viewmodel

import androidx.lifecycle.ViewModel
import com.pocketcurrency.data.repository.ScanRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository
) : ViewModel() {

    val scanAmount: StateFlow<Double?> = scanRepository.scanAmount
    val scanCurrency: StateFlow<String?> = scanRepository.scanCurrency
    val scanCurrencyConfident: StateFlow<Boolean> = scanRepository.scanCurrencyConfident

    fun onScanResult(amount: Double, currencyCode: String?, isConfident: Boolean) {
        scanRepository.onScanResult(amount, currencyCode, isConfident)
    }
}
