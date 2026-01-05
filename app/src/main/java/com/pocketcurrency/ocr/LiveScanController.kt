package com.pocketcurrency.ocr

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import java.util.concurrent.Executor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class LiveScanController(
    context: Context,
    private val onDetected: (DetectedPrice) -> Unit,
    private val onError: (Throwable) -> Unit,
    private val cameraProviderFuture: ListenableFuture<ProcessCameraProvider> =
        ProcessCameraProvider.getInstance(context),
    private val coordinator: LiveScanCoordinator = LiveScanCoordinator(DEFAULT_THROTTLE_MS),
    private val executorFactory: () -> ExecutorService = { Executors.newSingleThreadExecutor() },
    private val mainExecutor: Executor = ContextCompat.getMainExecutor(context),
    private val cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
) : DefaultLifecycleObserver, AutoCloseable {

    private var lifecycleOwner: LifecycleOwner? = null
    private var surfaceProvider: Preview.SurfaceProvider? = null
    private var imageAnalysis: ImageAnalysis? = null
    private var cameraExecutor: ExecutorService? = null
    private var analysisScope: CoroutineScope? = null
    private var isEnabled = false
    private var hasPermission = false
    private var isLifecycleStarted = false
    private var isBound = false
    private var sessionId = 0

    fun bindToLifecycle(owner: LifecycleOwner) {
        if (lifecycleOwner === owner) {
            isLifecycleStarted =
                owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
            refreshState()
            return
        }
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = owner
        owner.lifecycle.addObserver(this)
        isLifecycleStarted = owner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
        refreshState()
    }

    fun setSurfaceProvider(provider: Preview.SurfaceProvider?) {
        if (surfaceProvider === provider) {
            return
        }
        surfaceProvider = provider
        if (provider == null) {
            stopCamera()
        } else {
            refreshState()
        }
    }

    fun setActive(enabled: Boolean, permissionGranted: Boolean) {
        isEnabled = enabled
        hasPermission = permissionGranted
        refreshState()
    }

    override fun onStart(owner: LifecycleOwner) {
        isLifecycleStarted = true
        refreshState()
    }

    override fun onStop(owner: LifecycleOwner) {
        isLifecycleStarted = false
        stopCamera()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        isLifecycleStarted = false
        stopCamera()
        if (lifecycleOwner === owner) {
            owner.lifecycle.removeObserver(this)
            lifecycleOwner = null
        }
    }

    private fun refreshState() {
        val ready = isEnabled &&
            hasPermission &&
            isLifecycleStarted &&
            lifecycleOwner != null &&
            surfaceProvider != null
        if (ready) {
            startCamera()
        } else {
            stopCamera()
        }
    }

    private fun startCamera() {
        if (isBound) {
            return
        }
        val owner = lifecycleOwner ?: return
        val surface = surfaceProvider ?: return
        val executor = executorFactory()
        cameraExecutor = executor
        analysisScope = analysisScope ?: CoroutineScope(SupervisorJob())
        val startSessionId = ++sessionId

        cameraProviderFuture.addListener({
            val cameraProvider = try {
                cameraProviderFuture.get()
            } catch (e: Exception) {
                cleanupFailedStart(executor, e)
                return@addListener
            }
            if (startSessionId != sessionId) {
                cameraProvider.unbindAll()
                executor.shutdown()
                return@addListener
            }

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(surface)
            }
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            imageAnalysis = analysis
            analysis.setAnalyzer(
                executor,
                object : ImageAnalysis.Analyzer {
                    override fun analyze(imageProxy: ImageProxy) {
                        val scope = analysisScope
                        if (scope == null) {
                            imageProxy.close()
                            return
                        }
                        coordinator.handleImageProxy(
                            imageProxy = imageProxy,
                            scope = scope,
                            onDetected = onDetected,
                            onError = onError
                        )
                    }
                }
            )

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    cameraSelector,
                    preview,
                    analysis
                )
                isBound = true
            } catch (e: Exception) {
                cameraProvider.unbindAll()
                isBound = false
                cleanupFailedStart(executor, e)
            }
        }, mainExecutor)
    }

    private fun stopCamera() {
        if (!isBound && cameraExecutor == null && imageAnalysis == null) {
            return
        }
        sessionId++
        imageAnalysis?.clearAnalyzer()
        imageAnalysis = null
        cameraExecutor?.shutdown()
        cameraExecutor = null
        analysisScope?.cancel()
        analysisScope = null
        isBound = false

        cameraProviderFuture.addListener({
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (e: Exception) {
                onError(e)
            }
        }, mainExecutor)
    }

    private fun cleanupFailedStart(executor: ExecutorService, error: Throwable) {
        executor.shutdown()
        if (cameraExecutor === executor) {
            cameraExecutor = null
        }
        analysisScope?.cancel()
        analysisScope = null
        onError(error)
    }

    override fun close() {
        stopCamera()
        coordinator.close()
        lifecycleOwner?.lifecycle?.removeObserver(this)
        lifecycleOwner = null
    }

    companion object {
        private const val DEFAULT_THROTTLE_MS = 400L
    }
}
