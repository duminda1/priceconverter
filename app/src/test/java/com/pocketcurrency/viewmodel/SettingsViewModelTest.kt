package com.pocketcurrency.viewmodel

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.data.repository.ApiUsageState
import com.pocketcurrency.data.repository.RateUpdateRepository
import com.pocketcurrency.data.repository.SettingsRepository
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.utils.Constants
import io.mockk.capture
import io.mockk.coEvery
import io.mockk.constructedWith
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Before
    fun setUp() {
        mockkConstructor(SettingsRepository::class)
        mockkConstructor(RateUpdateRepository::class)
        stubDefaults()
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun verifyAndSaveApiKey_blank_setsStatus() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = SettingsViewModel(mockk(relaxed = true))

        viewModel.onApiKeyChanged(" ")
        viewModel.verifyAndSaveApiKey()

        assertEquals("Enter a valid API key.", viewModel.uiState.value.apiKeyStatus)
        assertFalse(viewModel.uiState.value.isApiKeyVerified)
    }

    @Test
    fun verifyAndSaveApiKey_success_savesAndRefreshes() = runTest(dispatcherRule.testDispatcher) {
        coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any()).verifyApiKey("NEWKEY")
        } returns ApiRateResult(
            rate = CurrencyRate(1.2, 100L, RateSource.LIVE),
            warningMessage = "Usage warning",
            errorMessage = null
        )
        every { constructedWith<SettingsRepository>(any()).getApiKey() } returns "NEWKEY"

        val viewModel = SettingsViewModel(mockk(relaxed = true))

        viewModel.onApiKeyChanged("NEWKEY")
        viewModel.verifyAndSaveApiKey()
        advanceUntilIdle()

        verify { constructedWith<SettingsRepository>(any()).setApiKey("NEWKEY") }
        assertEquals("API key verified and saved.", viewModel.uiState.value.apiKeyStatus)
        assertTrue(viewModel.uiState.value.isApiKeyVerified)
        assertEquals("Usage warning", viewModel.uiState.value.usageWarning)
    }

    @Test
    fun verifyAndSaveApiKey_failure_setsError() = runTest(dispatcherRule.testDispatcher) {
        coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any()).verifyApiKey("BAD")
        } returns ApiRateResult(
            rate = null,
            warningMessage = "Warn",
            errorMessage = "Invalid key"
        )

        val viewModel = SettingsViewModel(mockk(relaxed = true))

        viewModel.onApiKeyChanged("BAD")
        viewModel.verifyAndSaveApiKey()
        advanceUntilIdle()

        assertEquals("Invalid key", viewModel.uiState.value.apiKeyStatus)
        assertFalse(viewModel.uiState.value.isApiKeyVerified)
        assertEquals("Warn", viewModel.uiState.value.usageWarning)
    }

    @Test
    fun fetchAndReplaceRate_invalidCodes_setsActionStatus() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = SettingsViewModel(mockk(relaxed = true))

        viewModel.fetchAndReplaceRate(null, " ", "USD")

        assertEquals("Enter valid currency codes.", viewModel.uiState.value.actionStatus)
    }

    @Test
    fun fetchAndReplaceRate_success_removesOriginalAndUpdatesStatus() =
        runTest(dispatcherRule.testDispatcher) {
            coEvery {
                constructedWith<RateUpdateRepository>(any(), any(), any(), any())
                    .fetchRate("USD", "AUD", 1.0, true)
            } returns ApiRateResult(
                rate = CurrencyRate(1.3, 200L, RateSource.LIVE),
                warningMessage = "Warn",
                errorMessage = null
            )

            val viewModel = SettingsViewModel(mockk(relaxed = true))
            val original = CurrencyPairRate("EUR", "GBP", 0.9, 10L)

            viewModel.fetchAndReplaceRate(original, "usd", "aud")
            advanceUntilIdle()

            verify { constructedWith<SettingsRepository>(any()).removeSavedRate("EUR", "GBP") }
            assertEquals("Saved rate for USD/AUD.", viewModel.uiState.value.actionStatus)
            assertEquals("Warn", viewModel.uiState.value.usageWarning)
        }

    @Test
    fun refreshSavedRates_empty_setsStatus() = runTest(dispatcherRule.testDispatcher) {
        every { anyConstructed<SettingsRepository>().getSavedRates() } returns emptyList()
        val viewModel = SettingsViewModel(mockk(relaxed = true))

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
            every { anyConstructed<SettingsRepository>().getSavedRates() } returns savedRates
            coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any())
                .fetchRate("USD", "AUD", 1.0, true)
        } returns ApiRateResult(
            rate = CurrencyRate(1.1, 1L, RateSource.LIVE),
            warningMessage = null,
            errorMessage = null
        )
            coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any())
                .fetchRate("EUR", "JPY", 1.0, true)
        } returns ApiRateResult(
            rate = null,
            warningMessage = "Warn",
            errorMessage = "Service down"
        )

            val viewModel = SettingsViewModel(mockk(relaxed = true))

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
        val viewModel = SettingsViewModel(mockk(relaxed = true))

        viewModel.upsertManualRate("", "USD", 1.0)

        assertEquals("Enter valid currency codes.", viewModel.uiState.value.actionStatus)
    }

    @Test
    fun upsertManualRate_valid_savesRateAndUpdatesStatus() =
        runTest(dispatcherRule.testDispatcher) {
            val viewModel = SettingsViewModel(mockk(relaxed = true))

            viewModel.upsertManualRate("usd", "aud", 1.25)

            val captured = slot<CurrencyPairRate>()
            verify {
                constructedWith<SettingsRepository>(any()).upsertManualRate(capture(captured))
            }
            assertEquals("USD", captured.captured.from)
            assertEquals("AUD", captured.captured.to)
            assertEquals(1.25, captured.captured.rate, 0.0001)
            assertEquals("Manual rate saved.", viewModel.uiState.value.actionStatus)
        }

    private fun stubDefaults() {
        every {
            constructedWith<SettingsRepository>(any()).getUsageState()
        } returns ApiUsageState(
            monthKey = "2024-01",
            count = 0,
            warn50 = false,
            warn75 = false,
            warn90 = false
        )
        every { constructedWith<SettingsRepository>(any()).getService() } returns Constants.PROVIDER_EXCHANGE_RATES
        every { constructedWith<SettingsRepository>(any()).getApiKey() } returns "KEY"
        every { constructedWith<SettingsRepository>(any()).hasApiKey() } returns true
        every { constructedWith<SettingsRepository>(any()).isFreePlan() } returns true
        every { constructedWith<SettingsRepository>(any()).isLiveScanEnabled() } returns true
        every { constructedWith<SettingsRepository>(any()).getHomeCurrency() } returns "USD"
        every { constructedWith<SettingsRepository>(any()).getDestinationCurrency() } returns "AUD"
        every { constructedWith<SettingsRepository>(any()).isDestinationAuto() } returns true
        every { constructedWith<SettingsRepository>(any()).getSavedRates() } returns emptyList()
        every { constructedWith<SettingsRepository>(any()).getManualRates() } returns emptyList()

        every { constructedWith<SettingsRepository>(any()).setApiKey(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setFreePlan(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setService(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setLiveScanEnabled(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setHomeCurrency(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setDestinationAuto(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).setDestinationCurrency(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).removeSavedRate(any(), any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).upsertManualRate(any()) } returns Unit
        every { constructedWith<SettingsRepository>(any()).removeManualRate(any(), any()) } returns Unit

        coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any()).verifyApiKey(any())
        } returns ApiRateResult(rate = null, warningMessage = null, errorMessage = "Error")
        coEvery {
            constructedWith<RateUpdateRepository>(any(), any(), any(), any())
                .fetchRate(any(), any(), any(), any())
        } returns ApiRateResult(rate = null, warningMessage = null, errorMessage = "Error")
    }
}
