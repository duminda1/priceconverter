package com.pocketcurrency.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Info
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.pocketcurrency.R
import com.pocketcurrency.ocr.LiveScanCoordinator
import com.pocketcurrency.ui.component.PriceCard
import com.pocketcurrency.ui.Screen
import com.pocketcurrency.viewmodel.ConversionState
import com.pocketcurrency.viewmodel.MainViewModel
import com.pocketcurrency.viewmodel.MainSettingsViewModel
import com.pocketcurrency.viewmodel.ScanViewModel
import com.pocketcurrency.domain.model.ServiceStatusType
import com.pocketcurrency.utils.Constants
import com.pocketcurrency.util.AmountInputFormatter
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "MainScreen"
private const val OCR_THROTTLE_MS = 400L

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    conversionViewModel: MainViewModel,
    settingsViewModel: MainSettingsViewModel,
    scanViewModel: ScanViewModel,
    navController: NavHostController
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()
    val cameraPermission = Manifest.permission.CAMERA
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                cameraPermission
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraPermissionUiState by rememberSaveable { mutableStateOf(CameraPermissionUiState.Off) }

    val conversionState by conversionViewModel.conversionState.collectAsState()
    val serviceStatus by conversionViewModel.serviceStatus.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val scanAmount by scanViewModel.scanAmount.collectAsState()
    val scanCurrency by scanViewModel.scanCurrency.collectAsState()
    val scanCurrencyConfident by scanViewModel.scanCurrencyConfident.collectAsState()

    val realtimeEnabled = settingsState.realtimeEnabled
    val serviceReady = settingsState.serviceReady
    val provider = settingsState.provider
    val realtimeAvailable = settingsState.realtimeAvailable
    val liveScanEnabled = settingsState.liveScanEnabled
    val manualCurrencies = settingsState.manualCurrencies
    val manualRates = settingsState.manualRates
    val savedRates = settingsState.savedRates
    val defaultFrom = settingsState.destinationCurrency
    val defaultTo = settingsState.homeCurrency

    var amountInput by rememberSaveable { mutableStateOf("") }
    var fromCurrency by rememberSaveable { mutableStateOf(defaultFrom) }
    var toCurrency by rememberSaveable { mutableStateOf(defaultTo) }
    var hasCustomFrom by rememberSaveable { mutableStateOf(false) }
    var hasCustomTo by rememberSaveable { mutableStateOf(false) }
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
    val amountValue = AmountInputFormatter.parseInput(amountInput)
    val convertEnabled =
        amountValue != null && normalizedFrom.isNotBlank() && normalizedTo.isNotBlank()
    val shouldStartCamera = liveScanEnabled && hasCameraPermission
    val showCameraHint = shouldStartCamera && scanAmount == null
    val convertLabel = stringResource(R.string.action_convert)
    val fromLabel = stringResource(R.string.main_currency_from_label)
    val toLabel = stringResource(R.string.main_currency_to_label)
    val fromPlaceholder = stringResource(R.string.main_currency_from_placeholder)
    val toPlaceholder = stringResource(R.string.main_currency_to_placeholder)

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) { androidx.camera.view.PreviewView(context) }
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var cameraExecutor by remember { mutableStateOf<ExecutorService?>(null) }
    val liveScanCoordinator = remember { LiveScanCoordinator(OCR_THROTTLE_MS) }

    val requestCameraPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, cameraPermission)
        }
        hasCameraPermission = isGranted
        cameraPermissionUiState =
            cameraPermissionUiStateForResult(isGranted, shouldShowRationale)
        settingsViewModel.setLiveScanEnabled(isGranted)
    }

    DisposableEffect(shouldStartCamera, lifecycleOwner, previewView) {
        var disposed = false

        if (shouldStartCamera) {
            val executor = Executors.newSingleThreadExecutor()
            cameraExecutor = executor

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                if (disposed) {
                    cameraProvider.unbindAll()
                    executor.shutdown()
                    return@addListener
                }

                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                imageAnalysis = analysis

                analysis.setAnalyzer(
                    executor,
                    object : ImageAnalysis.Analyzer {
                        override fun analyze(imageProxy: ImageProxy) {
                            if (disposed) {
                                imageProxy.close()
                                return
                            }
                            liveScanCoordinator.handleImageProxy(
                                imageProxy = imageProxy,
                                scope = scope,
                                onDetected = { detected ->
                                    scanViewModel.onScanResult(
                                        amount = detected.amount,
                                        currencyCode = detected.currencyCode,
                                        isConfident = detected.isConfident
                                    )
                                },
                                onError = { e ->
                                    Log.e(TAG, "Failed to recognize live scan text", e)
                                }
                            )
                        }
                    }
                )

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        analysis
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to bind camera for live scan", e)
                }
            }, mainExecutor)
        } else {
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
            }, mainExecutor)
        }

        onDispose {
            disposed = true
            imageAnalysis?.clearAnalyzer()
            imageAnalysis = null
            cameraExecutor?.shutdown()
            cameraExecutor = null
            liveScanCoordinator.close()
            cameraProviderFuture.addListener({
                cameraProviderFuture.get().unbindAll()
            }, mainExecutor)
        }
    }

    LaunchedEffect(scanAmount) {
        scanAmount?.let { amountInput = AmountInputFormatter.formatAmount(it) }
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
        settingsViewModel.refreshState()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                settingsViewModel.refreshState()
                val granted = ContextCompat.checkSelfPermission(
                    context,
                    cameraPermission
                ) == PackageManager.PERMISSION_GRANTED
                hasCameraPermission = granted
                if (granted && cameraPermissionUiState != CameraPermissionUiState.Off) {
                    cameraPermissionUiState = CameraPermissionUiState.Off
                }
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
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    TextButton(
                        onClick = { navController.navigate(Screen.Settings.route) },
                        modifier = Modifier.heightIn(min = 48.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text(stringResource(R.string.action_settings))
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
            val scrollState = rememberScrollState()
            // Use a wider aspect ratio in landscape to reduce vertical dominance.
            val isLandscape = maxWidth > maxHeight
            val isTablet = maxWidth >= 600.dp
            val useSideBySide = isTablet || isLandscape
            val cameraAspectRatio = if (isLandscape) 16f / 9f else 4f / 3f
            // Split on tablets (and wide landscapes) to use horizontal space effectively.
            val cameraWeight = when {
                isTablet -> 1.1f
                isLandscape -> 0.95f
                else -> 1f
            }
            val contentWeight = when {
                isTablet -> 0.9f
                isLandscape -> 1.05f
                else -> 1f
            }
            val enableScroll = !useSideBySide || maxHeight < 520.dp
            val anchorControls = useSideBySide && !enableScroll
            val contentScrollModifier = if (enableScroll) {
                Modifier.verticalScroll(scrollState)
            } else {
                Modifier
            }
            val permissionCardState = resolveCameraPermissionCardState(
                hasPermission = hasCameraPermission,
                liveScanEnabled = liveScanEnabled,
                uiState = cameraPermissionUiState
            )

            val cameraCard: @Composable (Modifier) -> Unit = { modifier ->
                Card(
                    modifier = modifier,
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    if (shouldStartCamera) {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { previewView }
                            )

                            if (showCameraHint) {
                                CameraHintOverlay(
                                    text = stringResource(R.string.main_camera_hint)
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
                                    text = stringResource(R.string.main_live_scan_label),
                                    style = MaterialTheme.typography.labelLarge,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            val detectedCurrency = scanCurrency
                            if (scanCurrencyConfident && !detectedCurrency.isNullOrBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.main_detected_currency,
                                            detectedCurrency
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (permissionCardState.show &&
                        permissionCardState.state != CameraPermissionUiState.Off
                    ) {
                        val promptSpec = when (permissionCardState.state) {
                            CameraPermissionUiState.Rationale -> {
                                CameraPermissionPromptSpec(
                                    title = stringResource(R.string.main_camera_permission_title),
                                    message = stringResource(R.string.main_camera_permission_body),
                                    primaryActionLabel = stringResource(R.string.action_allow_camera),
                                    onPrimaryAction = {
                                        requestCameraPermission.launch(cameraPermission)
                                    }
                                )
                            }
                            CameraPermissionUiState.Denied -> {
                                CameraPermissionPromptSpec(
                                    title = stringResource(R.string.main_camera_permission_denied_title),
                                    message = stringResource(R.string.main_camera_permission_denied_body),
                                    primaryActionLabel = stringResource(R.string.action_try_again),
                                    onPrimaryAction = {
                                        requestCameraPermission.launch(cameraPermission)
                                    }
                                )
                            }
                            CameraPermissionUiState.PermanentlyDenied -> {
                                CameraPermissionPromptSpec(
                                    title = stringResource(R.string.main_camera_permission_blocked_title),
                                    message = stringResource(R.string.main_camera_permission_blocked_body),
                                    primaryActionLabel = stringResource(R.string.action_open_settings),
                                    onPrimaryAction = {
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                                        ).apply {
                                            data = Uri.fromParts("package", context.packageName, null)
                                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        }
                                        context.startActivity(intent)
                                    }
                                )
                            }
                            CameraPermissionUiState.Off -> {
                                CameraPermissionPromptSpec(
                                    title = "",
                                    message = "",
                                    primaryActionLabel = "",
                                    onPrimaryAction = {}
                                )
                            }
                        }
                        CameraPermissionPrompt(
                            title = promptSpec.title,
                            message = promptSpec.message,
                            primaryActionLabel = promptSpec.primaryActionLabel,
                            onPrimaryAction = promptSpec.onPrimaryAction,
                            secondaryActionLabel = stringResource(R.string.action_not_now),
                            onSecondaryAction = {
                                cameraPermissionUiState = CameraPermissionUiState.Off
                                if (liveScanEnabled) {
                                    settingsViewModel.setLiveScanEnabled(false)
                                }
                            }
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.main_live_scan_off_title),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                stringResource(R.string.main_live_scan_off_subtitle),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            val contentColumn: @Composable (Modifier, Boolean) -> Unit = { modifier, anchorControls ->
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val manualEntryCard: @Composable () -> Unit = {
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
                                Text(
                                    stringResource(R.string.main_manual_entry_title),
                                    style = MaterialTheme.typography.titleMedium
                                )

                                val amountFieldMinHeight = 56.dp
                                val currencyFieldMinHeight = amountFieldMinHeight * 0.9f

                                OutlinedTextField(
                                    value = amountInput,
                                    onValueChange = { value ->
                                        amountInput = AmountInputFormatter.filterInput(value)
                                    },
                                    label = { Text(stringResource(R.string.main_amount_label)) },
                                    placeholder = { Text(stringResource(R.string.main_amount_placeholder)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = amountFieldMinHeight),
                                    shape = RoundedCornerShape(14.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CurrencyInputDropdown(
                                        modifier = Modifier.weight(1f),
                                        label = fromLabel,
                                        value = fromCurrency,
                                        placeholder = fromPlaceholder,
                                        options = manualCurrencies,
                                        onValueChange = {
                                            fromCurrency = it
                                            hasCustomFrom = true
                                        },
                                        minHeight = currencyFieldMinHeight
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
                                            contentDescription = stringResource(
                                                R.string.content_swap_currencies
                                            )
                                        )
                                    }

                                    CurrencyInputDropdown(
                                        modifier = Modifier.weight(1f),
                                        label = toLabel,
                                        value = toCurrency,
                                        placeholder = toPlaceholder,
                                        options = manualCurrencies,
                                        onValueChange = {
                                            toCurrency = it
                                            hasCustomTo = true
                                        },
                                        minHeight = currencyFieldMinHeight
                                    )
                                }

                                Button(
                                    onClick = {
                                        amountValue?.let {
                                            conversionViewModel.convertPrice(
                                                it,
                                                fromCurrency,
                                                toCurrency,
                                                realtimeEnabled
                                            )
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .semantics { contentDescription = convertLabel },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ),
                                    enabled = convertEnabled
                                ) {
                                    Text(convertLabel)
                                }
                                if (!canConvert) {
                                    val noRatesMessage = if (provider == Constants.PROVIDER_FRANKFURTER) {
                                        stringResource(R.string.main_no_rates_saved_pair)
                                    } else {
                                        stringResource(R.string.main_no_rates_add_offline_or_api)
                                    }
                                    Text(
                                        noRatesMessage,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    val conversionCard: @Composable () -> Unit = {
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
                                            stringResource(R.string.main_waiting_price),
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
                                            Text(
                                                stringResource(R.string.main_converting),
                                                style = MaterialTheme.typography.titleMedium
                                            )
                                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                                        }
                                    }
                                }
                                is ConversionState.Success ->
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(0.5.dp)
                                    ) {
                                        serviceStatus?.let { status ->
                                            val statusLabel = serviceStatusLabel(status.type)
                                            val relativeUpdated =
                                                formatRelativeUpdated(status.lastUpdatedAtMillis)
                                            val labelColor =
                                                if (status.type == ServiceStatusType.LIVE) {
                                                    MaterialTheme.colorScheme.onSurfaceVariant
                                                } else {
                                                    reassuranceColor
                                                }
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = statusLabel,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = labelColor
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.main_status_updated,
                                                        relativeUpdated
                                                    ),
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = reassuranceColor
                                                )
                                                if (status.isStale) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.main_status_stale
                                                        ),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.error
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { showRateInfo = true },
                                                    modifier = Modifier.size(42.dp)
                                                ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Info,
                                                    contentDescription = stringResource(
                                                        R.string.content_rate_source_info
                                                    ),
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                        }
                                        PriceCard(
                                            modifier = Modifier.offset(y = (-1).dp),
                                            result = (conversionState as ConversionState.Success).result
                                        )
                                        if (showRateInfo) {
                                            AlertDialog(
                                                onDismissRequest = { showRateInfo = false },
                                                confirmButton = {
                                                    TextButton(onClick = { showRateInfo = false }) {
                                                        Text(stringResource(R.string.action_got_it))
                                                    }
                                                },
                                                text = {
                                                    Column(
                                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                                    ) {
                                                        Text(
                                                            stringResource(R.string.main_rate_info_body)
                                                        )
                                                        if (serviceStatus?.isStale == true) {
                                                            Text(
                                                                stringResource(
                                                                    R.string.main_rate_info_stale
                                                                )
                                                            )
                                                        }
                                                    }
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
                    }

                    val controlsCard: @Composable () -> Unit = {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 44.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
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
                                            stringResource(R.string.main_realtime_label),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            modifier = Modifier.scale(0.85f),
                                            checked = realtimeEnabled,
                                            onCheckedChange = { settingsViewModel.setRealtimeEnabled(it) },
                                            enabled = canUseRealtime
                                        )
                                    }
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            stringResource(R.string.main_scan_label),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Switch(
                                            modifier = Modifier.scale(0.85f),
                                            checked = liveScanEnabled,
                                            onCheckedChange = { enabled ->
                                                if (enabled) {
                                                    if (hasCameraPermission) {
                                                        settingsViewModel.setLiveScanEnabled(true)
                                                        cameraPermissionUiState =
                                                            CameraPermissionUiState.Off
                                                    } else {
                                                        settingsViewModel.setLiveScanEnabled(false)
                                                        cameraPermissionUiState =
                                                            nextCameraPermissionUiStateOnEnableAttempt(
                                                                cameraPermissionUiState
                                                            )
                                                    }
                                                } else {
                                                    settingsViewModel.setLiveScanEnabled(false)
                                                    cameraPermissionUiState = CameraPermissionUiState.Off
                                                }
                                            }
                                        )
                                    }
                                }
                                if (showRealtimeHelper) {
                                    Text(
                                        stringResource(R.string.main_realtime_helper),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (anchorControls) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            manualEntryCard()
                            conversionCard()
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        controlsCard()
                    } else {
                        manualEntryCard()
                        conversionCard()
                        controlsCard()
                    }
                }
            }

            if (useSideBySide) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    cameraCard(
                        Modifier
                            .weight(cameraWeight)
                            .aspectRatio(cameraAspectRatio)
                    )
                    contentColumn(
                        Modifier
                            .weight(contentWeight)
                            .then(if (anchorControls) Modifier.fillMaxHeight() else Modifier)
                            .then(contentScrollModifier),
                        anchorControls
                    )
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    cameraCard(
                        Modifier
                            .fillMaxWidth()
                            // Responsive camera sizing without fixed heights.
                            .aspectRatio(cameraAspectRatio)
                    )
                    contentColumn(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            // Scrollable details area for small screens.
                            .then(contentScrollModifier),
                        false
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraHintOverlay(
    modifier: Modifier = Modifier,
    text: String
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

private data class CameraPermissionPromptSpec(
    val title: String,
    val message: String,
    val primaryActionLabel: String,
    val onPrimaryAction: () -> Unit
)

@Composable
private fun CameraPermissionPrompt(
    title: String,
    message: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    secondaryActionLabel: String,
    onSecondaryAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onSecondaryAction,
                modifier = Modifier.weight(1f)
            ) {
                Text(secondaryActionLabel)
            }
            Button(
                onClick = onPrimaryAction,
                modifier = Modifier.weight(1f)
            ) {
                Text(primaryActionLabel)
            }
        }
    }
}

@Composable
private fun CurrencyInputDropdown(
    modifier: Modifier = Modifier,
    label: String,
    value: String,
    placeholder: String,
    options: List<String>,
    onValueChange: (String) -> Unit,
    minHeight: Dp = 48.dp
) {
    var expanded by remember { mutableStateOf(false) }
    val filtered = options
    val selectorDescription = stringResource(
        R.string.content_currency_selector_description,
        label
    )

    Box(modifier = modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it.uppercase()) },
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .semantics { contentDescription = selectorDescription }
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
                            contentDescription = stringResource(
                                R.string.content_show_options
                            )
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

@Composable
private fun serviceStatusLabel(type: ServiceStatusType): String {
    return when (type) {
        ServiceStatusType.LIVE -> stringResource(R.string.main_status_live)
        ServiceStatusType.SAVED -> stringResource(R.string.main_status_saved)
        ServiceStatusType.MANUAL -> stringResource(R.string.main_status_manual)
    }
}

private fun formatRelativeUpdated(lastUpdatedMillis: Long): CharSequence {
    return DateUtils.getRelativeTimeSpanString(
        lastUpdatedMillis,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    )
}
