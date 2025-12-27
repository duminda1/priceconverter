package com.pocketcurrency.data.model

data class ApiRateResult(
    val rate: CurrencyRate?,
    val warningMessage: String?,
    val errorMessage: String?
)
