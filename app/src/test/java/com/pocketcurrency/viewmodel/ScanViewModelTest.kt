package com.pocketcurrency.viewmodel

import android.content.Context
import androidx.camera.core.Preview
import androidx.lifecycle.LifecycleOwner
import com.pocketcurrency.data.repository.ScanRepository
import com.pocketcurrency.domain.usecase.ProcessScanResultUseCase
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
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

        viewModel.setLiveScanActive(true, true)
        advanceUntilIdle()

        assertTrue(viewModel.ocrReadiness.value is OcrReadiness.Unavailable)
        assertEquals(false, liveScanController.lastEnabled)
        assertEquals(true, liveScanController.lastPermission)

        viewModel.requestOcrModelDownload()
        advanceUntilIdle()

        assertEquals(OcrReadiness.Ready, viewModel.ocrReadiness.value)
        assertEquals(true, liveScanController.lastEnabled)
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

    override fun close() = Unit
}

private class FakeOcrModelManager(
    private val checkResult: OcrReadiness,
    private val installResult: OcrReadiness
) : OcrModelManager {
    override suspend fun checkAvailability(): OcrReadiness = checkResult

    override suspend fun installModel(): OcrReadiness = installResult
}
