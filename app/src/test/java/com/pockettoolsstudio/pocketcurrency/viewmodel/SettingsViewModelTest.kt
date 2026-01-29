package com.pockettoolsstudio.pocketcurrency.viewmodel

import com.pockettoolsstudio.pocketcurrency.data.model.ApiRateResult
import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyPairRate
import com.pockettoolsstudio.pocketcurrency.data.model.CurrencyRate
import com.pockettoolsstudio.pocketcurrency.data.repository.ApiUsageState
import com.pockettoolsstudio.pocketcurrency.data.repository.RateUpdateRepository
import com.pockettoolsstudio.pocketcurrency.data.repository.SettingsRepository
import com.pockettoolsstudio.pocketcurrency.domain.model.RateSource
import com.pockettoolsstudio.pocketcurrency.util.Constants
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun verifyAndSaveApiKey_blank_setsStatus() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        val viewModel = SettingsViewModel(
            settingsRepository,
            rateUpdateRepository,
            TestStringProvider()
        )

        viewModel.onApiKeyChanged(" ")
        viewModel.verifyAndSaveApiKey()

        assertEquals("Enter a valid API key.", viewModel.uiState.value.apiKeyStatus)
        assertFalse(viewModel.uiState.value.isApiKeyVerified)
    }

    @Test
    fun verifyAndSaveApiKey_success_savesAndRefreshes() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        coEvery {
            rateUpdateRepository.verifyApiKey("NEWKEY")
        } returns ApiRateResult(
            rate = CurrencyRate(1.2, 100L, RateSource.LIVE),
            warningMessage = "Usage warning",
            errorMessage = null
        )
        every { settingsRepository.getApiKey() } returns "NEWKEY"

        val viewModel = SettingsViewModel(
            settingsRepository,
            rateUpdateRepository,
            TestStringProvider()
        )

        viewModel.onApiKeyChanged("NEWKEY")
        viewModel.verifyAndSaveApiKey()
        advanceUntilIdle()

        verify { settingsRepository.setApiKey("NEWKEY") }
        assertEquals("API key verified and saved.", viewModel.uiState.value.apiKeyStatus)
        assertTrue(viewModel.uiState.value.isApiKeyVerified)
        assertEquals("Usage warning", viewModel.uiState.value.usageWarning)
    }

    @Test
    fun verifyAndSaveApiKey_failure_setsError() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
        coEvery {
            rateUpdateRepository.verifyApiKey("BAD")
        } returns ApiRateResult(
            rate = null,
            warningMessage = "Warn",
            errorMessage = "Invalid key"
        )

        val viewModel = SettingsViewModel(
            settingsRepository,
            rateUpdateRepository,
            TestStringProvider()
        )

        viewModel.onApiKeyChanged("BAD")
        viewModel.verifyAndSaveApiKey()
        advanceUntilIdle()

        assertEquals("Invalid key", viewModel.uiState.value.apiKeyStatus)
        assertFalse(viewModel.uiState.value.isApiKeyVerified)
        assertEquals("Warn", viewModel.uiState.value.usageWarning)
    }

    @Test
    fun fetchAndReplaceRate_invalidCodes_setsActionStatus() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = SettingsViewModel(
            baseSettingsRepository(),
            mockk(relaxed = true),
            TestStringProvider()
        )

        viewModel.fetchAndReplaceRate(null, " ", "USD")

        assertEquals("Enter valid currency codes.", viewModel.uiState.value.actionStatus)
    }

    @Test
    fun fetchAndReplaceRate_success_removesOriginalAndUpdatesStatus() =
        runTest(dispatcherRule.testDispatcher) {
            val settingsRepository = baseSettingsRepository()
            val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
            coEvery {
                rateUpdateRepository.fetchRate("USD", "AUD", 1.0, true)
            } returns ApiRateResult(
                rate = CurrencyRate(1.3, 200L, RateSource.LIVE),
                warningMessage = "Warn",
                errorMessage = null
            )

            val viewModel = SettingsViewModel(
                settingsRepository,
                rateUpdateRepository,
                TestStringProvider()
            )
            val original = CurrencyPairRate("EUR", "GBP", 0.9, 10L)

            viewModel.fetchAndReplaceRate(original, "usd", "aud")
            advanceUntilIdle()

            verify { settingsRepository.removeSavedRate("EUR", "GBP") }
            assertEquals("Saved rate for USD/AUD.", viewModel.uiState.value.actionStatus)
            assertEquals("Warn", viewModel.uiState.value.usageWarning)
        }

    @Test
    fun refreshSavedRates_empty_setsStatus() = runTest(dispatcherRule.testDispatcher) {
        val settingsRepository = baseSettingsRepository()
        every { settingsRepository.getSavedRates() } returns emptyList()
        val viewModel = SettingsViewModel(
            settingsRepository,
            mockk(relaxed = true),
            TestStringProvider()
        )

        viewModel.refreshSavedRates()

        assertEquals("No saved rates to refresh.", viewModel.uiState.value.actionStatus)
    }

    @Test
    fun refreshSavedRates_mixedResults_updatesStatusAndWarning() =
        runTest(dispatcherRule.testDispatcher) {
            val savedRates = listOf(
                CurrencyPairRate("USD", "AUD", 1.1, 1L),
                CurrencyPairRate("EUR", "JPY", 2.2, 2L)
            )
            val settingsRepository = baseSettingsRepository()
            val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
            every { settingsRepository.getSavedRates() } returns savedRates
            coEvery {
                rateUpdateRepository.fetchRate("USD", "AUD", 1.0, true)
            } returns ApiRateResult(
                rate = CurrencyRate(1.1, 1L, RateSource.LIVE),
                warningMessage = null,
                errorMessage = null
            )
            coEvery {
                rateUpdateRepository.fetchRate("EUR", "JPY", 1.0, true)
            } returns ApiRateResult(
                rate = null,
                warningMessage = "Warn",
                errorMessage = "Service down"
            )

            val viewModel = SettingsViewModel(
                settingsRepository,
                rateUpdateRepository,
                TestStringProvider()
            )

            viewModel.refreshSavedRates()
            advanceUntilIdle()

            assertEquals(
                "Refreshed 1/2 saved rates. Service down",
                viewModel.uiState.value.actionStatus
            )
            assertEquals("Warn", viewModel.uiState.value.usageWarning)
            assertFalse(viewModel.uiState.value.isRefreshingSavedRates)
        }

    @Test
    fun upsertManualRate_invalidCodes_setsStatus() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = SettingsViewModel(
            baseSettingsRepository(),
            mockk(relaxed = true),
            TestStringProvider()
        )

        viewModel.upsertManualRate("", "USD", 1.0)

        assertEquals("Enter valid currency codes.", viewModel.uiState.value.actionStatus)
    }

    @Test
    fun upsertManualRate_valid_savesRateAndUpdatesStatus() =
        runTest(dispatcherRule.testDispatcher) {
            val settingsRepository = baseSettingsRepository()
            val viewModel = SettingsViewModel(
                settingsRepository,
                mockk(relaxed = true),
                TestStringProvider()
            )

            viewModel.upsertManualRate("usd", "aud", 1.25)

            verify { settingsRepository.upsertManualRate(any()) }
            assertEquals("Manual rate saved.", viewModel.uiState.value.actionStatus)
        }

    private fun baseSettingsRepository(): SettingsRepository {
        val settingsRepository = mockk<SettingsRepository>(relaxed = true)
        every { settingsRepository.getUsageState() } returns ApiUsageState(
            monthKey = "2024-01",
            count = 0,
            warn50 = false,
            warn75 = false,
            warn90 = false
        )
        every { settingsRepository.getService() } returns Constants.PROVIDER_EXCHANGE_RATES
        every { settingsRepository.getApiKey() } returns "KEY"
        every { settingsRepository.hasApiKey() } returns true
        every { settingsRepository.isFreePlan() } returns true
        every { settingsRepository.isLiveScanEnabled() } returns true
        every { settingsRepository.getHomeCurrency() } returns "USD"
        every { settingsRepository.getDestinationCurrency() } returns "AUD"
        every { settingsRepository.isDestinationAuto() } returns true
        every { settingsRepository.getSavedRates() } returns emptyList()
        every { settingsRepository.getManualRates() } returns emptyList()
        return settingsRepository
    }
}
