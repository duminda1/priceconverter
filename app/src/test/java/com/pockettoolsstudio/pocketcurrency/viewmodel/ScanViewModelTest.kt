package com.pockettoolsstudio.pocketcurrency.viewmodel

import android.content.Context
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.pockettoolsstudio.pocketcurrency.data.repository.ScanRepository
import com.pockettoolsstudio.pocketcurrency.domain.usecase.ProcessScanResultUseCase
import com.pockettoolsstudio.pocketcurrency.ocr.CurrencyConfidence
import com.pockettoolsstudio.pocketcurrency.ocr.CurrencySource
import com.pockettoolsstudio.pocketcurrency.ocr.DetectedPrice
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScanViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `ocr readiness gates live scan until model installed`() = runTest {
        val repository = ScanRepository()
        val useCase = mockk<ProcessScanResultUseCase>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val viewModel = ScanViewModel(repository, useCase, context)

        val liveScanController = FakeLiveScanController()
        val ocrManager = FakeOcrModelManager(
            checkResult = OcrReadiness.Unavailable(OcrUnavailableReason.ModelNotDownloaded),
            installResult = OcrReadiness.Ready
        )
        viewModel.setLiveScanControllerForTesting(liveScanController)
        viewModel.setOcrModelManagerForTesting(ocrManager)

        viewModel.updateLiveScanAvailability(true, true)
        viewModel.startLiveScan()
        runCurrent()

        assertTrue(viewModel.ocrReadiness.value is OcrReadiness.Unavailable)
        assertEquals(false, liveScanController.lastEnabled)
        assertEquals(true, liveScanController.lastPermission)

        viewModel.requestOcrModelDownload()
        runCurrent()

        assertEquals(OcrReadiness.Ready, viewModel.ocrReadiness.value)
        assertEquals(true, liveScanController.lastEnabled)
    }

    @Test
    fun `camera stays idle until user requests live scan`() = runTest {
        val repository = ScanRepository()
        val useCase = mockk<ProcessScanResultUseCase>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val viewModel = ScanViewModel(repository, useCase, context)

        val liveScanController = FakeLiveScanController()
        val ocrManager = FakeOcrModelManager(
            checkResult = OcrReadiness.Ready,
            installResult = OcrReadiness.Ready
        )
        viewModel.setLiveScanControllerForTesting(liveScanController)
        viewModel.setOcrModelManagerForTesting(ocrManager)

        viewModel.updateLiveScanAvailability(true, true)
        runCurrent()

        assertEquals(OcrReadiness.Ready, viewModel.ocrReadiness.value)
        assertTrue(viewModel.scanUiState.value is LiveScanUiState.Idle)
        assertEquals(false, liveScanController.lastEnabled)
    }

    @Test
    fun `detected price stops scan and emits result`() = runTest {
        val repository = ScanRepository()
        val useCase = mockk<ProcessScanResultUseCase>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val viewModel = ScanViewModel(repository, useCase, context)

        val liveScanController = FakeLiveScanController()
        val ocrManager = FakeOcrModelManager(
            checkResult = OcrReadiness.Ready,
            installResult = OcrReadiness.Ready
        )
        viewModel.setLiveScanControllerForTesting(liveScanController)
        viewModel.setOcrModelManagerForTesting(ocrManager)

        viewModel.updateLiveScanAvailability(true, true)
        viewModel.startLiveScan()
        runCurrent()

        assertEquals(true, liveScanController.lastEnabled)
        assertTrue(viewModel.scanUiState.value is LiveScanUiState.Scanning)

        viewModel.onDetectedPriceForTesting(
            DetectedPrice(
                amount = 12.34,
                currencyCode = "USD",
                currencyConfidence = CurrencyConfidence.HIGH,
                currencySource = CurrencySource.OCR_EXPLICIT
            )
        )
        runCurrent()

        val result = viewModel.scanUiState.value as LiveScanUiState.Result
        assertEquals(12.34, result.amount ?: 0.0, 0.0)
        assertEquals("USD", result.currencyCode)
        assertEquals(false, liveScanController.lastEnabled)
    }

    @Test
    fun `scan times out and stops camera`() = runTest {
        val repository = ScanRepository()
        val useCase = mockk<ProcessScanResultUseCase>(relaxed = true)
        val context = mockk<Context>(relaxed = true)
        val viewModel = ScanViewModel(repository, useCase, context)

        val liveScanController = FakeLiveScanController()
        val ocrManager = FakeOcrModelManager(
            checkResult = OcrReadiness.Ready,
            installResult = OcrReadiness.Ready
        )
        viewModel.setLiveScanControllerForTesting(liveScanController)
        viewModel.setOcrModelManagerForTesting(ocrManager)

        viewModel.updateLiveScanAvailability(true, true)
        viewModel.startLiveScan()
        runCurrent()

        advanceTimeBy(LIVE_SCAN_TIMEOUT_MS)
        runCurrent()

        val result = viewModel.scanUiState.value as LiveScanUiState.Result
        assertNull(result.amount)
        assertEquals(false, liveScanController.lastEnabled)
    }
}

private class FakeLiveScanController : LiveScanControllerDelegate {
    var lastEnabled: Boolean? = null
    var lastPermission: Boolean? = null

    override fun bindToLifecycle(owner: LifecycleOwner) = Unit

    override fun setSurfaceProvider(provider: Preview.SurfaceProvider?) = Unit

    override fun setActive(enabled: Boolean, permissionGranted: Boolean) {
        lastEnabled = enabled
        lastPermission = permissionGranted
    }

    override fun setSelectedFromCurrency(currencyCode: String?) = Unit

    override fun close() = Unit
}

private class FakeOcrModelManager(
    private val checkResult: OcrReadiness,
    private val installResult: OcrReadiness
) : OcrModelManager {
    override suspend fun checkAvailability(): OcrReadiness = checkResult

    override suspend fun installModel(): OcrReadiness = installResult
}
