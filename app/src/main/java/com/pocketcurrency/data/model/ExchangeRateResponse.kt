package com.pocketcurrency.data.model

import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("info") val info: Info
) {
    data class Info(
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("quote") val quote: Double
    )
}
