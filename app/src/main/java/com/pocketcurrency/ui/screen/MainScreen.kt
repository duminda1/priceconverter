package com.pocketcurrency.ui.screen

import android.graphics.Bitmap
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import android.text.format.DateUtils
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import com.pocketcurrency.ocr.PriceExtractor
import com.pocketcurrency.ocr.TextRecognizerHelper
import com.pocketcurrency.ui.component.PriceCard
import com.pocketcurrency.ui.Screen
import com.pocketcurrency.viewmodel.ConversionState
import com.pocketcurrency.viewmodel.MainViewModel
import com.pocketcurrency.domain.model.ServiceStatusType
import com.pocketcurrency.utils.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel, navController: NavHostController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val conversionState by viewModel.conversionState.collectAsState()
    val realtimeEnabled by viewModel.realtimeEnabled.collectAsState()
    val serviceReady by viewModel.serviceReady.collectAsState()
    val provider by viewModel.provider.collectAsState()
    val realtimeAvailable by viewModel.realtimeAvailable.collectAsState()
    val liveScanEnabled by viewModel.liveScanEnabled.collectAsState()
    val manualCurrencies by viewModel.manualCurrencies.collectAsState()
    val manualRates by viewModel.manualRates.collectAsState()
    val savedRates by viewModel.savedRates.collectAsState()
    val defaultFrom by viewModel.destinationCurrency.collectAsState()
    val defaultTo by viewModel.homeCurrency.collectAsState()
    val serviceStatus by viewModel.serviceStatus.collectAsState()
    val scanAmount by viewModel.scanAmount.collectAsState()
    val scanCurrency by viewModel.scanCurrency.collectAsState()
    val scanCurrencyConfident by viewModel.scanCurrencyConfident.collectAsState()

    var amountInput by remember { mutableStateOf("") }
    var fromCurrency by remember { mutableStateOf(defaultFrom) }
    var toCurrency by remember { mutableStateOf(defaultTo) }
    var hasCustomFrom by remember { mutableStateOf(false) }
    var hasCustomTo by remember { mutableStateOf(false) }
    var showRateInfo by remember { mutableStateOf(false) }
    val normalizedFrom = fromCurrency.trim().uppercase()
    val normalizedTo = toCurrency.trim().uppercase()
    val manualRateAvailable = manualRates.any {
        (it.from == normalizedFrom && it.to == normalizedTo) ||
            (it.from == normalizedTo && it.to == normalizedFrom && it.rate != 0.0)
    }
    val savedRateAvailable = savedRates.any {
        (it.from == normalizedFrom && it.to == normalizedTo) ||
            (it.from == normalizedTo && it.to == normalizedFrom && it.rate != 0.0)
    }
    val canUseRealtime = serviceReady && realtimeAvailable
    val canConvert = manualRateAvailable || savedRateAvailable || canUseRealtime
    val showRealtimeHelper = !realtimeEnabled || !canUseRealtime
    val amountValue = amountInput.toDoubleOrNull()
    val convertEnabled =
        amountValue != null && normalizedFrom.isNotBlank() && normalizedTo.isNotBlank()
    val showCameraHint = liveScanEnabled && scanAmount == null

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var cameraExecutor: ExecutorService? = null
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(scanAmount) {
        scanAmount?.let { amountInput = formatAmountInput(it) }
    }

    LaunchedEffect(scanCurrency, scanCurrencyConfident) {
        if (scanCurrencyConfident && !scanCurrency.isNullOrBlank()) {
            fromCurrency = scanCurrency!!
            hasCustomFrom = true
        }
    }

    LaunchedEffect(defaultFrom) {
        if (!hasCustomFrom) {
            fromCurrency = defaultFrom
        }
    }

    LaunchedEffect(defaultTo) {
        if (!hasCustomTo) {
            toCurrency = defaultTo
        }
    }

    LaunchedEffect(Unit) {
        viewModel.refreshSettings()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshSettings()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val gradient = Brush.verticalGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            MaterialTheme.colorScheme.background
        )
    )
    // Soft reassurance accent for offline/saved cues.
    val reassuranceColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.9f)

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text("PocketCurrency") },
                actions = {
                    TextButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { padding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient)
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            val cameraHeight = maxHeight * 0.25f
            val bottomHeight = maxHeight - cameraHeight

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cameraHeight),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    if (liveScanEnabled) {
                        Box(
                            modifier = Modifier.fillMaxSize()
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
                                                val detected = priceExtractor.extract(text)
                                                if (detected != null) {
                                                    viewModel.onScanResult(
                                                        amount = detected.amount,
                                                        currencyCode = detected.currencyCode,
                                                        isConfident = detected.isConfident
                                                    )
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

                            if (showCameraHint) {
                                CameraHintOverlay(
                                    text = "Looking for prices..."
                                )
                            }

                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(12.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(16.dp),
                                tonalElevation = 2.dp
                            ) {
                                Text(
                                    text = "Live scan",
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            if (scanCurrencyConfident && !scanCurrency.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = "Detected ${scanCurrency}",
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text("Live scan is off", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Enable it from the toggle below or in Settings.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(bottomHeight),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 180.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Manual entry", style = MaterialTheme.typography.titleMedium)

                            OutlinedTextField(
                                value = amountInput,
                                onValueChange = { value ->
                                    amountInput = filterAmountInput(value)
                                },
                                label = { Text("Amount") },
                                placeholder = { Text("e.g. 18.50") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CurrencyInputDropdown(
                                    label = "From",
                                    value = fromCurrency,
                                    placeholder = "USD",
                                    options = manualCurrencies,
                                    onValueChange = {
                                        fromCurrency = it
                                        hasCustomFrom = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )

                                IconButton(
                                    onClick = {
                                        val temp = fromCurrency
                                        fromCurrency = toCurrency
                                        toCurrency = temp
                                        hasCustomFrom = true
                                        hasCustomTo = true
                                    },
                                    modifier = Modifier.size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.SwapHoriz,
                                        contentDescription = "Swap currencies"
                                    )
                                }

                                CurrencyInputDropdown(
                                    label = "To",
                                    value = toCurrency,
                                    placeholder = "AUD",
                                    options = manualCurrencies,
                                    onValueChange = {
                                        toCurrency = it
                                        hasCustomTo = true
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Button(
                                onClick = {
                                    amountValue?.let { viewModel.convertPrice(it, fromCurrency, toCurrency) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .semantics { contentDescription = "Convert" },
                                shape = RoundedCornerShape(14.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                enabled = convertEnabled
                            ) {
                                Text("Convert")
                            }
                            if (!canConvert) {
                                val noRatesMessage = if (provider == Constants.PROVIDER_FRANKFURTER) {
                                    "No rates available. Refresh a saved pair or add an offline rate."
                                } else {
                                    "No rates available. Add an offline rate or API key."
                                }
                                Text(
                                    noRatesMessage,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 110.dp)
                    ) {
                        when (conversionState) {
                            is ConversionState.Idle -> {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                                ) {
                                    Text(
                                        "Waiting for a price to convert.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            is ConversionState.Loading -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text("Converting...", style = MaterialTheme.typography.titleMedium)
                                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            is ConversionState.Success ->
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    serviceStatus?.let { status ->
                                        val statusLabel = serviceStatusLabel(status.type)
                                        val relativeUpdated = formatRelativeUpdated(status.lastUpdatedMillis)
                                        val labelColor =
                                            if (status.type == ServiceStatusType.LIVE) {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            } else {
                                                reassuranceColor
                                            }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = statusLabel,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = labelColor
                                            )
                                            Text(
                                                text = "· Updated $relativeUpdated",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = reassuranceColor
                                            )
                                            IconButton(
                                                onClick = { showRateInfo = true },
                                                modifier = Modifier.size(48.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = "Rate source information",
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    PriceCard(
                                        (conversionState as ConversionState.Success).result,
                                        modifier = Modifier.offset(y = (-2).dp)
                                    )
                                    if (showRateInfo) {
                                        AlertDialog(
                                            onDismissRequest = { showRateInfo = false },
                                            confirmButton = {
                                                TextButton(onClick = { showRateInfo = false }) {
                                                    Text("Got it")
                                                }
                                            },
                                            text = {
                                                Text(
                                                    "PocketCurrency keeps working even without internet using saved rates."
                                                )
                                            }
                                        )
                                    }
                                }
                            is ConversionState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                ) {
                                    Text(
                                        (conversionState as ConversionState.Error).message,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(12.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 44.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Realtime",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        modifier = Modifier.scale(0.85f),
                                        checked = realtimeEnabled,
                                        onCheckedChange = { viewModel.setRealtimeEnabled(it) },
                                        enabled = canUseRealtime
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        "Scan",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Switch(
                                        modifier = Modifier.scale(0.85f),
                                        checked = liveScanEnabled,
                                        onCheckedChange = { viewModel.setLiveScanEnabled(it) }
                                    )
                                }
                            }
                            if (showRealtimeHelper) {
                                Text(
                                    "Realtime updates require internet access and a supported rate service.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(top = 6.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CameraHintOverlay(
    text: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun CurrencyInputDropdown(
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = options

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.uppercase()) },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .semantics { contentDescription = "$label currency selector" }
                .onFocusChanged {
                    if (it.isFocused && options.isNotEmpty()) {
                        expanded = true
                    }
                },
            shape = RoundedCornerShape(14.dp),
            trailingIcon = {
                if (options.isNotEmpty()) {
                    IconButton(onClick = { expanded = true }) {
                        Icon(
                            imageVector = Icons.Filled.ArrowDropDown,
                            contentDescription = "Show options"
                        )
                    }
                }
            }
        )
        DropdownMenu(
            expanded = expanded && filtered.isNotEmpty(),
            onDismissRequest = { expanded = false }
        ) {
            filtered.forEach { item ->
                DropdownMenuItem(
                    text = { Text(item) },
                    onClick = {
                        onValueChange(item)
                        expanded = false
                    }
                )
            }
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

private fun filterAmountInput(input: String): String {
    val filtered = input.filter { it.isDigit() || it == '.' }
    val firstDotIndex = filtered.indexOf('.')
    if (firstDotIndex == -1) {
        return filtered
    }
    val beforeDot = filtered.substring(0, firstDotIndex + 1)
    val afterDot = filtered.substring(firstDotIndex + 1).replace(".", "")
    return beforeDot + afterDot
}

private fun formatAmountInput(amount: Double): String {
    val symbols = DecimalFormatSymbols(Locale.US)
    val formatter = DecimalFormat("0.##", symbols)
    return formatter.format(amount)
}

private fun serviceStatusLabel(type: ServiceStatusType): String {
    return when (type) {
        ServiceStatusType.LIVE -> "Live updates"
        ServiceStatusType.SAVED -> "Saved for offline use"
        ServiceStatusType.MANUAL -> "Works offline"
    }
}

private fun formatRelativeUpdated(lastUpdatedMillis: Long): CharSequence {
    return DateUtils.getRelativeTimeSpanString(
        lastUpdatedMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
}
