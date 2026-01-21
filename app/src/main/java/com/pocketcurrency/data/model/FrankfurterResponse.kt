package com.pocketcurrency.data.model

import com.google.gson.annotations.SerializedName
import androidx.annotation.Keep

@Keep
data class FrankfurterResponse(
    @SerializedName("amount") val amount: Double,
    @SerializedName("base") val base: String,
    @SerializedName("date") val date: String,
    @SerializedName("rates") val rates: Map<String, Double>
)
