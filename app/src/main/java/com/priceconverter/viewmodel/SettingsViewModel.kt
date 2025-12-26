package com.priceconverter.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.priceconverter.data.model.CurrencyPairRate
import com.priceconverter.data.repository.ExchangeRateRepository
import com.priceconverter.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val service: String = "",
    val apiKeyInput: String = "",
    val apiKeyStatus: String? = null,
    val actionStatus: String? = null,
    val isApiKeyVerified: Boolean = false,
    val isFreePlan: Boolean = true,
    val isLiveScanEnabled: Boolean = true,
    val usageCount: Int = 0,
    val usageMonth: String = "",
    val usageWarning: String? = null,
    val savedRates: List<CurrencyPairRate> = emptyList(),
    val manualRates: List<CurrencyPairRate> = emptyList(),
    val isVerifying: Boolean = false
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsRepository = SettingsRepository(application)
    private val exchangeRepository = ExchangeRateRepository(settingsRepository)

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        refreshState()
    }

    fun refreshState() {
        val usage = settingsRepository.getUsageState()
        _uiState.value = _uiState.value.copy(
            service = settingsRepository.getService(),
            apiKeyInput = settingsRepository.getApiKey().orEmpty(),
            isApiKeyVerified = settingsRepository.hasApiKey(),
            isFreePlan = settingsRepository.isFreePlan(),
            isLiveScanEnabled = settingsRepository.isLiveScanEnabled(),
            usageCount = usage.count,
            usageMonth = usage.monthKey,
            savedRates = settingsRepository.getSavedRates(),
            manualRates = settingsRepository.getManualRates()
        )
    }

    fun onApiKeyChanged(value: String) {
        _uiState.value = _uiState.value.copy(
            apiKeyInput = value,
            apiKeyStatus = null,
            isApiKeyVerified = false
        )
    }

    fun verifyAndSaveApiKey() {
        val apiKey = _uiState.value.apiKeyInput.trim()
        if (apiKey.isBlank()) {
            _uiState.value = _uiState.value.copy(apiKeyStatus = "Enter a valid API key.")
            return
        }

        _uiState.value = _uiState.value.copy(isVerifying = true, apiKeyStatus = null)
        viewModelScope.launch {
            val result = exchangeRepository.verifyApiKey(apiKey)
            if (result.rate != null) {
                settingsRepository.setApiKey(apiKey)
                _uiState.value = _uiState.value.copy(
                    apiKeyStatus = "API key verified and saved.",
                    isApiKeyVerified = true,
                    isVerifying = false,
                    usageWarning = result.warningMessage
                )
                refreshState()
            } else {
                _uiState.value = _uiState.value.copy(
                    apiKeyStatus = result.errorMessage ?: "Unable to verify API key.",
                    isApiKeyVerified = false,
                    isVerifying = false,
                    usageWarning = result.warningMessage
                )
            }
        }
    }

    fun setFreePlan(isFreePlan: Boolean) {
        settingsRepository.setFreePlan(isFreePlan)
        refreshState()
    }

    fun setService(service: String) {
        settingsRepository.setService(service)
        refreshState()
    }

    fun setLiveScanEnabled(enabled: Boolean) {
        settingsRepository.setLiveScanEnabled(enabled)
        refreshState()
    }

    fun clearUsageWarning() {
        _uiState.value = _uiState.value.copy(usageWarning = null)
    }

    fun fetchAndSaveRate(from: String, to: String) {
        fetchAndReplaceRate(null, from, to)
    }

    fun fetchAndReplaceRate(
        original: CurrencyPairRate?,
        from: String,
        to: String
    ) {
        val normalizedFrom = from.trim().uppercase()
        val normalizedTo = to.trim().uppercase()
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) {
            _uiState.value = _uiState.value.copy(actionStatus = "Enter valid currency codes.")
            return
        }

        viewModelScope.launch {
            val result = exchangeRepository.getExchangeRate(normalizedFrom, normalizedTo, 1.0)
            if (result.rate != null) {
                settingsRepository.upsertSavedRate(
                    CurrencyPairRate(
                        from = normalizedFrom,
                        to = normalizedTo,
                        rate = result.rate.rate,
                        lastUpdatedMillis = result.rate.lastUpdatedMillis
                    )
                )
                if (original != null &&
                    (original.from != normalizedFrom || original.to != normalizedTo)
                ) {
                    settingsRepository.removeSavedRate(original.from, original.to)
                }
                _uiState.value = _uiState.value.copy(
                    actionStatus = "Saved rate for $normalizedFrom/$normalizedTo.",
                    usageWarning = result.warningMessage
                )
                refreshState()
            } else {
                _uiState.value = _uiState.value.copy(
                    actionStatus = result.errorMessage ?: "Failed to fetch rate.",
                    usageWarning = result.warningMessage
                )
            }
        }
    }

    fun removeSavedRate(from: String, to: String) {
        settingsRepository.removeSavedRate(from, to)
        refreshState()
    }

    fun upsertManualRate(from: String, to: String, rate: Double) {
        val normalizedFrom = from.trim().uppercase()
        val normalizedTo = to.trim().uppercase()
        if (normalizedFrom.isBlank() || normalizedTo.isBlank()) {
            _uiState.value = _uiState.value.copy(actionStatus = "Enter valid currency codes.")
            return
        }

        settingsRepository.upsertManualRate(
            CurrencyPairRate(
                from = normalizedFrom,
                to = normalizedTo,
                rate = rate,
                lastUpdatedMillis = System.currentTimeMillis()
            )
        )
        _uiState.value = _uiState.value.copy(actionStatus = "Manual rate saved.")
        refreshState()
    }

    fun removeManualRate(from: String, to: String) {
        settingsRepository.removeManualRate(from, to)
        refreshState()
    }
}
