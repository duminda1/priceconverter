package com.pocketcurrency.data.model

import com.google.gson.annotations.SerializedName

data class ConvertResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("query") val query: Query?,
    @SerializedName("info") val info: Info?,
    @SerializedName("result") val result: Double?,
    @SerializedName("error") val error: ApiError?
)

data class Query(
    @SerializedName("from") val from: String,
    @SerializedName("to") val to: String,
    @SerializedName("amount") val amount: Double
)

data class Info(
    @SerializedName("rate") val rate: Double?,
    @SerializedName("timestamp") val timestamp: Long?
)

data class ApiError(
    @SerializedName("code") val code: Int?,
    @SerializedName("info") val info: String?
)
