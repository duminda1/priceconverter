package com.pocketcurrency.ocr

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class LiveScanCoordinator(
    throttleMs: Long,
    private val textRecognizerSession: TextRecognizerSession = TextRecognizerSession(),
    private val priceExtractor: PriceExtractor = PriceExtractor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable {

    private val ocrThrottle = OcrThrottle(throttleMs)

    @androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
    fun handleImageProxy(
        imageProxy: ImageProxy,
        scope: CoroutineScope,
        onDetected: (DetectedPrice) -> Unit,
        onError: (Throwable) -> Unit
    ) {
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        if (!ocrThrottle.tryStart()) {
            imageProxy.close()
            return
        }
        val inputImage = try {
            InputImage.fromMediaImage(
                mediaImage,
                imageProxy.imageInfo.rotationDegrees
            )
        } catch (e: Exception) {
            ocrThrottle.onAbort()
            imageProxy.close()
            return
        }

        val recognizer = textRecognizerSession.acquire()
        scope.launch(dispatcher) {
            try {
                val text = recognizer.recognizeText(inputImage)
                val detected = priceExtractor.extract(text)
                if (detected != null) {
                    onDetected(detected)
                }
            } catch (e: Exception) {
                onError(e)
            } finally {
                ocrThrottle.onComplete()
                imageProxy.close()
            }
        }
    }

    override fun close() {
        textRecognizerSession.close()
    }
}
