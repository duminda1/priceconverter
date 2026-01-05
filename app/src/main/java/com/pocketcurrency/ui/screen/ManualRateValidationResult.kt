package com.pocketcurrency.ui.screen

internal data class ManualRateValidationResult(
    val normalizedFrom: String,
    val normalizedTo: String,
    val rateValue: Double?,
    val fromError: String?,
    val toError: String?,
    val rateError: String?
) {
    val isValid: Boolean
        get() = fromError == null && toError == null && rateError == null
}
