package com.example.priceconverter.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.priceconverter.data.repository.ExchangeRateRepository
import com.example.priceconverter.domain.model.ConversionResult
import com.example.priceconverter.domain.model.Price
import com.example.priceconverter.domain.usecase.ConvertCurrencyUseCase
import com.example.priceconverter.domain.usecase.GetRatesUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime

sealed class ConversionState {
    object Idle : ConversionState()
    object Loading : ConversionState()
    data class Success(val result: ConversionResult) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

class MainViewModel : ViewModel() {

    private val repository = ExchangeRateRepository()
    private val getRatesUseCase = GetRatesUseCase(repository)
    private val convertCurrencyUseCase = ConvertCurrencyUseCase()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState

    fun convertPrice(amount: Double, fromCurrency: String, toCurrency: String) {
        val price = Price(amount, fromCurrency)
        _conversionState.value = ConversionState.Loading

        viewModelScope.launch {
            try {
                // 1️⃣ Fetch live CurrencyRate from API
                val currencyRate = getRatesUseCase.execute(fromCurrency, toCurrency, amount = 1.0)

                if (currencyRate != null) {
                    // 2️⃣ Convert amount using ConvertCurrencyUseCase
                    val result: ConversionResult = convertCurrencyUseCase.execute(
                        price = price,
                        rate = currencyRate,
                        toCurrency = toCurrency,
                        lastUpdated = LocalDateTime.now().toString()
                    )

                    _conversionState.value = ConversionState.Success(result)
                } else {
                    _conversionState.value = ConversionState.Error("Failed to get conversion rate")
                }

            } catch (e: Exception) {
                _conversionState.value = ConversionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
