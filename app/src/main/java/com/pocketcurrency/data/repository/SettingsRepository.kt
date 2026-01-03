package com.pocketcurrency.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.pocketcurrency.data.model.CurrencyPairRate

class SettingsRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        SecurePrefsProvider.create(context)
    }

    private val userSettings: UserSettingsRepository by lazy {
        UserSettingsRepository(prefs)
    }

    private val usageRepository: ApiUsageRepository by lazy {
        ApiUsageRepository(prefs)
    }

    private val rateStorageRepository: RateStorageRepository by lazy {
        RateStorageRepository(prefs)
    }

    fun getService(): String = userSettings.getService()

    fun setService(service: String) {
        userSettings.setService(service)
    }

    fun getApiKey(): String? = userSettings.getApiKey()

    fun setApiKey(apiKey: String) {
        userSettings.setApiKey(apiKey)
    }

    fun hasApiKey(): Boolean = userSettings.hasApiKey()

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
}
