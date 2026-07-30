package com.avelcam.android.camera.pipeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class CameraFrameMetadataTest {
    @Test
    fun constructorCopiesMatrixToAvoidAliasing() {
        val source = FloatArray(16) { it.toFloat() }
        val metadata = CameraFrameMetadata(
            sourceTimestampNs = 100L,
            mappedTimestampNs = 200L,
            rotationDegrees = 90,
            isFrontCamera = false,
            surfaceTextureTransformMatrix = source
        )

        source[0] = 999f
        val copied = metadata.copySurfaceTextureTransformMatrix()

        assertEquals(0f, copied[0], 0.0001f)
        assertNotSame(source, copied)
    }

    @Test
    fun copiedMatrixIsDefensiveAgainstMutation() {
        val metadata = CameraFrameMetadata(
            sourceTimestampNs = 1L,
            mappedTimestampNs = 2L,
            rotationDegrees = 180,
            isFrontCamera = true,
            surfaceTextureTransformMatrix = FloatArray(16) { 1f }
        )

        val copy = metadata.copySurfaceTextureTransformMatrix()
        copy[0] = 777f

        val second = metadata.copySurfaceTextureTransformMatrix()
        assertEquals(1f, second[0], 0.0001f)
    }

    @Test
    fun mutationAfterCreationCannotAffectStoredMatrix() {
        val input = FloatArray(16) { 0.25f }
        val metadata = CameraFrameMetadata(
            sourceTimestampNs = 11L,
            mappedTimestampNs = 22L,
            rotationDegrees = 270,
            isFrontCamera = false,
            surfaceTextureTransformMatrix = input
        )

        val copyBefore = metadata.copySurfaceTextureTransformMatrix()
        input[15] = -1f

        val copyAfter = metadata.copySurfaceTextureTransformMatrix()
        assertEquals(copyBefore[15], copyAfter[15], 0.0001f)
    }
}
