package com.avelcam.android.camera.pipeline.surface
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CameraInputSurfaceFactoryInstrumentedTest {

    @Test
    fun cameraInputSurfaceFactoryCreatesDetachedSurfaceTextureAndSurface() {
        val factory = DefaultCameraInputSurfaceFactory()
        val requested = CameraSurfaceRequestResolution(640, 360)
        val inputSurface = factory.create(requested)
        val surfaceToken = (inputSurface as CameraInputSurface)

        assertEquals(requested.width, surfaceToken.resolution.width)
        assertEquals(requested.height, surfaceToken.resolution.height)
        assertNotNull(surfaceToken.surface)

        val surface = surfaceToken.surface.resolveSurface()
        assertNotNull(surface)
        assertTrue(surface.isValid)

        surfaceToken.surfaceTexture.setDefaultBufferSize(640, 360)
    }

    @Test
    fun cameraInputSurfaceFactoryReleaseIsIdempotent() {
        val factory = DefaultCameraInputSurfaceFactory()
        val inputSurface = factory.create(CameraSurfaceRequestResolution(640, 360))

        inputSurface.release()
        inputSurface.release()
    }
}
