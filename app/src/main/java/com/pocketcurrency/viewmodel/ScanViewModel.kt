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
import com.pocketcurrency.ocr.DetectedPrice
import com.pocketcurrency.ocr.LiveScanController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    val scanCurrencyConfident: StateFlow<Boolean> = scanRepository.scanCurrencyConfident

    private val _ocrReadiness = MutableStateFlow<OcrReadiness>(OcrReadiness.Unknown)
    val ocrReadiness: StateFlow<OcrReadiness> = _ocrReadiness

    private val onDetectedHandler: (DetectedPrice) -> Unit = { detected ->
        onScanResult(
            amount = detected.amount,
            currencyCode = detected.currencyCode,
            isConfident = detected.isConfident
        )
    }
    private val onErrorHandler: (Throwable) -> Unit = { error ->
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
    private var liveScanRequested = false
    private var hasCameraPermission = false

    fun onScanResult(amount: Double, currencyCode: String?, isConfident: Boolean) {
        processScanResultUseCase.execute(amount, currencyCode, isConfident)
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

    fun setLiveScanActive(enabled: Boolean, hasPermission: Boolean) {
        liveScanRequested = enabled
        hasCameraPermission = hasPermission
        updateLiveScanState()
        if (enabled && hasPermission) {
            refreshOcrReadiness()
        }
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

    private fun updateLiveScanState() {
        val ready = _ocrReadiness.value is OcrReadiness.Ready
        liveScanController().setActive(
            liveScanRequested && hasCameraPermission && ready,
            hasCameraPermission
        )
    }

    override fun onCleared() {
        liveScanController?.close()
        super.onCleared()
    }
}

private const val TAG = "ScanViewModel"

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

internal interface LiveScanControllerDelegate : AutoCloseable {
    fun bindToLifecycle(owner: LifecycleOwner)
    fun setSurfaceProvider(provider: Preview.SurfaceProvider?)
    fun setActive(enabled: Boolean, permissionGranted: Boolean)
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
