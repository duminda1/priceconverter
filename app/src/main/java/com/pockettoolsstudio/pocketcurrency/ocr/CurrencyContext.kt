package com.pockettoolsstudio.pocketcurrency.ocr

import java.util.Locale

data class CurrencyContext(
    val selectedFromCurrency: String? = null,
    val locale: Locale = Locale.getDefault()
)
