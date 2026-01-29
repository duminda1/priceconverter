package com.pockettoolsstudio.pocketcurrency.data.api

import com.pockettoolsstudio.pocketcurrency.data.model.ApiError

object ExchangeRateErrorMapper {
    fun map(error: ApiError?): String? {
        if (error == null) {
            return null
        }
        return when (error.code) {
            101 -> "Invalid API key. Please check it in Settings."
            102 -> "API account is inactive. Please activate it or contact support."
            103 -> "Requested API function is not available. Please try again later."
            104 -> "Monthly API request limit reached. Upgrade your plan or wait for the next cycle."
            105 -> "Your plan does not support this request. Upgrade to enable this feature."
            106 -> "No results found for this currency pair."
            201 -> "Invalid base currency. Check the source currency code."
            202 -> "Invalid currency code(s). Check your selection."
            301 -> "Date is required for historical rates."
            302 -> "Invalid date. Please check and try again."
            401 -> "Invalid source currency. Check the \"from\" value."
            402 -> "Invalid target currency. Check the \"to\" value."
            403 -> "Invalid amount. Enter a numeric amount."
            404 -> "Requested resource not found. Please try again later."
            else -> {
                val info = error.info?.lowercase().orEmpty()
                when {
                    info.contains("access key") && info.contains("invalid") ->
                        "Invalid API key. Please check it in Settings."
                    info.contains("access key") && info.contains("missing") ->
                        "API key missing. Add it in Settings to use Live rates."
                    info.contains("not found") ->
                        "Service unavailable. Please try again."
                    else -> null
                }
            }
        }
    }
}
