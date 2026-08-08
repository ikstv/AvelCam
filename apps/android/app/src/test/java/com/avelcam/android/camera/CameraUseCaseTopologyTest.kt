package com.avelcam.android.camera

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraUseCaseTopologyTest {
    @Test
    fun fanoutTopologyBindsOnlyOnePreviewAndImageAnalysis() {
        assertEquals(
            listOf(
                CameraUseCaseRole.FANOUT_PREVIEW,
                CameraUseCaseRole.IMAGE_ANALYSIS,
            ),
            fanoutCameraUseCaseRoles(),
        )
    }
}
