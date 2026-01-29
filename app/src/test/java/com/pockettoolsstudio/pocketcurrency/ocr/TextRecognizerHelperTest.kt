package com.pockettoolsstudio.pocketcurrency.ocr

import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognizer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class TextRecognizerHelperTest {

    @Test
    fun recognizeText_returnsTextFromRecognizer() = runTest {
        val textResult = mockk<Text>()
        every { textResult.text } returns "Sample text"
        every { textResult.textBlocks } returns emptyList()
        val recognizer = mockk<TextRecognizer>()
        val inputImage = mockk<InputImage>()

        every { recognizer.process(inputImage) } returns Tasks.forResult(textResult)

        val helper = TextRecognizerHelper(recognizer)

        val result = helper.recognizeText(inputImage)

        assertEquals("Sample text", result)
        verify(exactly = 1) { recognizer.process(inputImage) }
    }

    @Test
    fun formatRecognizedText_aggregatesTextBlocks() {
        val result = formatRecognizedText("Ignored", listOf("Total", "12.50"))

        assertEquals("Total\n12.50", result)
    }

    @Test
    fun close_forwardsToRecognizer() {
        val recognizer = mockk<TextRecognizer>(relaxed = true)
        val helper = TextRecognizerHelper(recognizer)

        helper.close()

        verify(exactly = 1) { recognizer.close() }
    }
}
