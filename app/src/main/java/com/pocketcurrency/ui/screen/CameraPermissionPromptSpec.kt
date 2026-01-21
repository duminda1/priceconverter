package com.pocketcurrency.ui.screen

internal data class CameraPermissionPromptSpec(
    val title: String,
    val message: String,
    val primaryActionLabel: String,
    val onPrimaryAction: () -> Unit
)
