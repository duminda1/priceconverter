package com.example.priceconverter.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.priceconverter.data.repository.ExchangeRateRepository
import com.example.priceconverter.domain.usecase.GetRatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RateViewModel : ViewModel() {

    private val repository = ExchangeRateRepository()
    private val getRatesUseCase = GetRatesUseCase(repository)

    private val _rate = MutableStateFlow(0.0)
    val rate: StateFlow<Double> = _rate

    fun loadRate(baseCurrency: String, targetCurrency: String) {
        viewModelScope.launch {
            try {
                val currencyRate = getRatesUseCase.execute(baseCurrency, targetCurrency)
                _rate.value = currencyRate?.rate ?: 0.0
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
