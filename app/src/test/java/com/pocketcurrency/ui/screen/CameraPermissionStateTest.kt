package com.pocketcurrency.ui.screen

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraPermissionStateTest {

    @Test
    fun resolveCameraPermissionCardState_whenPermissionGranted_hidesCard() {
        val state = resolveCameraPermissionCardState(
            hasPermission = true,
            liveScanEnabled = true,
            uiState = CameraPermissionUiState.Denied
        )

        assertEquals(false, state.show)
        assertEquals(CameraPermissionUiState.Denied, state.state)
    }

    @Test
    fun resolveCameraPermissionCardState_whenLiveScanEnabled_promptsRationale() {
        val state = resolveCameraPermissionCardState(
            hasPermission = false,
            liveScanEnabled = true,
            uiState = CameraPermissionUiState.Off
        )

        assertEquals(true, state.show)
        assertEquals(CameraPermissionUiState.Rationale, state.state)
    }

    @Test
    fun resolveCameraPermissionCardState_whenDenied_persistsDeniedState() {
        val state = resolveCameraPermissionCardState(
            hasPermission = false,
            liveScanEnabled = false,
            uiState = CameraPermissionUiState.Denied
        )

        assertEquals(true, state.show)
        assertEquals(CameraPermissionUiState.Denied, state.state)
    }

    @Test
    fun nextCameraPermissionUiStateOnEnableAttempt_keepsDenialStates() {
        assertEquals(
            CameraPermissionUiState.Denied,
            nextCameraPermissionUiStateOnEnableAttempt(CameraPermissionUiState.Denied)
        )
        assertEquals(
            CameraPermissionUiState.PermanentlyDenied,
            nextCameraPermissionUiStateOnEnableAttempt(CameraPermissionUiState.PermanentlyDenied)
        )
    }

    @Test
    fun nextCameraPermissionUiStateOnEnableAttempt_promptsRationaleWhenOff() {
        assertEquals(
            CameraPermissionUiState.Rationale,
            nextCameraPermissionUiStateOnEnableAttempt(CameraPermissionUiState.Off)
        )
    }

    @Test
    fun cameraPermissionUiStateForResult_granted_resetsState() {
        assertEquals(
            CameraPermissionUiState.Off,
            cameraPermissionUiStateForResult(isGranted = true, shouldShowRationale = false)
        )
    }

    @Test
    fun cameraPermissionUiStateForResult_denied_setsExpectedState() {
        assertEquals(
            CameraPermissionUiState.Denied,
            cameraPermissionUiStateForResult(isGranted = false, shouldShowRationale = true)
        )
        assertEquals(
            CameraPermissionUiState.PermanentlyDenied,
            cameraPermissionUiStateForResult(isGranted = false, shouldShowRationale = false)
        )
        assertEquals(
            CameraPermissionUiState.Denied,
            cameraPermissionUiStateForResult(isGranted = false, shouldShowRationale = null)
        )
    }
}
