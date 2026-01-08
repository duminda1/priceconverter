package com.pocketcurrency.viewmodel

import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.moduleinstall.ModuleInstall
import com.google.android.gms.common.moduleinstall.ModuleInstallRequest
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.pocketcurrency.BuildConfig
import com.pocketcurrency.data.repository.ScanRepository
import com.pocketcurrency.domain.usecase.ProcessScanResultUseCase
import com.pocketcurrency.ocr.CurrencyConfidence
import com.pocketcurrency.ocr.CurrencySource
import com.pocketcurrency.ocr.DetectedPrice
import com.pocketcurrency.ocr.LiveScanController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scanRepository: ScanRepository,
    private val processScanResultUseCase: ProcessScanResultUseCase,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    val scanAmount: StateFlow<Double?> = scanRepository.scanAmount
    val scanCurrency: StateFlow<String?> = scanRepository.scanCurrency
    val scanCurrencyConfidence: StateFlow<CurrencyConfidence?> = scanRepository.scanCurrencyConfidence
    val scanCurrencySource: StateFlow<CurrencySource?> = scanRepository.scanCurrencySource

    private val _ocrReadiness = MutableStateFlow<OcrReadiness>(OcrReadiness.Unknown)
    val ocrReadiness: StateFlow<OcrReadiness> = _ocrReadiness

    private val _scanUiState = MutableStateFlow<LiveScanUiState>(LiveScanUiState.Idle)
    val scanUiState: StateFlow<LiveScanUiState> = _scanUiState

    private val onDetectedHandler: (DetectedPrice) -> Unit = { detected ->
        handleDetectedPrice(detected)
    }
    private val onErrorHandler: (Throwable) -> Unit = handler@{ error ->
        if (error is CancellationException) {
            return@handler
        }
        if (BuildConfig.DEBUG) {
            Log.e(TAG, "Failed to recognize live scan text", error)
        }
    }
    private var liveScanController: LiveScanControllerDelegate? = null
    private val liveScanControllerFactory: () -> LiveScanControllerDelegate = {
        LiveScanControllerDelegateImpl(
            context = appContext,
            onDetected = onDetectedHandler,
            onError = onErrorHandler
        )
    }
    private var ocrModelManager: OcrModelManager? = null
    private var liveScanEnabled = false
    private var hasCameraPermission = false
    private var pendingStart = false
    private var isCameraActive = false
    private var scanTimeoutJob: Job? = null

    fun onScanResult(
        amount: Double,
        currencyCode: String?,
        currencyConfidence: CurrencyConfidence,
        currencySource: CurrencySource
    ) {
        processScanResultUseCase.execute(
            amount,
            currencyCode,
            currencyConfidence,
            currencySource
        )
    }

    internal fun onDetectedPriceForTesting(detected: DetectedPrice) {
        handleDetectedPrice(detected)
    }

    internal fun setLiveScanControllerForTesting(controller: LiveScanControllerDelegate) {
        liveScanController = controller
    }

    internal fun setOcrModelManagerForTesting(manager: OcrModelManager) {
        ocrModelManager = manager
    }

    private fun liveScanController(): LiveScanControllerDelegate {
        return liveScanController ?: liveScanControllerFactory().also { liveScanController = it }
    }

    private fun ocrModelManager(): OcrModelManager {
        return ocrModelManager ?: PlayServicesOcrModelManager(appContext).also {
            ocrModelManager = it
        }
    }

    fun bindLiveScanLifecycle(lifecycleOwner: LifecycleOwner) {
        liveScanController().bindToLifecycle(lifecycleOwner)
    }

    fun setLiveScanSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        liveScanController().setSurfaceProvider(surfaceProvider)
    }

    fun updateLiveScanAvailability(enabled: Boolean, hasPermission: Boolean) {
        val wasEnabled = liveScanEnabled
        liveScanEnabled = enabled
        hasCameraPermission = hasPermission
        if (wasEnabled && !enabled) {
            stopLiveScan()
            return
        }
        if (enabled && pendingStart && _scanUiState.value !is LiveScanUiState.Scanning) {
            beginLiveScan()
        }
        updateLiveScanState()
        if (enabled && hasPermission) {
            refreshOcrReadiness()
        }
    }

    fun startLiveScan() {
        pendingStart = true
        scanRepository.clearScan()
        _scanUiState.value = LiveScanUiState.Idle
        if (liveScanEnabled) {
            beginLiveScan()
        } else {
            updateLiveScanState()
        }
    }

    fun stopLiveScan() {
        pendingStart = false
        _scanUiState.value = LiveScanUiState.Idle
        cancelScanTimeout()
        updateLiveScanState()
    }

    fun setSelectedFromCurrency(currencyCode: String?) {
        liveScanController().setSelectedFromCurrency(currencyCode)
    }

    fun refreshOcrReadiness() {
        val current = _ocrReadiness.value
        if (current is OcrReadiness.Ready ||
            current is OcrReadiness.Checking ||
            current is OcrReadiness.Installing
        ) {
            return
        }
        _ocrReadiness.value = OcrReadiness.Checking
        viewModelScope.launch {
            _ocrReadiness.value = ocrModelManager().checkAvailability()
            updateLiveScanState()
        }
    }

    fun requestOcrModelDownload() {
        val current = _ocrReadiness.value
        if (current is OcrReadiness.Installing ||
            current is OcrReadiness.Ready ||
            current is OcrReadiness.Checking
        ) {
            return
        }
        _ocrReadiness.value = OcrReadiness.Installing
        viewModelScope.launch {
            _ocrReadiness.value = ocrModelManager().installModel()
            updateLiveScanState()
        }
    }

    private fun handleDetectedPrice(detected: DetectedPrice) {
        if (_scanUiState.value !is LiveScanUiState.Scanning) {
            return
        }
        completeScan(
            LiveScanUiState.Result(
                amount = detected.amount,
                currencyCode = detected.currencyCode
            )
        )
        onScanResult(
            amount = detected.amount,
            currencyCode = detected.currencyCode,
            currencyConfidence = detected.currencyConfidence,
            currencySource = detected.currencySource
        )
    }

    private fun beginLiveScan() {
        if (_scanUiState.value is LiveScanUiState.Scanning) {
            return
        }
        _scanUiState.value = LiveScanUiState.Scanning
        updateLiveScanState()
    }

    private fun completeScan(result: LiveScanUiState.Result) {
        if (_scanUiState.value !is LiveScanUiState.Scanning) {
            return
        }
        pendingStart = false
        _scanUiState.value = result
        cancelScanTimeout()
        updateLiveScanState()
    }

    private fun updateLiveScanState() {
        val ready = _ocrReadiness.value is OcrReadiness.Ready
        val shouldRun = _scanUiState.value is LiveScanUiState.Scanning &&
            liveScanEnabled &&
            hasCameraPermission &&
            ready
        if (shouldRun && !isCameraActive) {
            startScanTimeout()
        } else if (!shouldRun && isCameraActive) {
            cancelScanTimeout()
        }
        isCameraActive = shouldRun
        liveScanController().setActive(shouldRun, hasCameraPermission)
    }

    private fun startScanTimeout() {
        cancelScanTimeout()
        scanTimeoutJob = viewModelScope.launch {
            delay(LIVE_SCAN_TIMEOUT_MS)
            if (_scanUiState.value is LiveScanUiState.Scanning) {
                completeScan(LiveScanUiState.Result(amount = null, currencyCode = null))
            }
        }
    }

    private fun cancelScanTimeout() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
    }

    override fun onCleared() {
        cancelScanTimeout()
        liveScanController?.close()
        super.onCleared()
    }
}

