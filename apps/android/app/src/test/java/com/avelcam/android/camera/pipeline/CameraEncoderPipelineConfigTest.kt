
package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class CameraEncoderPipelineConfigTest {
    @Test
    fun validConfigAccepted() {
        val config = CameraEncoderPipelineConfig(
            cameraWidth = 1280,
            cameraHeight = 720,
            previewWidth = 720,
            previewHeight = 1280,
            encoderWidth = 1280,
            encoderHeight = 720,
            frontCameraPreviewMirrored = true,
            frontCameraEncoderMirrored = false,
            cropMode = CameraPipelineCropMode.FIT
        )
        assertEquals(1280, config.cameraWidth)
    }

    @Test
    fun zeroDimensionRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraEncoderPipelineConfig(
                cameraWidth = 0,
                cameraHeight = 720,
                previewWidth = 720,
                previewHeight = 1280,
                encoderWidth = 1280,
                encoderHeight = 720,
                frontCameraPreviewMirrored = false,
                frontCameraEncoderMirrored = false,
                cropMode = CameraPipelineCropMode.FIT
            )
        }
    }

    @Test
    fun negativeDimensionRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraEncoderPipelineConfig(
                cameraWidth = 1280,
                cameraHeight = -720,
                previewWidth = 720,
                previewHeight = 1280,
                encoderWidth = 1280,
                encoderHeight = 720,
                frontCameraPreviewMirrored = false,
                frontCameraEncoderMirrored = false,
                cropMode = CameraPipelineCropMode.FIT
            )
        }
    }

    @Test
    fun oddEncoderDimensionsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraEncoderPipelineConfig(
                cameraWidth = 1280,
                cameraHeight = 720,
                previewWidth = 720,
                previewHeight = 1280,
                encoderWidth = 1279,
                encoderHeight = 720,
                frontCameraPreviewMirrored = false,
                frontCameraEncoderMirrored = false,
                cropMode = CameraPipelineCropMode.FIT
            )
        }
    }

    @Test
    fun invalidCropModeRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            CameraEncoderPipelineConfig(
                cameraWidth = 1280,
                cameraHeight = 720,
                previewWidth = 720,
                previewHeight = 1280,
                encoderWidth = 1280,
                encoderHeight = 720,
                frontCameraPreviewMirrored = false,
                frontCameraEncoderMirrored = false,
                cropMode = CameraPipelineCropMode.valueOf("FIT")
            )
        }
    }
}
