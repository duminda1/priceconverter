package com.pocketcurrency.data.api

import com.pocketcurrency.data.model.FrankfurterResponse
import retrofit2.http.GET
import retrofit2.http.Query
import androidx.annotation.Keep

@Keep
interface FrankfurterApi {
    @GET("latest")
    suspend fun latest(
        @Query("from") from: String,
        @Query("to") to: String,
        @Query("amount") amount: Double
    ): FrankfurterResponse
}
