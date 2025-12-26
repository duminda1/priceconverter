package com.priceconverter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priceconverter.data.model.CurrencyPairRate
import com.priceconverter.data.repository.ExchangeRateRepository
import com.priceconverter.data.repository.SettingsRepository
import com.priceconverter.data.model.CurrencyRate
import com.priceconverter.domain.model.ConversionResult
import com.priceconverter.domain.model.Price
import com.priceconverter.domain.model.RateSource
import com.priceconverter.domain.usecase.ConvertCurrencyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConversionState {
    object Idle : ConversionState()
    object Loading : ConversionState()
    data class Success(val result: ConversionResult) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val repository = ExchangeRateRepository(settingsRepository)
    private val convertCurrencyUseCase = ConvertCurrencyUseCase()

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState

    private val _realtimeEnabled =
        MutableStateFlow(settingsRepository.isRealtimeEnabled())
    val realtimeEnabled: StateFlow<Boolean> = _realtimeEnabled

    private val _serviceReady =
        MutableStateFlow(settingsRepository.hasApiKey())
    val serviceReady: StateFlow<Boolean> = _serviceReady

    private val _liveScanEnabled =
        MutableStateFlow(settingsRepository.isLiveScanEnabled())
    val liveScanEnabled: StateFlow<Boolean> = _liveScanEnabled

    private val _manualCurrencies = MutableStateFlow<List<String>>(emptyList())
    val manualCurrencies: StateFlow<List<String>> = _manualCurrencies

    private val _manualRates = MutableStateFlow<List<CurrencyPairRate>>(emptyList())
    val manualRates: StateFlow<List<CurrencyPairRate>> = _manualRates

    private val _savedRates = MutableStateFlow<List<CurrencyPairRate>>(emptyList())
    val savedRates: StateFlow<List<CurrencyPairRate>> = _savedRates

    private val _usageWarning = MutableStateFlow<String?>(null)
    val usageWarning: StateFlow<String?> = _usageWarning

    init {
        refreshSettings()
    }

    fun setRealtimeEnabled(enabled: Boolean) {
        if (!_serviceReady.value) {
            _realtimeEnabled.value = false
            return
        }
        settingsRepository.setRealtimeEnabled(enabled)
        _realtimeEnabled.value = enabled
    }

    fun setLiveScanEnabled(enabled: Boolean) {
        settingsRepository.setLiveScanEnabled(enabled)
        _liveScanEnabled.value = enabled
    }

    fun refreshSettings() {
        val ready = settingsRepository.hasApiKey()
        _serviceReady.value = ready
        if (!ready) {
            settingsRepository.setRealtimeEnabled(false)
        }
        _realtimeEnabled.value = settingsRepository.isRealtimeEnabled()
        _liveScanEnabled.value = settingsRepository.isLiveScanEnabled()
        val manualRates = settingsRepository.getManualRates()
        val savedRates = settingsRepository.getSavedRates()
        _manualRates.value = manualRates
        _savedRates.value = savedRates
        _manualCurrencies.value = manualRates
            .flatMap { listOf(it.from, it.to) }
            .distinct()
            .sorted()
    }

    fun clearUsageWarning() {
        _usageWarning.value = null
    }

    fun convertPrice(amount: Double, fromCurrency: String, toCurrency: String) {
        val normalizedFrom = fromCurrency.trim().uppercase()
        val normalizedTo = toCurrency.trim().uppercase()
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) {
            _conversionState.value = ConversionState.Error("Enter valid currency codes.")
            return
        }

        val price = Price(amount, normalizedFrom)
        _conversionState.value = ConversionState.Loading

        viewModelScope.launch {
            try {
                val manualRate = settingsRepository.findManualRate(normalizedFrom, normalizedTo)
                if (manualRate != null) {
                    val result = convertCurrencyUseCase.execute(
                        price = price,
                        rate = CurrencyRate(
                            rate = manualRate.rate,
                            lastUpdatedMillis = manualRate.lastUpdatedMillis,
                            source = RateSource.MANUAL
                        ),
                        toCurrency = normalizedTo
                    )
                    _conversionState.value = ConversionState.Success(result)
                    return@launch
                }

                if (!_realtimeEnabled.value) {
                    val savedRate = settingsRepository.findSavedRate(normalizedFrom, normalizedTo)
                    if (savedRate != null) {
                        val result = convertCurrencyUseCase.execute(
                            price = price,
                            rate = CurrencyRate(
                                rate = savedRate.rate,
                                lastUpdatedMillis = savedRate.lastUpdatedMillis,
                                source = RateSource.SAVED
                            ),
                            toCurrency = normalizedTo
                        )
                        _conversionState.value = ConversionState.Success(result)
                        return@launch
                    }
                }

                val apiResult = repository.getExchangeRate(
                    fromCurrency = normalizedFrom,
                    toCurrency = normalizedTo,
                    amount = 1.0
                )
                apiResult.warningMessage?.let { _usageWarning.value = it }

                val rate = apiResult.rate
                if (rate != null) {
                    if (!_realtimeEnabled.value) {
                        settingsRepository.upsertSavedRate(
                            CurrencyPairRate(
                                from = normalizedFrom,
                                to = normalizedTo,
                                rate = rate.rate,
                                lastUpdatedMillis = rate.lastUpdatedMillis
                            )
                        )
                    }

                    val result = convertCurrencyUseCase.execute(
                        price = price,
                        rate = rate,
                        toCurrency = normalizedTo
                    )
                    _conversionState.value = ConversionState.Success(result)
                } else {
                    _conversionState.value =
                        ConversionState.Error(apiResult.errorMessage ?: "Failed to get conversion rate")
                }
            } catch (e: Exception) {
                _conversionState.value = ConversionState.Error(e.message ?: "Unknown error")
            }
        }
    }
}
