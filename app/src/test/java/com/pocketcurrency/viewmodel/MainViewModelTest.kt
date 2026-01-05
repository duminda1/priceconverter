package com.pocketcurrency.viewmodel

import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.data.repository.RateRepository
import com.pocketcurrency.data.repository.RateResult
import com.pocketcurrency.domain.model.RateSource
import com.pocketcurrency.domain.model.ServiceStatusType
import com.pocketcurrency.domain.usecase.ConvertCurrencyUseCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    @get:Rule
    val dispatcherRule = MainDispatcherRule()

    @Test
    fun convertPrice_invalidCurrency_setsError() = runTest(dispatcherRule.testDispatcher) {
        val viewModel = buildViewModel()

        viewModel.convertPrice(12.0, " ", "USD", realtimeEnabled = true)

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Enter valid currency codes.", state.message)
    }

    @Test
    fun convertPrice_success_updatesState() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = CurrencyRate(2.0, 123L, RateSource.SAVED),
            warningMessage = "Warn",
            errorMessage = null
        )
        val viewModel = MainViewModel(
            rateRepository,
            ConvertCurrencyUseCase(),
            TestStringProvider()
        )

        viewModel.convertPrice(10.0, "usd", "aud", realtimeEnabled = true)
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
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = null,
            warningMessage = null,
            errorMessage = "Service down"
        )
        val viewModel = MainViewModel(
            rateRepository,
            ConvertCurrencyUseCase(),
            TestStringProvider()
        )

        viewModel.convertPrice(10.0, "USD", "AUD", realtimeEnabled = true)
        advanceUntilIdle()

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Service down", state.message)
    }

    @Test
    fun convertPrice_exception_setsError() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } throws RuntimeException("Boom")

        val viewModel = MainViewModel(
            rateRepository,
            ConvertCurrencyUseCase(),
            TestStringProvider()
        )

        viewModel.convertPrice(10.0, "USD", "AUD", realtimeEnabled = true)
        advanceUntilIdle()

        val state = viewModel.conversionState.value as ConversionState.Error
        assertEquals("Boom", state.message)
    }

    @Test
    fun clearUsageWarning_resetsWarning() = runTest(dispatcherRule.testDispatcher) {
        val rateRepository = mockk<RateRepository>()
        coEvery {
            rateRepository.getRate("USD", "AUD", true)
        } returns RateResult(
            rate = CurrencyRate(1.5, 123L, RateSource.LIVE),
            warningMessage = "Warn",
            errorMessage = null
        )

        val viewModel = MainViewModel(
            rateRepository,
            ConvertCurrencyUseCase(),
            TestStringProvider()
        )

        viewModel.convertPrice(10.0, "USD", "AUD", realtimeEnabled = true)
        advanceUntilIdle()
        assertEquals("Warn", viewModel.usageWarning.value)

        viewModel.clearUsageWarning()

        assertEquals(null, viewModel.usageWarning.value)
    }

    private fun buildViewModel(
        rateRepository: RateRepository = mockk(relaxed = true)
    ): MainViewModel {
        return MainViewModel(
            rateRepository,
            ConvertCurrencyUseCase(),
            TestStringProvider()
        )
    }
}
