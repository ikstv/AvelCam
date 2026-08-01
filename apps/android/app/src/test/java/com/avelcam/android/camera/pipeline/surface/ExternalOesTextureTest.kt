package com.avelcam.android.camera.pipeline.surface

import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ExternalOesTextureTest {
    @Test
    fun createConfiguresTextureAndCloseDeletesOnce() {
        val gl = FakeExternalOesTextureGlApi(textureId = 17)

        val texture = ExternalOesTexture.create(gl)
        texture.close()
        texture.close()

        assertEquals(17, texture.textureId)
        assertEquals(listOf("create", "configure:17", "delete:17"), gl.events)
    }

    @Test
    fun createRejectsInvalidTextureId() {
        val gl = FakeExternalOesTextureGlApi(textureId = 0)

        try {
            ExternalOesTexture.create(gl)
            fail("Expected IllegalArgumentException")
        } catch (expected: IllegalArgumentException) {
            assertEquals("External OES texture id must be > 0.", expected.message)
        }
    }

    @Test
    fun createDeletesTextureWhenConfigurationFails() {
        val gl = FakeExternalOesTextureGlApi(
            textureId = 23,
            configureFailure = IllegalStateException("configure failed"),
        )

        try {
            ExternalOesTexture.create(gl)
            fail("Expected IllegalStateException")
        } catch (expected: IllegalStateException) {
            assertEquals("configure failed", expected.message)
        }

        assertEquals(listOf("create", "configure:23", "delete:23"), gl.events)
    }
}

private class FakeExternalOesTextureGlApi(
    private val textureId: Int,
    private val configureFailure: RuntimeException? = null,
) : ExternalOesTextureGlApi {
    val events = mutableListOf<String>()

    override fun createTexture(): Int {
        events += "create"
        return textureId
    }

    override fun configureExternalOesTexture(textureId: Int) {
        events += "configure:$textureId"
        configureFailure?.let { throw it }
    }

    override fun deleteTexture(textureId: Int) {
        events += "delete:$textureId"
    }
}
