package com.avelcam.android

import androidx.camera.core.CameraSelector
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraStateTest {
    @Test
    fun defaultSelectedCameraIsRear() {
        val state = defaultCameraState()
        assertEquals(CameraSelector.LENS_FACING_BACK, state.selectedLens)
    }

    @Test
    fun switchRearToFront() {
        assertEquals(
            CameraSelector.LENS_FACING_FRONT,
            nextCameraForSwitch(
                CameraSelector.LENS_FACING_BACK,
                hasRear = true,
                hasFront = true
            )
        )
    }

    @Test
    fun switchFrontToRear() {
        assertEquals(
            CameraSelector.LENS_FACING_BACK,
            nextCameraForSwitch(
                CameraSelector.LENS_FACING_FRONT,
                hasRear = true,
                hasFront = true
            )
        )
    }

    @Test
    fun unavailableRearCameraCannotBeSelected() {
        assertEquals(
            CameraSelector.LENS_FACING_FRONT,
            nextCameraForSwitch(
                CameraSelector.LENS_FACING_BACK,
                hasRear = false,
                hasFront = true
            )
        )
    }

    @Test
    fun permissionStateMapsDeniedWithoutRationaleAsPermanent() {
        assertEquals(
            PermissionUiState.PERMANENTLY_DENIED,
            resolvePermissionState(
                granted = false,
                shouldShowRationale = false,
                hasPermissionCapability = true
            )
        )
    }
}

