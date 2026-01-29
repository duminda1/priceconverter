package com.pockettoolsstudio.pocketcurrency.ui.screen

internal enum class CameraPermissionUiState {
    Off,
    Rationale,
    Denied,
    PermanentlyDenied
}

internal fun resolveCameraPermissionCardState(
    hasPermission: Boolean,
    liveScanEnabled: Boolean,
    uiState: CameraPermissionUiState
): CameraPermissionCardState {
    val show = !hasPermission && (liveScanEnabled || uiState != CameraPermissionUiState.Off)
    val state = if (show && uiState == CameraPermissionUiState.Off) {
        CameraPermissionUiState.Rationale
    } else {
        uiState
    }
    return CameraPermissionCardState(show, state)
}

internal fun nextCameraPermissionUiStateOnEnableAttempt(
    currentState: CameraPermissionUiState
): CameraPermissionUiState {
    return when (currentState) {
        CameraPermissionUiState.Denied -> CameraPermissionUiState.Denied
        CameraPermissionUiState.PermanentlyDenied -> CameraPermissionUiState.PermanentlyDenied
        CameraPermissionUiState.Off,
        CameraPermissionUiState.Rationale -> CameraPermissionUiState.Rationale
    }
}

internal fun cameraPermissionUiStateForResult(
    isGranted: Boolean,
    shouldShowRationale: Boolean?
): CameraPermissionUiState {
    if (isGranted) {
        return CameraPermissionUiState.Off
    }
    return when (shouldShowRationale) {
        true -> CameraPermissionUiState.Denied
        false -> CameraPermissionUiState.PermanentlyDenied
        null -> CameraPermissionUiState.Denied
    }
}
