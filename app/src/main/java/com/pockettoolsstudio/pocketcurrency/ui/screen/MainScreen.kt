package com.pockettoolsstudio.pocketcurrency.ui.screen

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.text.format.DateUtils
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
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
import androidx.compose.ui.text.font.FontWeight
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
import com.pockettoolsstudio.pocketcurrency.R
import com.pockettoolsstudio.pocketcurrency.ocr.CurrencySource
import com.pockettoolsstudio.pocketcurrency.ui.component.PriceCard
import com.pockettoolsstudio.pocketcurrency.ui.Screen
import com.pockettoolsstudio.pocketcurrency.viewmodel.ConversionState
import com.pockettoolsstudio.pocketcurrency.viewmodel.MainViewModel
import com.pockettoolsstudio.pocketcurrency.viewmodel.MainSettingsViewModel
import com.pockettoolsstudio.pocketcurrency.viewmodel.OcrReadiness
import com.pockettoolsstudio.pocketcurrency.viewmodel.OcrUnavailableReason
import com.pockettoolsstudio.pocketcurrency.viewmodel.ScanViewModel
import com.pockettoolsstudio.pocketcurrency.viewmodel.LiveScanUiState
import com.pockettoolsstudio.pocketcurrency.domain.model.ServiceStatusType
import com.pockettoolsstudio.pocketcurrency.util.Constants
import com.pockettoolsstudio.pocketcurrency.util.AmountInputFormatter

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
    val scanCurrencySource by scanViewModel.scanCurrencySource.collectAsState()
    val ocrReadiness by scanViewModel.ocrReadiness.collectAsState()
    val scanUiState by scanViewModel.scanUiState.collectAsState()

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
    val amountInputInvalid = amountInput.isNotBlank() && amountValue == null
    val convertEnabled =
        amountValue != null && normalizedFrom.isNotBlank() && normalizedTo.isNotBlank()
    val ocrReady = ocrReadiness is OcrReadiness.Ready
    val isScanning = scanUiState is LiveScanUiState.Scanning
    val scanResult = scanUiState as? LiveScanUiState.Result
    val shouldStartCamera = isScanning && liveScanEnabled && hasCameraPermission && ocrReady
    val showCameraHint = shouldStartCamera && scanAmount == null
    val convertLabel = stringResource(R.string.action_convert)
    val fromLabel = stringResource(R.string.main_currency_from_label)
    val toLabel = stringResource(R.string.main_currency_to_label)
    val fromPlaceholder = stringResource(R.string.main_currency_from_placeholder)
    val toPlaceholder = stringResource(R.string.main_currency_to_placeholder)
    val realtimeHint = stringResource(R.string.main_realtime_hint)
    val scanHint = stringResource(R.string.main_scan_hint)
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) { PreviewView(context) }

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
        if (!isGranted) {
            scanViewModel.stopLiveScan()
        }
    }

    LaunchedEffect(liveScanEnabled, hasCameraPermission) {
        scanViewModel.updateLiveScanAvailability(liveScanEnabled, hasCameraPermission)
    }

    LaunchedEffect(lifecycleOwner) {
        scanViewModel.bindLiveScanLifecycle(lifecycleOwner)
    }

    LaunchedEffect(scanAmount) {
        scanAmount?.let { amountInput = AmountInputFormatter.formatAmount(it) }
    }

    LaunchedEffect(scanCurrency, scanCurrencySource) {
        if (!hasCustomFrom && !scanCurrency.isNullOrBlank()) {
            fromCurrency = scanCurrency!!
        }
    }

    LaunchedEffect(normalizedFrom) {
        scanViewModel.setSelectedFromCurrency(
            normalizedFrom.takeIf { it.isNotBlank() }
        )
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
    val sectionHeaderStyle =
        MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
    val helperTextStyle = MaterialTheme.typography.bodySmall
    val helperTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
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
            val baseCameraAspectRatio = if (isLandscape) 16f / 9f else 4f / 3f
            val cameraHeightScale = if (useSideBySide) {
                1f
            } else if (shouldStartCamera) {
                0.4f
            } else {
                0.6f
            }
            val cameraAspectRatio = baseCameraAspectRatio / cameraHeightScale
            val cameraMinHeight = if (useSideBySide) {
                0.dp
            } else if (shouldStartCamera) {
                120.dp
            } else {
                160.dp
            }
            val cameraHeightModifier = if (useSideBySide) {
                Modifier
            } else {
                Modifier.heightIn(min = cameraMinHeight)
            }
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
                        DisposableEffect(previewView) {
                            scanViewModel.setLiveScanSurfaceProvider(previewView.surfaceProvider)
                            onDispose {
                                scanViewModel.setLiveScanSurfaceProvider(null)
                            }
                        }
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
                            val detectedSource = scanCurrencySource
                            if (!detectedCurrency.isNullOrBlank() && detectedSource != null) {
                                val isInferred = detectedSource != CurrencySource.OCR_EXPLICIT &&
                                    detectedSource != CurrencySource.OCR_SYMBOL
                                val detectedLabel = if (isInferred) {
                                    stringResource(
                                        R.string.main_detected_currency_inferred,
                                        detectedCurrency
                                    )
                                } else {
                                    stringResource(
                                        R.string.main_detected_currency,
                                        detectedCurrency
                                    )
                                }
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp),
                                    tonalElevation = 2.dp
                                ) {
                                    Text(
                                        text = detectedLabel,
                                        style = MaterialTheme.typography.labelLarge,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else if (scanResult != null) {
                        ScanResultCard(
                            amount = scanResult.amount,
                            currencyCode = scanResult.currencyCode,
                            onScanAgain = { scanViewModel.startLiveScan() },
                            onEnterManually = { scanViewModel.stopLiveScan() }
                        )
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
                                scanViewModel.stopLiveScan()
                            }
                        )
                    } else if (liveScanEnabled && hasCameraPermission) {
                        when (val readiness = ocrReadiness) {
                            OcrReadiness.Checking,
                            OcrReadiness.Unknown -> {
                                OcrStatusPrompt(
                                    message = stringResource(R.string.main_ocr_checking),
                                    showProgress = true
                                )
                            }
                            OcrReadiness.Installing -> {
                                OcrStatusPrompt(
                                    message = stringResource(R.string.main_ocr_downloading),
                                    showProgress = true
                                )
                            }
                            is OcrReadiness.Unavailable -> {
                                val message = when (readiness.reason) {
                                    OcrUnavailableReason.ModelNotDownloaded ->
                                        stringResource(R.string.main_ocr_unavailable_body)
                                    OcrUnavailableReason.PlayServicesMissing,
                                    OcrUnavailableReason.PlayServicesDisabled,
                                    OcrUnavailableReason.PlayServicesUpdateRequired,
                                    OcrUnavailableReason.PlayServicesUpdating ->
                                        stringResource(R.string.main_ocr_play_services_body)
                                    OcrUnavailableReason.Unknown ->
                                        stringResource(R.string.main_ocr_unavailable_retry)
                                }
                                val primaryLabel =
                                    if (readiness.reason == OcrUnavailableReason.ModelNotDownloaded) {
                                        stringResource(R.string.action_download_ocr_model)
                                    } else {
                                        stringResource(R.string.action_try_again)
                                    }
                                val onPrimaryAction =
                                    if (readiness.reason == OcrUnavailableReason.ModelNotDownloaded) {
                                        { scanViewModel.requestOcrModelDownload() }
                                    } else {
                                        { scanViewModel.refreshOcrReadiness() }
                                    }
                                OcrStatusPrompt(
                                    title = stringResource(R.string.main_ocr_unavailable_title),
                                    message = message,
                                    primaryActionLabel = primaryLabel,
                                    onPrimaryAction = onPrimaryAction,
                                    secondaryActionLabel = stringResource(R.string.action_not_now),
                                    onSecondaryAction = {
                                        settingsViewModel.setLiveScanEnabled(false)
                                        scanViewModel.stopLiveScan()
                                    }
                                )
                            }
                            OcrReadiness.Ready -> {
                                StartLiveScanCard(
                                    onStart = { scanViewModel.startLiveScan() }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            Text(
                                stringResource(R.string.main_live_scan_off_title),
                                style = sectionHeaderStyle
                            )
                            Text(
                                stringResource(R.string.main_live_scan_off_subtitle),
                                style = helperTextStyle,
                                color = helperTextColor
                            )
                        }
                    }
                }
            }

            val contentColumn: @Composable (Modifier, Boolean) -> Unit = { modifier, anchorControls ->
                Column(
                    modifier = modifier,
                    verticalArrangement = Arrangement.spacedBy(20.dp)
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
                                    style = sectionHeaderStyle
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
                                    isError = amountInputInvalid,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        errorBorderColor =
                                            MaterialTheme.colorScheme.error.copy(alpha = 0.85f)
                                    ),
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
                                        .heightIn(min = 44.dp)
                                        .semantics { contentDescription = convertLabel },
                                    shape = RoundedCornerShape(14.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        style = helperTextStyle,
                                        color = helperTextColor
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
                                            style = helperTextStyle,
                                            modifier = Modifier.padding(12.dp),
                                            color = helperTextColor
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
                                                style = sectionHeaderStyle
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
                                            val statusTextColor = helperTextColor
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text(
                                                    text = statusLabel,
                                                    style = helperTextStyle,
                                                    color = statusTextColor
                                                )
                                                Text(
                                                    text = stringResource(
                                                        R.string.main_status_updated,
                                                        relativeUpdated
                                                    ),
                                                    style = helperTextStyle,
                                                    color = statusTextColor
                                                )
                                                if (status.isStale) {
                                                    Text(
                                                        text = stringResource(
                                                            R.string.main_status_stale
                                                        ),
                                                        style = helperTextStyle,
                                                        color = helperTextColor
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
                                                            stringResource(R.string.main_rate_info_body),
                                                            style = helperTextStyle,
                                                            color = helperTextColor
                                                        )
                                                        if (serviceStatus?.isStale == true) {
                                                            Text(
                                                                stringResource(
                                                                    R.string.main_rate_info_stale
                                                                ),
                                                                style = helperTextStyle,
                                                                color = helperTextColor
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
                                            style = helperTextStyle,
                                            modifier = Modifier.padding(12.dp),
                                            color = helperTextColor
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
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(R.string.main_realtime_label),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Switch(
                                                modifier = Modifier.scale(0.85f),
                                                checked = realtimeEnabled,
                                                onCheckedChange = {
                                                    settingsViewModel.setRealtimeEnabled(it)
                                                },
                                                enabled = canUseRealtime
                                            )
                                        }
                                        Text(
                                            realtimeHint,
                                            style = helperTextStyle,
                                            color = helperTextColor
                                        )
                                    }
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                stringResource(R.string.main_scan_label),
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Switch(
                                                modifier = Modifier.scale(0.85f),
                                                checked = liveScanEnabled,
                                                onCheckedChange = { enabled ->
                                                    if (enabled) {
                                                        if (hasCameraPermission) {
                                                            settingsViewModel.setLiveScanEnabled(true)
                                                            cameraPermissionUiState =
                                                                CameraPermissionUiState.Off
                                                            scanViewModel.startLiveScan()
                                                        } else {
                                                            settingsViewModel.setLiveScanEnabled(false)
                                                            cameraPermissionUiState =
                                                                nextCameraPermissionUiStateOnEnableAttempt(
                                                                    cameraPermissionUiState
                                                                )
                                                            scanViewModel.startLiveScan()
                                                        }
                                                    } else {
                                                        settingsViewModel.setLiveScanEnabled(false)
                                                        cameraPermissionUiState =
                                                            CameraPermissionUiState.Off
                                                        scanViewModel.stopLiveScan()
                                                    }
                                                }
                                            )
                                        }
                                        Text(
                                            scanHint,
                                            style = helperTextStyle,
                                            color = helperTextColor
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
                                if (liveScanEnabled && hasCameraPermission) {
                                    val ocrStatusText = when (ocrReadiness) {
                                        OcrReadiness.Ready ->
                                            stringResource(R.string.main_ocr_ready)
                                        OcrReadiness.Checking,
                                        OcrReadiness.Unknown ->
                                            stringResource(R.string.main_ocr_checking)
                                        OcrReadiness.Installing ->
                                            stringResource(R.string.main_ocr_downloading)
                                        is OcrReadiness.Unavailable ->
                                            stringResource(R.string.main_ocr_unavailable_short)
                                    }
                                    val ocrStatusColor = helperTextColor
                                    Text(
                                        ocrStatusText,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = ocrStatusColor,
                                        modifier = Modifier.padding(top = 6.dp)
                                    )
                                }
                            }
                        }
                    }

                    if (anchorControls) {
                        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
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
                            .then(cameraHeightModifier)
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
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    cameraCard(
                        Modifier
                            .fillMaxWidth()
                            // Keep the camera assistive while preserving room for prompts.
                            .aspectRatio(cameraAspectRatio)
                            .then(cameraHeightModifier)
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
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
    }
}

