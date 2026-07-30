package com.avelcam.android.camera.pipeline

import kotlin.math.abs
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
    fun landscapeSourceToPortraitDestinationFitPreservesWholeCrop() {
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

        assertEquals(0f, transform.sourceCrop.left)
        assertEquals(0f, transform.sourceCrop.top)
        assertEquals(1f, transform.sourceCrop.right)
        assertEquals(1f, transform.sourceCrop.bottom)
    }

    @Test
    fun portraitSourceToLandscapeDestinationFillCenterCropIsCenteredAndBounded() {
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

        assertTrue(transform.sourceCrop.left >= 0f)
        assertTrue(transform.sourceCrop.top > 0f)
        assertTrue(transform.sourceCrop.right <= 1f)
        assertTrue(transform.sourceCrop.bottom <= 1f)
        assertTrue(transform.sourceCrop.right > transform.sourceCrop.left)
        assertTrue(transform.sourceCrop.bottom > transform.sourceCrop.top)
    }

    @Test
    fun frontPreviewIsMirroredAndEncoderIsNotByDefault() {
        val front = calculator.calculate(
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

        assertEquals(-1f, front.matrix[0], 0.0001f)
        assertEquals(1f, front.matrix[3], 0.0001f)
        assertTrue(nonMirrored.matrix[0] > front.matrix[0])
    }

    @Test
    fun rotationNinetyMappingHasDistinctCornersAndFiniteValues() {
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

        val corners = listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
        val mapped = corners.map { (x, y) -> mapPoint(transform.matrix, x, y) }

        assertTrue(mapped.all { it.first.isFinite() && it.second.isFinite() })
        assertEquals(4, mapped.distinct().size)
    }

    @Test
    fun rotateNinetyTwiceThenInverseProducesIdentity() {
        val t90 = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 640,
                sourceHeight = 360,
                destinationWidth = 360,
                destinationHeight = 640,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val t270 = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 640,
                sourceHeight = 360,
                destinationWidth = 360,
                destinationHeight = 640,
                rotationDegrees = 270,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val corners = listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
        corners.forEach { (x, y) ->
            val first = mapPoint(t90.matrix, x, y)
            val second = mapPoint(t270.matrix, first.first, first.second)
            assertEquals(x, second.first, 0.0001f)
            assertEquals(y, second.second, 0.0001f)
        }
    }

    @Test
    fun fourNinetyRotationsReturnToOriginalForCorners() {
        val transform90 = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 640,
                sourceHeight = 360,
                destinationWidth = 360,
                destinationHeight = 640,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val corners = listOf(0f to 0f, 0.2f to 0.7f, 1f to 1f)
        corners.forEach { (x, y) ->
            var px = x
            var py = y
            repeat(4) {
                val next = mapPoint(transform90.matrix, px, py)
                px = next.first
                py = next.second
            }
            assertEquals(x, px, 0.0001f)
            assertEquals(y, py, 0.0001f)
        }
    }

    @Test
    fun rotate180TwiceReturnsIdentity() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 640,
                sourceHeight = 360,
                destinationWidth = 640,
                destinationHeight = 360,
                rotationDegrees = 180,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val rotated = mapPoint(transform.matrix, 0.25f, 0.7f)
        val restored = mapPoint(transform.matrix, rotated.first, rotated.second)

        assertEquals(0.25f, restored.first, 0.0001f)
        assertEquals(0.7f, restored.second, 0.0001f)
    }

    @Test
    fun fitAndFillRemainFiniteAndStableForRotatedAspect() {
        val fill = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 1280,
                sourceHeight = 720,
                destinationWidth = 720,
                destinationHeight = 720,
                rotationDegrees = 270,
                mirror = false,
                cropMode = CameraPipelineCropMode.FILL_CENTER_CROP,
                surfaceTextureMatrix = matrixIdentity
            )
        )
        assertTrue(fill.sourceCrop.left in 0.0f..1.0f)
        assertTrue(fill.sourceCrop.top in 0.0f..1.0f)
        assertTrue(fill.sourceCrop.right in 0.0f..1.0f)
        assertTrue(fill.sourceCrop.bottom in 0.0f..1.0f)
        assertTrue(fill.sourceCrop.right > fill.sourceCrop.left)
        assertTrue(fill.sourceCrop.bottom > fill.sourceCrop.top)
        assertEquals(16, fill.matrix.size)
        assertTrue(fill.matrix.all { it.isFinite() })
    }

    @Test
    fun sourceSurfaceAspectRotationBeforeDestinationCropIsUsed() {
        val withoutRotation = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 720,
                sourceHeight = 1280,
                destinationWidth = 1280,
                destinationHeight = 720,
                rotationDegrees = 0,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val withRotation = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 720,
                sourceHeight = 1280,
                destinationWidth = 1280,
                destinationHeight = 720,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val controlPoint = 0.25f to 0.75f
        val withoutMapped = mapPoint(withoutRotation.matrix, controlPoint.first, controlPoint.second)
        val withMapped = mapPoint(withRotation.matrix, controlPoint.first, controlPoint.second)

        assertTrue(
            abs(withoutMapped.first - withMapped.first) > 1e-5f ||
                abs(withoutMapped.second - withMapped.second) > 1e-5f
        )
    }

    @Test
    fun fitModeMatrixForNinetyKeepsFiniteCornerOutput() {
        val transform = calculator.calculate(
            CameraTransformInput(
                sourceWidth = 720,
                sourceHeight = 1280,
                destinationWidth = 1080,
                destinationHeight = 1920,
                rotationDegrees = 90,
                mirror = false,
                cropMode = CameraPipelineCropMode.FIT,
                surfaceTextureMatrix = matrixIdentity
            )
        )

        val corners = listOf(0f to 0f, 1f to 0f, 0f to 1f, 1f to 1f)
        val mapped = corners.map { mapPoint(transform.matrix, it.first, it.second) }
        mapped.forEach { assertNotNull(it) }
        assertEquals(4, mapped.distinct().size)
    }
}
