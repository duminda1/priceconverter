package com.pocketcurrency.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.R
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.domain.model.ConversionResult
import com.pocketcurrency.domain.model.Price
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.domain.model.ServiceStatus
import com.pocketcurrency.domain.model.ServiceStatusType
import com.pocketcurrency.domain.usecase.ConvertCurrencyUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ConversionState {
    object Idle : ConversionState()
    object Loading : ConversionState()
    data class Success(val result: ConversionResult) : ConversionState()
    data class Error(val message: String) : ConversionState()
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val rateRepository: RateRepository,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val strings: StringProvider
) : ViewModel() {

    private val _conversionState = MutableStateFlow<ConversionState>(ConversionState.Idle)
    val conversionState: StateFlow<ConversionState> = _conversionState

    private val _currentRate = MutableStateFlow<Double?>(null)
    val currentRate: StateFlow<Double?> = _currentRate

    private val _currentRateSource = MutableStateFlow<RateSource?>(null)
    val currentRateSource: StateFlow<RateSource?> = _currentRateSource

    private val _currentRateUpdatedAt = MutableStateFlow<Long?>(null)
    val currentRateUpdatedAt: StateFlow<Long?> = _currentRateUpdatedAt

    private val _serviceStatus = MutableStateFlow<ServiceStatus?>(null)
    val serviceStatus: StateFlow<ServiceStatus?> = _serviceStatus

    private val _usageWarning = MutableStateFlow<String?>(null)
    val usageWarning: StateFlow<String?> = _usageWarning

    fun clearUsageWarning() {
        _usageWarning.value = null
    }

    fun convertPrice(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        realtimeEnabled: Boolean
    ) {
        val normalizedFrom = fromCurrency.trim().uppercase()
        val normalizedTo = toCurrency.trim().uppercase()
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) {
            _conversionState.value =
                ConversionState.Error(strings.get(R.string.error_invalid_currency_codes))
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
                    realtimeEnabled = realtimeEnabled
                )
                rateResult.warningMessage?.let { _usageWarning.value = it }

                val rate = rateResult.rate
                if (rate != null) {
                    _currentRate.value = rate.rate
                    _currentRateSource.value = rate.source
                    _currentRateUpdatedAt.value = rate.lastUpdatedAtMillis
                    _serviceStatus.value = ServiceStatus(
                        type = when (rate.source) {
                            RateSource.LIVE -> ServiceStatusType.LIVE
                            RateSource.SAVED -> ServiceStatusType.SAVED
                            RateSource.MANUAL -> ServiceStatusType.MANUAL
                        },
                        lastUpdatedMillis = rate.lastUpdatedAtMillis,
                        isStale = rateResult.isStale
                    )
                    val result = convertCurrencyUseCase.execute(
                        price = price,
                        rate = rate,
                        toCurrency = normalizedTo
                    )
                    _conversionState.value = ConversionState.Success(result)
                } else {
                    _conversionState.value = ConversionState.Error(
                        rateResult.errorMessage
                            ?: strings.get(R.string.error_service_unavailable)
                    )
                }
            } catch (e: Exception) {
                _conversionState.value =
                    ConversionState.Error(
                        e.message ?: strings.get(R.string.error_service_unavailable)
                    )
            }
        }
    }
}
