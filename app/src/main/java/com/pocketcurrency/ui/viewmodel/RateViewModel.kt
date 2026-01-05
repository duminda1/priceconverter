package com.pocketcurrency.ui.viewmodel

import android.util.Log
import com.pocketcurrency.BuildConfig
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.domain.usecase.GetRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateViewModel @Inject constructor(
    private val getRatesUseCase: GetRatesUseCase
) : ViewModel() {

    private val _rate = MutableStateFlow(0.0)
    val rate: StateFlow<Double> = _rate

    fun loadRate(baseCurrency: String, targetCurrency: String) {
        viewModelScope.launch {
            try {
                val result = getRatesUseCase.execute(baseCurrency, targetCurrency)
                _rate.value = result.rate?.rate ?: 0.0
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to fetch exchange rates", e)
                }
            }
        }
    }

    companion object {
        private const val TAG = "RateViewModel"
    }
}
