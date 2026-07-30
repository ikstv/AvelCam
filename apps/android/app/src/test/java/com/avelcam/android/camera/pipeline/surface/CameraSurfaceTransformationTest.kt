package com.avelcam.android.camera.pipeline.surface

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraSurfaceTransformationTest {

    @Test
    fun rotationMustBeRightAngle() {
        val rect = CameraSurfaceCropRect(0, 0, 100, 100)
        val transformation = CameraSurfaceTransformation(
            rotationDegrees = 90,
            cropRect = rect,
        )

        assertEquals(90, transformation.rotationDegrees)
        assertEquals(rect, transformation.cropRect)
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidRotationIsRejected() {
        CameraSurfaceTransformation(
            rotationDegrees = 45,
            cropRect = CameraSurfaceCropRect(0, 0, 10, 10),
        )
    }
}
