package com.pocketcurrency.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.domain.usecase.GetRatesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RateViewModel @Inject constructor(
    application: Application,
    private val getRatesUseCase: GetRatesUseCase
) : AndroidViewModel(application) {

    private val _rate = MutableStateFlow(0.0)
    val rate: StateFlow<Double> = _rate

    fun loadRate(baseCurrency: String, targetCurrency: String) {
        viewModelScope.launch {
            try {
                val result = getRatesUseCase.execute(baseCurrency, targetCurrency)
                _rate.value = result.rate?.rate ?: 0.0
            } catch (e: Exception) {
                Log.e(TAG, "Failed to fetch exchange rates", e)
            }
        }
    }

    companion object {
        private const val TAG = "RateViewModel"
    }
}
