package com.pocketcurrency.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviders
import com.pocketcurrency.data.repository.RateUpdatePolicyRegistry
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.model.RateProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class SettingsUiState(
    val service: String = "",
    val providers: List<RateProvider> = emptyList(),
    val apiKeyInput: String = "",
    val apiKeyStatus: String? = null,
    val actionStatus: String? = null,
    val isApiKeyVerified: Boolean = false,
    val isFreePlan: Boolean = true,
    val isLiveScanEnabled: Boolean = true,
    val homeCurrency: String = "",
    val destinationCurrency: String = "",
    val destinationAuto: Boolean = true,
    val usageCount: Int = 0,
    val usageMonth: String = "",
    val usageWarning: String? = null,
    val savedRates: List<CurrencyPairRate> = emptyList(),
    val manualRates: List<CurrencyPairRate> = emptyList(),
    val isVerifying: Boolean = false,
    val isRefreshingSavedRates: Boolean = false
)

class SettingsViewModel(
    application: Application,
    private val settingsRepository: SettingsRepository = SettingsRepository(application),
    private val rateUpdateRepository: RateUpdateRepository = RateUpdateRepository(
        settingsRepository = settingsRepository,
        providerRegistry = RateProviders.registry,
        networkMonitor = NetworkMonitor(application),
        policyRegistry = RateUpdatePolicyRegistry()
    )
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState

    init {
        refreshState()
    }

    fun refreshState() {

        val usage = settingsRepository.getUsageState()

        _uiState.value = _uiState.value.copy(
            service = settingsRepository.getService(),
            providers = RateProvider.values().toList(),
            apiKeyInput = settingsRepository.getApiKey().orEmpty(),
            isApiKeyVerified = settingsRepository.hasApiKey(),
            isFreePlan = settingsRepository.isFreePlan(),
            isLiveScanEnabled = settingsRepository.isLiveScanEnabled(),
            homeCurrency = settingsRepository.getHomeCurrency(),
            destinationCurrency = settingsRepository.getDestinationCurrency(),
            destinationAuto = settingsRepository.isDestinationAuto(),
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
            val result = rateUpdateRepository.verifyApiKey(apiKey)
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

    fun setHomeCurrency(currency: String) {
        settingsRepository.setHomeCurrency(currency)
        refreshState()
    }

    fun setDestinationAuto(enabled: Boolean) {
        settingsRepository.setDestinationAuto(enabled)
        refreshState()
    }

    fun setDestinationCurrency(currency: String) {
        settingsRepository.setDestinationCurrency(currency)
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
            val result = rateUpdateRepository.fetchRate(
                fromCurrency = normalizedFrom,
                toCurrency = normalizedTo,
                amount = 1.0,
                saveOnSuccess = true
            )
            if (result.rate != null) {
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

    fun refreshSavedRates() {
        val savedRates = settingsRepository.getSavedRates()
        if (savedRates.isEmpty()) {
            _uiState.value = _uiState.value.copy(actionStatus = "No saved rates to refresh.")
            return
        }
        if (_uiState.value.isRefreshingSavedRates) {
            return
        }

        _uiState.value = _uiState.value.copy(
            isRefreshingSavedRates = true,
            actionStatus = "Refreshing saved rates..."
        )

        viewModelScope.launch {
            var refreshedCount = 0
            var lastWarning: String? = null
            var firstError: String? = null

            savedRates.forEach { rate ->
                val result = rateUpdateRepository.fetchRate(
                    fromCurrency = rate.from,
                    toCurrency = rate.to,
                    amount = 1.0,
                    saveOnSuccess = true
                )
                if (result.rate != null) {
                    refreshedCount += 1
                } else if (firstError == null) {
                    firstError = result.errorMessage
                }
                if (result.warningMessage != null) {
                    lastWarning = result.warningMessage
                }
            }

            val status = if (firstError == null) {
                "Refreshed $refreshedCount/${savedRates.size} saved rates."
            } else {
                "Refreshed $refreshedCount/${savedRates.size} saved rates. ${firstError}"
            }

            _uiState.value = _uiState.value.copy(
                isRefreshingSavedRates = false,
                actionStatus = status,
                usageWarning = lastWarning
            )
            refreshState()
        }
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
