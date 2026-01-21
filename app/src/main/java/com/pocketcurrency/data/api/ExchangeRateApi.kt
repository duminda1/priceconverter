package com.pocketcurrency.data.api

import com.pocketcurrency.data.model.ConvertResponse
import retrofit2.http.GET
import retrofit2.http.Query
import androidx.annotation.Keep

@Keep
interface ExchangeRateApi {

    // Convert endpoint: https://api.exchangerate.host/convert?access_key=KEY&from=USD&to=AUD&amount=1
    @GET("convert")
    suspend fun convert(
        @Query("access_key") accessKey: String,
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amount") amount: Double
    ): ConvertResponse
}
