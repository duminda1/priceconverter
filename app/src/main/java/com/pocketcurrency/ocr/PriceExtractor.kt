package com.pocketcurrency.ocr

class PriceExtractor(
    private val amountParser: AmountParser = AmountParser()
) {

    /**
     * Extracts the most likely price and optional currency information from OCR text.
     */
    fun extract(text: String): DetectedPrice? = amountParser.parse(text)
}
