package com.pocketcurrency.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.utils.Constants
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainSettingsUiState(
    val realtimeEnabled: Boolean = false,
    val serviceReady: Boolean = false,
    val provider: String = "",
    val realtimeAvailable: Boolean = false,
    val liveScanEnabled: Boolean = true,
    val manualCurrencies: List<String> = emptyList(),
    val manualRates: List<CurrencyPairRate> = emptyList(),
    val savedRates: List<CurrencyPairRate> = emptyList(),
    val homeCurrency: String = "",
    val destinationCurrency: String = ""
)

@HiltViewModel
class MainSettingsViewModel @Inject constructor(
    private val rateRepository: RateRepository,
    private val settingsRepository: SettingsRepository,
    private val rateUpdateRepository: RateUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainSettingsUiState())
    val uiState: StateFlow<MainSettingsUiState> = _uiState

    init {
        refreshState()
    }

    fun refreshState() {
        val providerConfig = rateUpdateRepository.getActiveProviderConfig()
        val serviceReady = if (providerConfig.requiresApiKey) {
            settingsRepository.hasApiKey()
        } else {
            true
        }
        if (!serviceReady || !providerConfig.supportsRealtime) {
            settingsRepository.setRealtimeEnabled(false)
        }
        val realtimeEnabled =
            settingsRepository.isRealtimeEnabled() && providerConfig.supportsRealtime && serviceReady
        val manualRates = rateRepository.getManualRates()
        val savedRates = rateRepository.getSavedRates()
        val manualCurrencies = (manualRates + savedRates)
            .flatMap { listOf(it.from, it.to) }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()

        _uiState.value = MainSettingsUiState(
            realtimeEnabled = realtimeEnabled,
            serviceReady = serviceReady,
            provider = providerConfig.id,
            realtimeAvailable = providerConfig.supportsRealtime,
            liveScanEnabled = settingsRepository.isLiveScanEnabled(),
            manualCurrencies = manualCurrencies,
            manualRates = manualRates,
            savedRates = savedRates,
            homeCurrency = settingsRepository.getHomeCurrency(),
            destinationCurrency = settingsRepository.getDestinationCurrency()
        )

        if (providerConfig.id == Constants.PROVIDER_FRANKFURTER) {
            viewModelScope.launch {
                rateUpdateRepository.refreshSavedRatesIfStale()
            }
        }
    }

    fun setRealtimeEnabled(enabled: Boolean) {
        val currentState = _uiState.value
        if (!currentState.realtimeAvailable) {
            settingsRepository.setRealtimeEnabled(false)
            _uiState.value = currentState.copy(realtimeEnabled = false)
            return
        }
        if (!currentState.serviceReady) {
            _uiState.value = currentState.copy(realtimeEnabled = false)
            return
        }
        settingsRepository.setRealtimeEnabled(enabled)
        _uiState.value = currentState.copy(realtimeEnabled = enabled)
    }

    fun setLiveScanEnabled(enabled: Boolean) {
        settingsRepository.setLiveScanEnabled(enabled)
        _uiState.value = _uiState.value.copy(liveScanEnabled = enabled)
    }
}
