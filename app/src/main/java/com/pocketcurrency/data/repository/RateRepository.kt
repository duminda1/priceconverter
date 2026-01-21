package com.pocketcurrency.data.repository

import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.domain.model.RateSource
import javax.inject.Inject

data class RateResult(
    val rate: CurrencyRate?,
    val warningMessage: String?,
    val errorMessage: String?,
    val isStale: Boolean = false
) {
    val lastUpdatedAtMillis: Long?
        get() = rate?.lastUpdatedAtMillis
}

class RateRepository(
    private val settingsRepository: SettingsRepository,
    private val rateUpdateRepository: RateUpdateRepository,
    private val timeProvider: () -> Long = System::currentTimeMillis
) {
    @Inject
    constructor(
        settingsRepository: SettingsRepository,
        rateUpdateRepository: RateUpdateRepository
    ) : this(
        settingsRepository = settingsRepository,
        rateUpdateRepository = rateUpdateRepository,
        timeProvider = System::currentTimeMillis
    )

    fun getSavedRates(): List<CurrencyPairRate> = settingsRepository.getSavedRates()

    fun getManualRates(): List<CurrencyPairRate> = settingsRepository.getManualRates()

    suspend fun getRate(
        fromCurrency: String,
        toCurrency: String,
        realtimeEnabled: Boolean
    ): RateResult {
        val nowMillis = timeProvider()
        val normalizedFrom = fromCurrency.trim().uppercase()
        val normalizedTo = toCurrency.trim().uppercase()
        val savedRate = findSavedRate(normalizedFrom, normalizedTo)
        val manualRate = findManualRate(normalizedFrom, normalizedTo)
        val providerConfig = rateUpdateRepository.getActiveProviderConfig()
        val hasApiKey = settingsRepository.hasApiKey()
        val canUseRealtime = realtimeEnabled &&
            providerConfig.supportsRealtime &&
            (!providerConfig.requiresApiKey || hasApiKey)

        if (!canUseRealtime) {
            val fallback = savedRate ?: manualRate
            if (fallback != null) {
                return RateResult(
                    rate = fallback,
                    warningMessage = null,
                    errorMessage = null,
                    isStale = isStale(fallback, nowMillis)
                )
            }
            val message = when {
                providerConfig.requiresApiKey && hasApiKey ->
                    "No saved or manual rates for this pair."
                providerConfig.requiresApiKey ->
                    "No saved or manual rates. Add a manual rate or API key."
                else ->
                    "No saved or manual rates. Refresh a saved pair or add a manual rate."
            }
            return RateResult(rate = null, warningMessage = null, errorMessage = message)
        }

        val apiResult = rateUpdateRepository.fetchRate(
            fromCurrency = normalizedFrom,
            toCurrency = normalizedTo,
            amount = 1.0,
            saveOnSuccess = true
        )
        if (apiResult.rate != null) {
            return RateResult(
                rate = apiResult.rate,
                warningMessage = apiResult.warningMessage,
                errorMessage = null,
                isStale = false
            )
        }

        val fallback = savedRate ?: manualRate
        if (fallback != null) {
            return RateResult(
                rate = fallback,
                warningMessage = apiResult.warningMessage,
                errorMessage = null,
                isStale = isStale(fallback, nowMillis)
            )
        }

        return RateResult(
            rate = null,
            warningMessage = apiResult.warningMessage,
            errorMessage = apiResult.errorMessage ?: "Service unavailable. Please try again.",
            isStale = false
        )
    }

    private fun findSavedRate(from: String, to: String): CurrencyRate? {
        return findRate(
            from = from,
            to = to,
            source = RateSource.SAVED,
            lookup = settingsRepository::findSavedRate
        )
    }

    private fun findManualRate(from: String, to: String): CurrencyRate? {
        return findRate(
            from = from,
            to = to,
            source = RateSource.MANUAL,
            lookup = settingsRepository::findManualRate
        )
    }

    private fun findRate(
        from: String,
        to: String,
        source: RateSource,
        lookup: (String, String) -> CurrencyPairRate?
    ): CurrencyRate? {
        val direct = lookup(from, to)
        if (direct != null) {
            return toCurrencyRate(direct, source, invert = false)
        }
        val reverse = lookup(to, from)
        if (reverse != null) {
            return toCurrencyRate(reverse, source, invert = true)
        }
        return null
    }

    private fun isStale(rate: CurrencyRate, nowMillis: Long): Boolean {
        if (rate.source == RateSource.LIVE) {
            return false
        }
        return rateUpdateRepository.isRateStale(rate.lastUpdatedAtMillis, nowMillis)
    }

    private fun toCurrencyRate(
        rate: CurrencyPairRate,
        source: RateSource,
        invert: Boolean
    ): CurrencyRate? {
        if (invert && rate.rate == 0.0) {
            return null
        }
        val value = if (invert) 1.0 / rate.rate else rate.rate
        return CurrencyRate(
            rate = value,
            lastUpdatedMillis = rate.lastUpdatedMillis,
            source = source
        )
    }
}
