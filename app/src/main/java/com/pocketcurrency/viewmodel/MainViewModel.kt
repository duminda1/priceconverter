package com.pocketcurrency.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.model.ConversionResult
import com.pocketcurrency.domain.model.Price
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.domain.model.ServiceStatus
import com.pocketcurrency.domain.model.ServiceStatusType
import com.pocketcurrency.domain.usecase.ConvertCurrencyUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class ConversionState {
    object Idle : ConversionState()
    object Loading : ConversionState()
    data class Success(val result: ConversionResult) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

class MainViewModel(
    application: Application,
    private val rateRepository: RateRepository,
    private val settingsRepository: SettingsRepository
) : AndroidViewModel(application) {

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

    private val _homeCurrency = MutableStateFlow(settingsRepository.getHomeCurrency())
    val homeCurrency: StateFlow<String> = _homeCurrency

    private val _destinationCurrency = MutableStateFlow(settingsRepository.getDestinationCurrency())
    val destinationCurrency: StateFlow<String> = _destinationCurrency

    private val _currentRate = MutableStateFlow<Double?>(null)
    val currentRate: StateFlow<Double?> = _currentRate

    private val _currentRateSource = MutableStateFlow<RateSource?>(null)
    val currentRateSource: StateFlow<RateSource?> = _currentRateSource

    private val _currentRateUpdatedAt = MutableStateFlow<Long?>(null)
    val currentRateUpdatedAt: StateFlow<Long?> = _currentRateUpdatedAt

    private val _serviceStatus = MutableStateFlow<ServiceStatus?>(null)
    val serviceStatus: StateFlow<ServiceStatus?> = _serviceStatus

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
        val homeCurrency = settingsRepository.getHomeCurrency()
        val destinationCurrency = settingsRepository.getDestinationCurrency()
        _homeCurrency.value = homeCurrency
        _destinationCurrency.value = destinationCurrency
        val manualRates = rateRepository.getManualRates()
        val savedRates = rateRepository.getSavedRates()
        _manualRates.value = manualRates
        _savedRates.value = savedRates
        _manualCurrencies.value = (manualRates + savedRates)
            .flatMap { listOf(it.from, it.to) }
            .plus(listOf(homeCurrency, destinationCurrency))
            .filter { it.isNotBlank() }
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
        _currentRate.value = null
        _currentRateSource.value = null
        _currentRateUpdatedAt.value = null
        _serviceStatus.value = null

        viewModelScope.launch {
            try {
                val rateResult = rateRepository.getRate(
                    fromCurrency = normalizedFrom,
                    toCurrency = normalizedTo,
                    realtimeEnabled = _realtimeEnabled.value
                )
                rateResult.warningMessage?.let { _usageWarning.value = it }

                val rate = rateResult.rate
                if (rate != null) {
                    _currentRate.value = rate.rate
                    _currentRateSource.value = rate.source
                    _currentRateUpdatedAt.value = rate.lastUpdatedMillis
                    _serviceStatus.value = ServiceStatus(
                        type = when (rate.source) {
                            RateSource.LIVE -> ServiceStatusType.LIVE
                            RateSource.SAVED -> ServiceStatusType.SAVED
                            RateSource.MANUAL -> ServiceStatusType.MANUAL
                        },
                        lastUpdatedMillis = rate.lastUpdatedMillis
                    )
                    val result = convertCurrencyUseCase.execute(
                        price = price,
                        rate = rate,
                        toCurrency = normalizedTo
                    )
                    _conversionState.value = ConversionState.Success(result)
                } else {
                    _conversionState.value = ConversionState.Error(
                        rateResult.errorMessage ?: "Service unavailable. Please try again."
                    )
                }
            } catch (e: Exception) {
                _conversionState.value =
                    ConversionState.Error(e.message ?: "Service unavailable. Please try again.")
            }
        }
    }

    fun onScanResult(amount: Double, currencyCode: String?, isConfident: Boolean) {
        val now = SystemClock.elapsedRealtime()
        val amountChanged =
            lastScanAmount == null || kotlin.math.abs(lastScanAmount!! - amount) > 0.01
        val currencyChanged = lastScanCurrency != currencyCode || lastScanConfident != isConfident
        if (!amountChanged && !currencyChanged && now - lastScanAt < 900) {
            return
        }
        if (now - lastScanAt < 350) {
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
