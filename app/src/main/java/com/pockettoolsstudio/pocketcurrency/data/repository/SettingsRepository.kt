package com.pockettoolsstudio.pocketcurrency.data.repository

import android.content.SharedPreferences
import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyPairRate
import com.pockettoolsstudio.pocketcurrency.di.DefaultPrefs
import com.pockettoolsstudio.pocketcurrency.util.Constants
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    @DefaultPrefs private val prefs: SharedPreferences,
    private val apiKeyRepository: ApiKeyRepository,
    private val userSettings: UserSettingsRepository,
    private val usageRepository: ApiUsageRepository,
    private val rateStorageRepository: RateStorageRepository
) {

    fun getService(): String = userSettings.getService()

    fun setService(service: String) {
        userSettings.setService(service)
    }

    fun getApiKey(): String? = apiKeyRepository.getApiKey()

    fun setApiKey(apiKey: String) {
        apiKeyRepository.setApiKey(apiKey)
    }

    fun hasApiKey(): Boolean = apiKeyRepository.hasApiKey()

    fun isFreePlan(): Boolean = userSettings.isFreePlan()

    fun setFreePlan(isFree: Boolean) {
        userSettings.setFreePlan(isFree)
    }

    fun isRealtimeEnabled(): Boolean = userSettings.isRealtimeEnabled()

    fun setRealtimeEnabled(enabled: Boolean) {
        userSettings.setRealtimeEnabled(enabled)
    }

    fun isLiveScanEnabled(): Boolean = userSettings.isLiveScanEnabled()

    fun setLiveScanEnabled(enabled: Boolean) {
        userSettings.setLiveScanEnabled(enabled)
    }

    fun getHomeCurrency(): String = userSettings.getHomeCurrency()

    fun setHomeCurrency(currencyCode: String) {
        userSettings.setHomeCurrency(currencyCode)
    }

    fun isDestinationAuto(): Boolean = userSettings.isDestinationAuto()

    fun setDestinationAuto(enabled: Boolean) {
        userSettings.setDestinationAuto(enabled)
    }

    fun getDestinationCurrency(): String = userSettings.getDestinationCurrency()

    fun setDestinationCurrency(currencyCode: String) {
        userSettings.setDestinationCurrency(currencyCode)
    }

    fun getUsageState(): ApiUsageState = usageRepository.getUsageState()

    fun recordApiRequest(): String? = usageRepository.recordApiRequest()

    fun getSavedRates(): List<CurrencyPairRate> = rateStorageRepository.getSavedRates()

    fun upsertSavedRate(rate: CurrencyPairRate) {
        rateStorageRepository.upsertSavedRate(rate)
    }

    fun removeSavedRate(from: String, to: String) {
        rateStorageRepository.removeSavedRate(from, to)
    }

    fun findSavedRate(from: String, to: String): CurrencyPairRate? =
        rateStorageRepository.findSavedRate(from, to)

    fun getManualRates(): List<CurrencyPairRate> = rateStorageRepository.getManualRates()

    fun upsertManualRate(rate: CurrencyPairRate) {
        rateStorageRepository.upsertManualRate(rate)
    }

    fun removeManualRate(from: String, to: String) {
        rateStorageRepository.removeManualRate(from, to)
    }

    fun findManualRate(from: String, to: String): CurrencyPairRate? =
        rateStorageRepository.findManualRate(from, to)

    fun getLastRateRefreshCheck(providerId: String): Long {
        return prefs.getLong(rateRefreshCheckKey(providerId), 0L)
    }

    fun setLastRateRefreshCheck(providerId: String, lastCheckedMillis: Long) {
        prefs.edit().putLong(rateRefreshCheckKey(providerId), lastCheckedMillis).apply()
    }

    private fun rateRefreshCheckKey(providerId: String): String {
        return "${Constants.PREFS_RATE_REFRESH_CHECK_PREFIX}$providerId"
    }
}
