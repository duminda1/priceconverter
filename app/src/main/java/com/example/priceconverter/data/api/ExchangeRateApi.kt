package com.example.priceconverter.data.api

import com.example.priceconverter.data.model.ConvertResponse
import com.example.priceconverter.data.model.ExchangeRateResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {

    // Convert endpoint: https://api.exchangerate.host/convert?access_key=KEY&from=USD&to=AUD&amount=1
    @GET("convert")
    suspend fun convert(
        @Query("access_key") accessKey: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amount") amount: Double
    ): ExchangeRateResponse
}
