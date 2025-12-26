package com.example.priceconverter.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SettingsViewModel : ViewModel() {

    private val _selectedCurrencies = MutableStateFlow(listOf("USD", "AUD"))
    val selectedCurrencies: StateFlow<List<String>> = _selectedCurrencies

    fun addCurrency(currency: String) {
        if (!_selectedCurrencies.value.contains(currency)) {
            _selectedCurrencies.value = _selectedCurrencies.value + currency
        }
    }

    fun removeCurrency(currency: String) {
        _selectedCurrencies.value = _selectedCurrencies.value - currency
    }

    fun setCurrencies(currencies: List<String>) {
        _selectedCurrencies.value = currencies
    }
}

