package com.pockettoolsstudio.pocketcurrency.ocr

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale

class LiveScanCoordinator(
    throttleMs: Long,
    private val textRecognizerSession: TextRecognizerSession = TextRecognizerSession(),
    private val priceExtractor: PriceExtractor = PriceExtractor(),
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default
) : AutoCloseable {

    private val ocrThrottle = OcrThrottle(throttleMs)
    private val priceStabilizer = OcrPriceStabilizer(priceExtractor)
    @Volatile
    private var currencyContext = CurrencyContext(
        selectedFromCurrency = null,
        locale = Locale.getDefault()
    )

    fun setSelectedFromCurrency(currencyCode: String?) {
        currencyContext = CurrencyContext(
            selectedFromCurrency = currencyCode
                ?.trim()
                ?.uppercase(Locale.ROOT)
                ?.takeIf { it.isNotBlank() },
            locale = Locale.getDefault()
        )
    }

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
                val detected = priceStabilizer.onFrame(text, currencyContext)
                if (detected != null) {
                    onDetected(detected)
                }
                ocrThrottle.onComplete()
            } catch (e: CancellationException) {
                ocrThrottle.onAbort()
            } catch (e: Exception) {
                onError(e)
                ocrThrottle.onComplete()
            } finally {
                imageProxy.close()
            }
        }
    }

    fun resetSession() {
        ocrThrottle.onAbort()
        priceStabilizer.reset()
    }

    override fun close() {
        priceStabilizer.reset()
        textRecognizerSession.close()
    }
}
