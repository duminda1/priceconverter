package com.pocketcurrency.ocr

enum class CurrencyConfidence {
    HIGH,
    MEDIUM,
    LOW
}

enum class CurrencySource {
    OCR_EXPLICIT,
    OCR_SYMBOL,
    USER_SELECTION,
    LOCALE_DEFAULT
}
