package com.pocketcurrency.ocr

class TextRecognizerSession(
    private val factory: () -> TextRecognizerHelper = { TextRecognizerHelper() }
) : AutoCloseable {
    private var recognizer: TextRecognizerHelper? = null

    fun acquire(): TextRecognizerHelper {
        val current = recognizer
        if (current != null) {
            return current
        }
        return factory().also { recognizer = it }
    }

    override fun close() {
        recognizer?.close()
        recognizer = null
    }
}
