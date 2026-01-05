package com.pocketcurrency.data.repository

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviderRegistry
import com.pocketcurrency.util.Constants
import com.pocketcurrency.util.FrankfurterSchedule
import javax.inject.Inject

class RateUpdateRepository(
    private val settingsRepository: SettingsRepository,
    private val providerRegistry: RateProviderRegistry,
    private val networkMonitor: NetworkMonitor,
    private val policyRegistry: RateUpdatePolicyRegistry,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        providerRegistry: RateProviderRegistry,
        networkMonitor: NetworkMonitor,
        policyRegistry: RateUpdatePolicyRegistry
    ) : this(
        settingsRepository = settingsRepository,
        providerRegistry = providerRegistry,
        networkMonitor = networkMonitor,
        policyRegistry = policyRegistry,
        timeProvider = System::currentTimeMillis
    )

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

    fun isRateStale(lastUpdatedMillis: Long): Boolean {
        return isRateStale(lastUpdatedMillis, timeProvider())
    }

    fun isRateStale(lastUpdatedMillis: Long, nowMillis: Long): Boolean {
        val policy = policyRegistry.getPolicy(getActiveProviderConfig().id)
        return policy.isStale(lastUpdatedMillis, nowMillis)
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
        refreshSavedRatesIfStale(timeProvider())
    }

    suspend fun refreshSavedRatesIfStale(nowMillis: Long) {
        val provider = providerRegistry.getProvider(settingsRepository.getService())
        if (provider.config.id != Constants.PROVIDER_FRANKFURTER) {
            return
        }
        if (!networkMonitor.isOnline()) {
            return
        }

        val policy = policyRegistry.getPolicy(provider.config.id)
        val lastScheduledUpdate = FrankfurterSchedule.lastScheduledUpdateMillis(nowMillis)
        val lastChecked = settingsRepository.getLastRateRefreshCheck(provider.config.id)
        if (lastChecked >= lastScheduledUpdate) {
            return
        }

        settingsRepository.setLastRateRefreshCheck(provider.config.id, nowMillis)
        val savedRates = settingsRepository.getSavedRates()
        if (savedRates.isEmpty()) {
            return
        }

        val staleRates = savedRates.filter {
            policy.isStale(it.lastUpdatedAtMillis, nowMillis)
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
