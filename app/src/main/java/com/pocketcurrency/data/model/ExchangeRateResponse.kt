package com.pocketcurrency.data.model

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class ExchangeRateResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("info") val info: Info
) {
    @Keep
    data class Info(
        @SerializedName("timestamp") val timestamp: Long,
        @SerializedName("quote") val quote: Double
    )
}