private const val TAG = "ScanViewModel"
// 4.5s timeout to balance OCR stability with battery/privacy.
internal const val LIVE_SCAN_TIMEOUT_MS = 4500L

sealed class OcrReadiness {
    object Unknown : OcrReadiness()
    object Checking : OcrReadiness()
    object Installing : OcrReadiness()
    object Ready : OcrReadiness()
    data class Unavailable(val reason: OcrUnavailableReason) : OcrReadiness()
}

enum class OcrUnavailableReason {
    PlayServicesMissing,
    PlayServicesDisabled,
    PlayServicesUpdateRequired,
    PlayServicesUpdating,
    ModelNotDownloaded,
    Unknown
}

sealed class LiveScanUiState {
    object Idle : LiveScanUiState()
    object Scanning : LiveScanUiState()
    data class Result(val amount: Double?, val currencyCode: String?) : LiveScanUiState()
}

internal interface LiveScanControllerDelegate : AutoCloseable {
    fun bindToLifecycle(owner: LifecycleOwner)
    fun setSurfaceProvider(provider: Preview.SurfaceProvider?)
    fun setActive(enabled: Boolean, permissionGranted: Boolean)
    fun setSelectedFromCurrency(currencyCode: String?)
}

internal class LiveScanControllerDelegateImpl(
    context: Context,
    private val onDetected: (DetectedPrice) -> Unit,
    private val onError: (Throwable) -> Unit
) : LiveScanControllerDelegate {
    private val controller = LiveScanController(
        context = context,
        onDetected = onDetected,
        onError = onError
    )

    override fun bindToLifecycle(owner: LifecycleOwner) {
        controller.bindToLifecycle(owner)
    }

    override fun setSurfaceProvider(provider: Preview.SurfaceProvider?) {
        controller.setSurfaceProvider(provider)
    }

    override fun setActive(enabled: Boolean, permissionGranted: Boolean) {
        controller.setActive(enabled, permissionGranted)
    }

    override fun setSelectedFromCurrency(currencyCode: String?) {
        controller.setSelectedFromCurrency(currencyCode)
    }

    override fun close() {
        controller.close()
    }
}

