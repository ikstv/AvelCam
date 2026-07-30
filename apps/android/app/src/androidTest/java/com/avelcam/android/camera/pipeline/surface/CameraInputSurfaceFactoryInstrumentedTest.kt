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
        val bufferSize = inputSurface.surfaceTexture::class.java
            .getMethod("getDefaultBufferSize")
            .invoke(inputSurface.surfaceTexture) as android.util.Size
        assertEquals(640, bufferSize.width)
        assertEquals(360, bufferSize.height)
    }

    @Test
    fun cameraInputSurfaceFactoryReleaseIsIdempotent() {
        val factory = DefaultCameraInputSurfaceFactory()
        val inputSurface = factory.create(Size(640, 360))

        inputSurface.release()
        inputSurface.release()
    }
}
