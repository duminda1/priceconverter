package com.pocketcurrency.viewmodel

import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.model.RateProvider
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
class MainSettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun refreshState_buildsManualCurrencies_andRefreshesFrankfurterRates() =
        runTest(dispatcherRule.testDispatcher) {
            val settingsRepository = mockk<SettingsRepository>(relaxed = true)
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

            val viewModel = MainSettingsViewModel(
                rateRepository,
                settingsRepository,
                rateUpdateRepository
            )

            advanceUntilIdle()

            assertEquals(listOf("AUD", "JPY", "USD"), viewModel.uiState.value.manualCurrencies)
            coVerify { rateUpdateRepository.refreshSavedRatesIfStale() }
        }

    @Test
    fun setRealtimeEnabled_notAvailable_forcesFalse() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        every { settingsRepository.isRealtimeEnabled() } returns true

        val rateRepository = baseRateRepository()

        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER

        val viewModel = MainSettingsViewModel(
            rateRepository,
            settingsRepository,
            rateUpdateRepository
        )

        viewModel.setRealtimeEnabled(true)

        assertFalse(viewModel.uiState.value.realtimeEnabled)
        verify { settingsRepository.setRealtimeEnabled(false) }
    }

    @Test
    fun setLiveScanEnabled_updatesState() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        val viewModel = MainSettingsViewModel(
            baseRateRepository(),
            settingsRepository,
            baseProvider()
        )

        viewModel.setLiveScanEnabled(false)

        assertFalse(viewModel.uiState.value.liveScanEnabled)
        verify { settingsRepository.setLiveScanEnabled(false) }
    }

    private fun baseSettingsRepository(): SettingsRepository {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.hasApiKey() } returns true
        every { settingsRepository.isRealtimeEnabled() } returns true
        every { settingsRepository.isLiveScanEnabled() } returns true
        every { settingsRepository.getHomeCurrency() } returns "USD"
        every { settingsRepository.getDestinationCurrency() } returns "AUD"
        return settingsRepository
    }

    private fun baseRateRepository(): RateRepository {
        val rateRepository = mockk<RateRepository>()
        every { rateRepository.getManualRates() } returns emptyList()
        every { rateRepository.getSavedRates() } returns emptyList()
        return rateRepository
    }

    private fun baseProvider(): RateUpdateRepository {
        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.EXCHANGE_RATES
        return rateUpdateRepository
    }
}
