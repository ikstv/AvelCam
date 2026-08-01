package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraGlFanoutPlanTest {
    @Test
    fun configGeneratesBothPreviewAndEncoderSpecs() {
        val config = CameraEncoderPipelineConfig(
            cameraWidth = 1920,
            cameraHeight = 1080,
            previewWidth = 1080,
            previewHeight = 1920,
            encoderWidth = 1280,
            encoderHeight = 720,
            frontCameraPreviewMirrored = false,
            frontCameraEncoderMirrored = false,
            cropMode = CameraPipelineCropMode.FIT
        )

        val plan = config.toGlFanoutOutputSpecs()

        assertEquals(2, plan.outputs.size)
        assertTrue(plan.outputs.any { it.role == CameraGlFanoutOutputRole.PREVIEW })
        assertTrue(plan.outputs.any { it.role == CameraGlFanoutOutputRole.ENCODER })
    }

    @Test(expected = IllegalArgumentException::class)
    fun planMissingEncoderRoleIsRejected() {
        CameraGlFanoutPlan(
            outputs = listOf(
                CameraGlFanoutOutputSpec(
                    role = CameraGlFanoutOutputRole.PREVIEW,
                    width = 640,
                    height = 480
                )
            )
        )
    }
}
