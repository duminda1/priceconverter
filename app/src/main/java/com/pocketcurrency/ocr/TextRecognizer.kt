package com.pocketcurrency.ocr

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class TextRecognizerHelper(
    private val recognizer: TextRecognizer = TextRecognition.getClient(
        TextRecognizerOptions.DEFAULT_OPTIONS
    )
) : AutoCloseable {

    /**
     * Recognize text from an InputImage.
     */
    suspend fun recognizeText(image: InputImage): String {
        val result = recognizer.process(image).await()
        return result.text
    }

    override fun close() {
        recognizer.close()
    }
}
