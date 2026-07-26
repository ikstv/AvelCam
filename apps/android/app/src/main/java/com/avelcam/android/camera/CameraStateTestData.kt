package com.avelcam.android

import androidx.camera.core.CameraSelector

data class TestInput(
    val current: Int,
    val hasRear: Boolean,
    val hasFront: Boolean
)

fun defaultCameraState(): CameraState = CameraState(
    selectedLens = CameraSelector.LENS_FACING_BACK,
    hasRearCamera = true,
    hasFrontCamera = true,
    permissionState = PermissionUiState.NOT_REQUESTED,
)

