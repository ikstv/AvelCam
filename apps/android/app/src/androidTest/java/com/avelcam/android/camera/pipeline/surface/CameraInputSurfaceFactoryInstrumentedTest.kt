package com.avelcam.android.camera.pipeline.surface

import android.util.Size
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraInputSurfaceFactoryInstrumentedTest {

    @Test
    fun cameraInputSurfaceFactoryCreatesDetachedSurfaceTextureAndSurface() {
        val factory = DefaultCameraInputSurfaceFactory()
        val inputSurface = factory.create(Size(640, 360))

        assertNotNull(inputSurface.surfaceTexture)
        assertNotNull(inputSurface.surface)
        assertTrue(inputSurface.surface.isValid)
        val size = inputSurface.surfaceTexture.getDefaultBufferSize()
        assertEquals(640, size.width)
        assertEquals(360, size.height)
    }

    @Test
    fun cameraInputSurfaceFactoryReleaseIsIdempotent() {
        val factory = DefaultCameraInputSurfaceFactory()
        val inputSurface = factory.create(Size(640, 360))

        inputSurface.release()
        inputSurface.release()
    }
}
