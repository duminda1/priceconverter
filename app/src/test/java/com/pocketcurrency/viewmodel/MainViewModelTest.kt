package com.pocketcurrency.viewmodel

import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.RateResult
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.model.RateProvider
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.domain.model.ServiceStatusType
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun refreshSettings_buildsManualCurrencies_andRefreshesFrankfurterRates() =
        runTest(dispatcherRule.testDispatcher) {
            val settingsRepository = mockk<SettingsRepository>(relaxed = true)
            every { settingsRepository.getService() } returns RateProvider.FRANKFURTER.id
            every { settingsRepository.hasApiKey() } returns false
            every { settingsRepository.isRealtimeEnabled() } returns false
            every { settingsRepository.isLiveScanEnabled() } returns true
            every { settingsRepository.getHomeCurrency() } returns "USD"
            every { settingsRepository.getDestinationCurrency() } returns "AUD"

            val rateRepository = mockk<RateRepository>()
            every { rateRepository.getManualRates() } returns listOf(
                CurrencyPairRate("USD", "AUD", 1.2, 10L)
            )
            every { rateRepository.getSavedRates() } returns listOf(
                CurrencyPairRate("JPY", "USD", 0.01, 11L)
            )

            val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
            every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER
            coEvery { rateUpdateRepository.refreshSavedRatesIfStale() } returns Unit

            val viewModel = MainViewModel(
                mockk(relaxed = true),
                rateRepository,
                settingsRepository,
                rateUpdateRepository
            )

            advanceUntilIdle()

            assertEquals(listOf("AUD", "JPY", "USD"), viewModel.manualCurrencies.value)
            coVerify { rateUpdateRepository.refreshSavedRatesIfStale() }
        }

    @Test
    fun setRealtimeEnabled_notAvailable_forcesFalse() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.getService() } returns RateProvider.FRANKFURTER.id
        every { settingsRepository.hasApiKey() } returns false
        every { settingsRepository.isRealtimeEnabled() } returns true
        every { settingsRepository.isLiveScanEnabled() } returns true
        every { settingsRepository.getHomeCurrency() } returns "USD"
        every { settingsRepository.getDestinationCurrency() } returns "AUD"

        val rateRepository = mockk<RateRepository>()
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()

        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER

        val viewModel = MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.setRealtimeEnabled(true)

        assertFalse(viewModel.realtimeEnabled.value)
        verify { settingsRepository.setRealtimeEnabled(false) }
    }

    @Test
    fun convertPrice_invalidCurrency_setsError() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.convertPrice(12.0, " ", "USD")

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Enter valid currency codes.", state.message)
    }

    @Test
    fun convertPrice_success_updatesState() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        val settingsRepository = baseSettings()
        val rateUpdateRepository = baseProvider(settingsRepository)
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = CurrencyRate(2.0, 123L, RateSource.SAVED),
            warningMessage = "Warn",
            errorMessage = null
        )
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()

        val viewModel = MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.convertPrice(10.0, "usd", "aud")
        advanceUntilIdle()

        val state = viewModel.conversionState.value as ConversionState.Success
        assertEquals(20.0, state.result.convertedAmount, 0.0001)
        assertEquals(2.0, viewModel.currentRate.value ?: 0.0, 0.0001)
        assertEquals(RateSource.SAVED, viewModel.currentRateSource.value)
        assertEquals(ServiceStatusType.SAVED, viewModel.serviceStatus.value!!.type)
        assertEquals("Warn", viewModel.usageWarning.value)
    }

    @Test
    fun convertPrice_repositoryError_setsError() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        val settingsRepository = baseSettings()
        val rateUpdateRepository = baseProvider(settingsRepository)
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = null,
            warningMessage = null,
            errorMessage = "Service down"
        )
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()

        val viewModel = MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.convertPrice(10.0, "USD", "AUD")
        advanceUntilIdle()

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Service down", state.message)
    }

    @Test
    fun convertPrice_exception_setsError() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        val settingsRepository = baseSettings()
        val rateUpdateRepository = baseProvider(settingsRepository)
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } throws RuntimeException("Boom")
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()

        val viewModel = MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.convertPrice(10.0, "USD", "AUD")
        advanceUntilIdle()

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Boom", state.message)
    }

    @Test
    fun setLiveScanEnabled_updatesState() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettings()
        val viewModel = buildViewModel(settingsRepository = settingsRepository)

        viewModel.setLiveScanEnabled(false)

        assertFalse(viewModel.liveScanEnabled.value)
        verify { settingsRepository.setLiveScanEnabled(false) }
    }

    @Test
    fun clearUsageWarning_resetsWarning() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        val settingsRepository = baseSettings()
        val rateUpdateRepository = baseProvider(settingsRepository)
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = CurrencyRate(1.5, 123L, RateSource.LIVE),
            warningMessage = "Warn",
            errorMessage = null
        )
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()

        val viewModel = MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.convertPrice(10.0, "USD", "AUD")
        advanceUntilIdle()
        assertEquals("Warn", viewModel.usageWarning.value)

        viewModel.clearUsageWarning()

        assertEquals(null, viewModel.usageWarning.value)
    }

    private fun buildViewModel(
        settingsRepository: SettingsRepository = baseSettings(),
        rateRepository: RateRepository = mockk(),
        rateUpdateRepository: RateUpdateRepository = baseProvider(settingsRepository)
    ): MainViewModel {
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()
        return MainViewModel(
            mockk(relaxed = true),
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )
    }

    private fun baseSettings(): SettingsRepository {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.getService() } returns RateProvider.EXCHANGE_RATES.id
        every { settingsRepository.hasApiKey() } returns true
        every { settingsRepository.isRealtimeEnabled() } returns true
        every { settingsRepository.isLiveScanEnabled() } returns true
        every { settingsRepository.getHomeCurrency() } returns "USD"
        every { settingsRepository.getDestinationCurrency() } returns "AUD"
        return settingsRepository
    }

    private fun baseProvider(settingsRepository: SettingsRepository): RateUpdateRepository {
        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.EXCHANGE_RATES
        return rateUpdateRepository
    }
}
