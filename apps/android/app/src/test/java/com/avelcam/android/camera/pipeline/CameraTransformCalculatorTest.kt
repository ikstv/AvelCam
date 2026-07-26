
package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraTransformCalculatorTest {
    private val matrixIdentity = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private val calculator = CameraTransformCalculator()

    @Test
    fun rearPortraitPreview() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 1280,
                rotationDegrees = 0,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        assertNotNull(transform.matrix)
        assertTrue(transform.matrix[0].isFinite())
        assertEquals(16, transform.matrix.size)
    }

    @Test
    fun rearLandscapePreview() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 720,
                sourceHeight = 1280,
                destinationWidth = 1280,
                destinationHeight = 720,
                rotationDegrees = 0,
                mirror = false,
                cropMode = CameraPipelineCropMode.FILL_CENTER_CROP,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        assertEquals(1f, transform.sourceCrop.right, 0.0001f)
    }

    @Test
    fun frontPortraitPreviewMirrored() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 1280,
                rotationDegrees = 0,
                mirror = true,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        assertEquals(-1f, transform.matrix[0], 0.0001f)
        assertEquals(1f, transform.matrix[3], 0.0001f)
    }

    @Test
    fun frontEncoderNonMirroredByDefault() {
        val nonMirrored = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 1280,
                rotationDegrees = 0,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )
        val mirrored = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 1280,
                rotationDegrees = 0,
                mirror = true,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )
        assertTrue(nonMirrored.matrix[0] > mirrored.matrix[0])
    }

    @Test
    fun fitModeMaintainsFiniteMatrix() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1920,
                sourceHeight = 1080,
                destinationWidth = 1280,
                destinationHeight = 720,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )
        assertTrue(transform.matrix.all { it.isFinite() })
        assertEquals(16, transform.matrix.size)
    }

    @Test
    fun fillModeUsesCenterCropForPortrait() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 720,
                rotationDegrees = 0,
                mirror = false,
                cropMode = CameraPipelineCropMode.FILL_CENTER_CROP,
                surfaceTextureMatrix = matrixIdentity
            )
        )
        assertTrue(transform.sourceCrop.right > transform.sourceCrop.left)
        assertTrue(transform.sourceCrop.bottom > transform.sourceCrop.top)
        assertTrue(transform.sourceCrop.left > 0f || transform.sourceCrop.right < 1f)
    }

    @Test
    fun rotationNinetyUsesExpectedSign() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1920,
                sourceHeight = 1080,
                destinationWidth = 1080,
                destinationHeight = 1920,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        assertEquals(0f, transform.matrix[1], 0.0001f)
        assertEquals(-1f, transform.matrix[4], 0.0001f)
    }
}
