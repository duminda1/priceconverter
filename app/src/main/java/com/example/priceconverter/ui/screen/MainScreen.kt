package com.example.priceconverter.ui.screen

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import com.example.priceconverter.ocr.PriceExtractor
import com.example.priceconverter.ocr.TextRecognizerHelper
import com.example.priceconverter.ui.component.PriceCard
import com.example.priceconverter.ui.Screen
import com.example.priceconverter.viewmodel.ConversionState
import com.example.priceconverter.viewmodel.MainViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val conversionState by viewModel.conversionState.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("AUD") }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraExecutor: ExecutorService? = null

    Column(modifier = Modifier
        .fillMaxSize()
        .padding(16.dp)
    ) {
        // --- Camera Preview ---
        Box(modifier = Modifier
            .fillMaxWidth()
            .height(250.dp)
        ) {
            AndroidView(factory = { ctx ->
                val previewView = androidx.camera.view.PreviewView(ctx)
                cameraExecutor = Executors.newSingleThreadExecutor()
                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()

                    val textRecognizer = TextRecognizerHelper(context)
                    val priceExtractor = PriceExtractor()

                    imageAnalysis.setAnalyzer(cameraExecutor!!) { imageProxy ->
                        val bitmap = imageProxy.toBitmap()
                            scope.launch(Dispatchers.Default) {
                                try {
                                    val text = textRecognizer.recognizeText(bitmap)
                                    val price = priceExtractor.extractPrice(text)
                                    if (price != null) {
                                        amountInput = price.toString()
                                    }
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                } finally {
                                    imageProxy.close()
                                }
                            }
                    }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            context as androidx.lifecycle.LifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            })
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- Manual input (fallback) ---
        OutlinedTextField(
            value = amountInput,
            onValueChange = { amountInput = it },
            label = { Text("Amount") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row {
            OutlinedTextField(
                value = fromCurrency,
                onValueChange = { fromCurrency = it },
                label = { Text("From") },
                modifier = Modifier.weight(1f)
            )

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = toCurrency,
                onValueChange = { toCurrency = it },
                label = { Text("To") },
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val amount = amountInput.toDoubleOrNull()
            if (amount != null) {
                viewModel.convertPrice(amount, fromCurrency, toCurrency)
            }
        }) {
            Text("Convert")
        }

        Button(onClick = { navController.navigate(Screen.Settings.route) }) {
            Text("Settings")
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (conversionState) {
            is ConversionState.Idle -> {}
            is ConversionState.Loading -> Text("Loading...")
            is ConversionState.Success -> PriceCard((conversionState as ConversionState.Success).result)
            is ConversionState.Error -> Text("Error: ${(conversionState as ConversionState.Error).message}")
        }
    }
}

// --- Helper extension to convert ImageProxy to Bitmap ---
fun ImageProxy.toBitmap(): Bitmap? {
    val planeProxy = this.planes.firstOrNull() ?: return null
    val buffer = planeProxy.buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}

