package com.pocketcurrency.domain.usecase

import com.pocketcurrency.data.model.CurrencyRate
import com.pocketcurrency.domain.model.Price
import com.pocketcurrency.domain.model.RateSource
import org.junit.Assert.assertEquals
import org.junit.Test

class ConvertCurrencyUseCaseTest {

    @Test
    fun execute_convertsUsingProvidedRate() {
        val useCase = ConvertCurrencyUseCase()
        val price = Price(amount = 12.50, currency = "AUD")
        val rate = CurrencyRate(rate = 1.6, lastUpdatedMillis = 0L, source = RateSource.LIVE)

        val result = useCase.execute(price, rate, toCurrency = "USD")

        assertEquals(20.0, result.convertedAmount, 0.0001)
        assertEquals("USD", result.toCurrency)
    }
}