@Composable
private fun StartLiveScanCard(
    onStart: () -> Unit,
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
            text = stringResource(R.string.main_live_scan_ready_title),
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.main_live_scan_ready_body),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(R.string.action_start_live_scan))
        }
    }
}

@Composable
private fun ScanResultCard(
    amount: Double?,
    currencyCode: String?,
    onScanAgain: () -> Unit,
    onEnterManually: () -> Unit,
    modifier: Modifier = Modifier
) {
    val normalizedCurrency = currencyCode?.trim()?.uppercase()
    val formattedAmount = amount?.let { AmountInputFormatter.formatAmount(it) }
    val resultText = when {
        formattedAmount == null -> stringResource(R.string.main_scan_result_empty)
        !normalizedCurrency.isNullOrBlank() -> stringResource(
            R.string.main_scan_result_amount_with_currency,
            formattedAmount,
            normalizedCurrency
        )
        else -> stringResource(R.string.main_scan_result_amount, formattedAmount)
    }
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = resultText,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onEnterManually,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_enter_manually))
            }
            Button(
                onClick = onScanAgain,
                modifier = Modifier.weight(1f)
            ) {
                Text(stringResource(R.string.action_scan_again))
            }
        }
    }
}

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
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
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
private fun OcrStatusPrompt(
    message: String,
    title: String? = null,
    primaryActionLabel: String? = null,
    onPrimaryAction: (() -> Unit)? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
    showProgress: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (showProgress) {
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (primaryActionLabel != null && onPrimaryAction != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (secondaryActionLabel != null && onSecondaryAction != null) {
                    OutlinedButton(
                        onClick = onSecondaryAction,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(secondaryActionLabel)
                    }
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
