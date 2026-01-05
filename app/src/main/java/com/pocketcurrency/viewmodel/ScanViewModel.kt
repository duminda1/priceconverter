package com.pocketcurrency.viewmodel

import android.content.Context
import android.util.Log
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import com.pocketcurrency.data.repository.ScanRepository
import com.pocketcurrency.domain.usecase.ProcessScanResultUseCase
import com.pocketcurrency.ocr.LiveScanController
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
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

    private val liveScanController = LiveScanController(
        context = appContext,
        onDetected = { detected ->
            onScanResult(
                amount = detected.amount,
                currencyCode = detected.currencyCode,
                isConfident = detected.isConfident
            )
        },
        onError = { error ->
            Log.e(TAG, "Failed to recognize live scan text", error)
        }
    )

    fun onScanResult(amount: Double, currencyCode: String?, isConfident: Boolean) {
        processScanResultUseCase.execute(amount, currencyCode, isConfident)
    }

    fun bindLiveScanLifecycle(lifecycleOwner: LifecycleOwner) {
        liveScanController.bindToLifecycle(lifecycleOwner)
    }

    fun setLiveScanSurfaceProvider(surfaceProvider: Preview.SurfaceProvider?) {
        liveScanController.setSurfaceProvider(surfaceProvider)
    }

    fun setLiveScanActive(enabled: Boolean, hasPermission: Boolean) {
        liveScanController.setActive(enabled, hasPermission)
    }

    override fun onCleared() {
        liveScanController.close()
        super.onCleared()
    }
}

private const val TAG = "ScanViewModel"
