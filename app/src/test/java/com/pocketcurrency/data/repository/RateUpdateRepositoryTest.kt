package com.pocketcurrency.data.repository

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.data.network.NetworkMonitor
import com.pocketcurrency.data.provider.RateProviderClient
import com.pocketcurrency.data.provider.RateProviderRegistry
import com.pocketcurrency.domain.model.RateProvider
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.utils.FrankfurterSchedule
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class RateUpdateRepositoryTest {

    @Test
    fun fetchRate_offline_returnsError() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val providerRegistry = mockk<RateProviderRegistry>(relaxed = true)
        val networkMonitor = mockk<NetworkMonitor>()
        every { networkMonitor.isOnline() } returns false
        val repository = RateUpdateRepository(
            settingsRepository = settingsRepository,
            providerRegistry = providerRegistry,
            networkMonitor = networkMonitor,
            policyRegistry = RateUpdatePolicyRegistry()
        )

        val result = repository.fetchRate("usd", "aud")

        assertEquals("No internet connection. Please try again.", result.errorMessage)
    }

    @Test
    fun fetchRate_savesRateAndReturnsUsageWarning() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val networkMonitor = mockk<NetworkMonitor>()
        every { networkMonitor.isOnline() } returns true
        every { settingsRepository.getService() } returns RateProvider.EXCHANGE_RATES.id
        every { settingsRepository.getApiKey() } returns "KEY"
        every { settingsRepository.recordApiRequest() } returns "Usage warning"

        val rate = CurrencyRate(
            rate = 1.25,
            lastUpdatedMillis = 123L,
            source = RateSource.LIVE
        )
        val provider = FakeProvider(
            config = RateProvider.EXCHANGE_RATES,
            fetchResult = ApiRateResult(rate, warningMessage = null, errorMessage = null)
        )
        val repository = RateUpdateRepository(
            settingsRepository = settingsRepository,
            providerRegistry = RateProviderRegistry(listOf(provider)),
            networkMonitor = networkMonitor,
            policyRegistry = RateUpdatePolicyRegistry()
        )

        val result = repository.fetchRate(" usd ", "aud", amount = 1.0, saveOnSuccess = true)

        assertEquals("Usage warning", result.warningMessage)
        verify {
            settingsRepository.upsertSavedRate(
                CurrencyPairRate(
                    from = "USD",
                    to = "AUD",
                    rate = 1.25,
                    lastUpdatedMillis = 123L
                )
            )
        }
    }

    @Test
    fun fetchRate_missingApiKey_skipsProviderCall() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val networkMonitor = mockk<NetworkMonitor>()
        every { networkMonitor.isOnline() } returns true
        every { settingsRepository.getService() } returns RateProvider.EXCHANGE_RATES.id
        every { settingsRepository.getApiKey() } returns null

        val provider = RecordingProvider(RateProvider.EXCHANGE_RATES)
        val repository = RateUpdateRepository(
            settingsRepository = settingsRepository,
            providerRegistry = RateProviderRegistry(listOf(provider)),
            networkMonitor = networkMonitor,
            policyRegistry = RateUpdatePolicyRegistry()
        )

        val result = repository.fetchRate("USD", "AUD", amount = 1.0, saveOnSuccess = true)

        assertEquals(
            "API key missing. Add it in Settings to use Live rates.",
            result.errorMessage
        )
        assertFalse(provider.fetchCalled)
    }

    @Test
    fun refreshSavedRatesIfStale_skipsWhenAlreadyChecked() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val networkMonitor = mockk<NetworkMonitor>()
        every { networkMonitor.isOnline() } returns true
        every { settingsRepository.getService() } returns RateProvider.FRANKFURTER.id

        val nowMillis = 1_720_000_000_000L
        val lastScheduled = FrankfurterSchedule.lastScheduledUpdateMillis(nowMillis)
        every { settingsRepository.getLastRateRefreshCheck(RateProvider.FRANKFURTER.id) } returns
            lastScheduled

        val provider = RecordingProvider(RateProvider.FRANKFURTER)
        val repository = RateUpdateRepository(
            settingsRepository = settingsRepository,
            providerRegistry = RateProviderRegistry(listOf(provider)),
            networkMonitor = networkMonitor,
            policyRegistry = RateUpdatePolicyRegistry()
        )

        repository.refreshSavedRatesIfStale(nowMillis)

        assertFalse(provider.fetchCalled)
        verify(exactly = 0) {
            settingsRepository.setLastRateRefreshCheck(any(), any())
        }
    }

    @Test
    fun refreshSavedRatesIfStale_updatesCheckAndRefreshesStaleRates() = runBlocking {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        val networkMonitor = mockk<NetworkMonitor>()
        every { networkMonitor.isOnline() } returns true
        every { settingsRepository.getService() } returns RateProvider.FRANKFURTER.id

        val nowMillis = 1_720_000_000_000L
        val lastScheduled = FrankfurterSchedule.lastScheduledUpdateMillis(nowMillis)
        every { settingsRepository.getLastRateRefreshCheck(RateProvider.FRANKFURTER.id) } returns
            lastScheduled - 1
        every { settingsRepository.getSavedRates() } returns listOf(
            CurrencyPairRate("USD", "AUD", 1.2, lastScheduled - 1_000)
        )

        val provider = FakeProvider(
            config = RateProvider.FRANKFURTER,
            fetchResult = ApiRateResult(
                rate = CurrencyRate(1.25, lastScheduled, RateSource.LIVE),
                warningMessage = null,
                errorMessage = null
            )
        )
        val repository = RateUpdateRepository(
            settingsRepository = settingsRepository,
            providerRegistry = RateProviderRegistry(listOf(provider)),
            networkMonitor = networkMonitor,
            policyRegistry = RateUpdatePolicyRegistry()
        )

        repository.refreshSavedRatesIfStale(nowMillis)

        verify {
            settingsRepository.setLastRateRefreshCheck(
                RateProvider.FRANKFURTER.id,
                nowMillis
            )
        }
        verify {
            settingsRepository.upsertSavedRate(
                CurrencyPairRate(
                    from = "USD",
                    to = "AUD",
                    rate = 1.25,
                    lastUpdatedMillis = lastScheduled
                )
            )
        }
    }

    private class FakeProvider(
        override val config: RateProvider,
        private val fetchResult: ApiRateResult,
        private val verifyResult: ApiRateResult = ApiRateResult(null, null, null)
    ) : RateProviderClient {
        override suspend fun fetchRate(
            fromCurrency: String,
            toCurrency: String,
            amount: Double,
            apiKey: String?
        ): ApiRateResult = fetchResult

        override suspend fun verifyApiKey(apiKey: String): ApiRateResult = verifyResult
    }

    private class RecordingProvider(
        override val config: RateProvider
    ) : RateProviderClient {
        var fetchCalled = false

        override suspend fun fetchRate(
            fromCurrency: String,
            toCurrency: String,
            amount: Double,
            apiKey: String?
        ): ApiRateResult {
            fetchCalled = true
            return ApiRateResult(null, null, null)
        }

        override suspend fun verifyApiKey(apiKey: String): ApiRateResult {
            return ApiRateResult(null, null, null)
        }
    }
}
