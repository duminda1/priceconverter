package com.priceconverter.data.model

data class ConvertResponse(
    val success: Boolean,
    val query: Query?,
    val info: Info?,
    val result: Double?,
    val error: ApiError?
)

data class Query(
    val from: String,
    val to: String,
    val amount: Double
)

data class Info(
    val rate: Double?,
    val timestamp: Long?
)

data class ApiError(
    val code: Int?,
    val info: String?
)
