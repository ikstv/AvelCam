package com.avelcam.android

import androidx.camera.core.CameraSelector

enum class PermissionUiState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
    PERMANENTLY_DENIED,
    UNAVAILABLE
}

data class CameraState(
    val selectedLens: Int = CameraSelector.LENS_FACING_BACK,
    val hasRearCamera: Boolean = true,
    val hasFrontCamera: Boolean = true,
    val permissionState: PermissionUiState = PermissionUiState.NOT_REQUESTED,
    val errorMessage: String? = null,
    val isBinding: Boolean = false
) {
    val canSwitchCamera: Boolean
        get() = hasRearCamera && hasFrontCamera

    val availableCamerasCount: Int
        get() {
            var count = 0
            if (hasRearCamera) count++
            if (hasFrontCamera) count++
            return count
        }
}

fun isRearCamera(lens: Int): Boolean = lens == CameraSelector.LENS_FACING_BACK

fun resolvePermissionState(granted: Boolean, shouldShowRationale: Boolean, hasPermissionCapability: Boolean): PermissionUiState {
    if (!hasPermissionCapability) {
        return PermissionUiState.UNAVAILABLE
    }

    return if (granted) {
        PermissionUiState.GRANTED
    } else if (shouldShowRationale) {
        PermissionUiState.DENIED
    } else {
        PermissionUiState.PERMANENTLY_DENIED
    }
}

fun nextCameraForSwitch(current: Int, hasRear: Boolean, hasFront: Boolean): Int {
    return if (current == CameraSelector.LENS_FACING_BACK) {
        if (hasFront) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
    } else {
        if (hasRear) CameraSelector.LENS_FACING_BACK else CameraSelector.LENS_FACING_FRONT
    }
}

