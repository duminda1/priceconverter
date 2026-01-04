package com.pocketcurrency.data.repository

import com.pocketcurrency.data.model.ApiRateResult
import com.pocketcurrency.data.model.CurrencyPairRate
import com.pocketcurrency.domain.model.RateProvider
import com.pocketcurrency.domain.model.RateSource
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class RateRepositoryTest {

    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)
    private val rateUpdateRepository = mockk<RateUpdateRepository>(relaxed = true)
    private val repository = RateRepository(settingsRepository, rateUpdateRepository)

    @Test
    fun getRate_realtimeDisabled_usesSavedRate() = runBlocking {
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.EXCHANGE_RATES
        every { settingsRepository.hasApiKey() } returns true
        every { settingsRepository.findSavedRate("USD", "AUD") } returns CurrencyPairRate(
            from = "USD",
            to = "AUD",
            rate = 1.5,
            lastUpdatedMillis = 123L
        )
        every { settingsRepository.findManualRate(any(), any()) } returns null

        val result = repository.getRate(" usd ", "aud", realtimeEnabled = false)

        assertNotNull(result.rate)
        assertEquals(RateSource.SAVED, result.rate!!.source)
        assertEquals(1.5, result.rate!!.rate, 0.0001)
        coVerify(exactly = 0) {
            rateUpdateRepository.fetchRate(any(), any(), any(), any())
        }
    }

    @Test
    fun getRate_realtimeEnabled_fallsBackToManualWhenApiFails() = runBlocking {
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.EXCHANGE_RATES
        every { settingsRepository.hasApiKey() } returns true
        every { settingsRepository.findSavedRate(any(), any()) } returns null
        every { settingsRepository.findManualRate("USD", "AUD") } returns CurrencyPairRate(
            from = "USD",
            to = "AUD",
            rate = 1.1,
            lastUpdatedMillis = 456L
        )
        coEvery {
            rateUpdateRepository.fetchRate("USD", "AUD", 1.0, true)
        } returns ApiRateResult(
            rate = null,
            warningMessage = "Warning",
            errorMessage = "Service unavailable."
        )

        val result = repository.getRate("USD", "AUD", realtimeEnabled = true)

        assertNotNull(result.rate)
        assertEquals(RateSource.MANUAL, result.rate!!.source)
        assertEquals(1.1, result.rate!!.rate, 0.0001)
        assertEquals("Warning", result.warningMessage)
        assertNull(result.errorMessage)
    }

    @Test
    fun getRate_reverseSavedRateZero_doesNotCrashAndReturnsError() = runBlocking {
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER
        every { settingsRepository.hasApiKey() } returns false
        every { settingsRepository.findSavedRate("USD", "AUD") } returns null
        every { settingsRepository.findSavedRate("AUD", "USD") } returns CurrencyPairRate(
            from = "AUD",
            to = "USD",
            rate = 0.0,
            lastUpdatedMillis = 789L
        )
        every { settingsRepository.findManualRate(any(), any()) } returns null

        val result = repository.getRate("USD", "AUD", realtimeEnabled = false)

        assertNull(result.rate)
        assertEquals(
            "No saved or manual rates. Refresh a saved pair or add a manual rate.",
            result.errorMessage
        )
    }

    @Test
    fun getRate_reverseSavedRate_invertsValue() = runBlocking {
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER
        every { settingsRepository.hasApiKey() } returns false
        every { settingsRepository.findSavedRate("USD", "AUD") } returns null
        every { settingsRepository.findSavedRate("AUD", "USD") } returns CurrencyPairRate(
            from = "AUD",
            to = "USD",
            rate = 0.5,
            lastUpdatedMillis = 111L
        )
        every { settingsRepository.findManualRate(any(), any()) } returns null

        val result = repository.getRate("USD", "AUD", realtimeEnabled = false)

        assertNotNull(result.rate)
        assertEquals(RateSource.SAVED, result.rate!!.source)
        assertEquals(2.0, result.rate!!.rate, 0.0001)
    }

    @Test
    fun getRate_realtimeDisabled_marksStaleSavedRate() = runBlocking {
        every { rateUpdateRepository.getActiveProviderConfig() } returns RateProvider.FRANKFURTER
        every { settingsRepository.hasApiKey() } returns false
        every { settingsRepository.findSavedRate("USD", "AUD") } returns CurrencyPairRate(
            from = "USD",
            to = "AUD",
            rate = 1.5,
            lastUpdatedMillis = 100L
        )
        every { settingsRepository.findManualRate(any(), any()) } returns null
        every { rateUpdateRepository.isRateStale(100L, 200L) } returns true

        val repository = RateRepository(settingsRepository, rateUpdateRepository) { 200L }
        val result = repository.getRate("USD", "AUD", realtimeEnabled = false)

        assertNotNull(result.rate)
        assertEquals(true, result.isStale)
    }
}
