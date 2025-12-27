package com.pocketcurrency.data.repository

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviderRegistry
import com.pocketcurrency.utils.Constants

class RateUpdateRepository(
    private val settingsRepository: SettingsRepository,
    private val providerRegistry: RateProviderRegistry,
    private val networkMonitor: NetworkMonitor,
    private val policyRegistry: RateUpdatePolicyRegistry
) {
    fun getActiveProviderConfig() =
        providerRegistry.getProvider(settingsRepository.getService()).config

    suspend fun fetchRate(
        fromCurrency: String,
        toCurrency: String,
        amount: Double = 1.0,
        saveOnSuccess: Boolean = false
    ): ApiRateResult {
        val normalizedFrom = fromCurrency.trim().uppercase()
        val normalizedTo = toCurrency.trim().uppercase()
        if (!networkMonitor.isOnline()) {
            return ApiRateResult(
                rate = null,
                warningMessage = null,
                errorMessage = "No internet connection. Please try again."
            )
        }

        val provider = providerRegistry.getProvider(settingsRepository.getService())
        val apiKey = if (provider.config.requiresApiKey) settingsRepository.getApiKey() else null
        if (provider.config.requiresApiKey && apiKey.isNullOrBlank()) {
            return ApiRateResult(
                rate = null,
                warningMessage = null,
                errorMessage = "API key missing. Add it in Settings to use Live rates."
            )
        }

        val result = provider.fetchRate(
            fromCurrency = normalizedFrom,
            toCurrency = normalizedTo,
            amount = amount,
            apiKey = apiKey
        )

        val warning = if (provider.config.supportsUsageTracking) {
            settingsRepository.recordApiRequest()
        } else {
            null
        }
        val updated = if (warning != null && result.warningMessage == null) {
            result.copy(warningMessage = warning)
        } else {
            result
        }

        if (updated.rate != null && saveOnSuccess) {
            settingsRepository.upsertSavedRate(
                CurrencyPairRate(
                    from = normalizedFrom,
                    to = normalizedTo,
                    rate = updated.rate.rate,
                    lastUpdatedMillis = updated.rate.lastUpdatedMillis
                )
            )
        }

        return updated
    }

    suspend fun verifyApiKey(apiKey: String): ApiRateResult {
        if (!networkMonitor.isOnline()) {
            return ApiRateResult(
                rate = null,
                warningMessage = null,
                errorMessage = "No internet connection. Please try again."
            )
        }

        val provider = providerRegistry.getProvider(settingsRepository.getService())
        if (!provider.config.requiresApiKey) {
            return ApiRateResult(
                rate = null,
                warningMessage = null,
                errorMessage = "API key not required for ${provider.config.displayName}."
            )
        }

        val result = provider.verifyApiKey(apiKey)
        val warning = if (provider.config.supportsUsageTracking) {
            settingsRepository.recordApiRequest()
        } else {
            null
        }
        return if (warning != null && result.warningMessage == null) {
            result.copy(warningMessage = warning)
        } else {
            result
        }
    }

    suspend fun refreshSavedRatesIfStale() {
        val provider = providerRegistry.getProvider(settingsRepository.getService())
        if (provider.config.id != Constants.PROVIDER_FRANKFURTER) {
            return
        }
        if (!networkMonitor.isOnline()) {
            return
        }

        val policy = policyRegistry.getPolicy(provider.config.id)
        val now = System.currentTimeMillis()
        val staleRates = settingsRepository.getSavedRates().filter {
            policy.isStale(it.lastUpdatedMillis, now)
        }
        if (staleRates.isEmpty()) {
            return
        }

        staleRates.forEach { rate ->
            fetchRate(
                fromCurrency = rate.from,
                toCurrency = rate.to,
                amount = 1.0,
                saveOnSuccess = true
            )
        }
    }
}