internal interface OcrModelManager {
    suspend fun checkAvailability(): OcrReadiness
    suspend fun installModel(): OcrReadiness
}

internal class PlayServicesOcrModelManager(
    private val appContext: Context
) : OcrModelManager {
    private val moduleInstallClient = ModuleInstall.getClient(appContext)
    private val googleApiAvailability = GoogleApiAvailability.getInstance()

    override suspend fun checkAvailability(): OcrReadiness {
        val playServicesStatus =
            googleApiAvailability.isGooglePlayServicesAvailable(appContext)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            return OcrReadiness.Unavailable(playServicesUnavailableReason(playServicesStatus))
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val response = moduleInstallClient.areModulesAvailable(recognizer).await()
            if (response.areModulesAvailable()) {
                OcrReadiness.Ready
            } else {
                OcrReadiness.Unavailable(OcrUnavailableReason.ModelNotDownloaded)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to check OCR model availability", e)
            }
            OcrReadiness.Unavailable(OcrUnavailableReason.Unknown)
        } finally {
            recognizer.close()
        }
    }

    override suspend fun installModel(): OcrReadiness {
        val playServicesStatus =
            googleApiAvailability.isGooglePlayServicesAvailable(appContext)
        if (playServicesStatus != ConnectionResult.SUCCESS) {
            return OcrReadiness.Unavailable(playServicesUnavailableReason(playServicesStatus))
        }
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return try {
            val request = ModuleInstallRequest.newBuilder()
                .addApi(recognizer)
                .build()
            val response = moduleInstallClient.installModules(request).await()
            if (response.areModulesAlreadyInstalled()) {
                OcrReadiness.Ready
            } else {
                OcrReadiness.Unavailable(OcrUnavailableReason.ModelNotDownloaded)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to download OCR model", e)
            }
            OcrReadiness.Unavailable(OcrUnavailableReason.ModelNotDownloaded)
        } finally {
            recognizer.close()
        }
    }

    private fun playServicesUnavailableReason(status: Int): OcrUnavailableReason {
        return when (status) {
            ConnectionResult.SERVICE_MISSING -> OcrUnavailableReason.PlayServicesMissing
            ConnectionResult.SERVICE_DISABLED -> OcrUnavailableReason.PlayServicesDisabled
            ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ->
                OcrUnavailableReason.PlayServicesUpdateRequired
            ConnectionResult.SERVICE_UPDATING -> OcrUnavailableReason.PlayServicesUpdating
            else -> OcrUnavailableReason.Unknown
        }
    }
}
