package com.avelcam.android.camera.pipeline.surface

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.util.concurrent.atomic.AtomicBoolean

internal class ExternalOesTexture private constructor(
    val textureId: Int,
    private val gl: ExternalOesTextureGlApi,
) : AutoCloseable {
    private val isClosed = AtomicBoolean(false)

    init {
        require(textureId > 0) {
            "External OES texture id must be > 0."
        }
    }

    override fun close() {
        if (!isClosed.compareAndSet(false, true)) {
            return
        }
        gl.deleteTexture(textureId)
    }

    companion object {
        fun create(gl: ExternalOesTextureGlApi = AndroidExternalOesTextureGlApi): ExternalOesTexture {
            val textureId = gl.createTexture()
            return try {
                gl.configureExternalOesTexture(textureId)
                ExternalOesTexture(textureId, gl)
            } catch (error: Throwable) {
                gl.deleteTexture(textureId)
                throw error
            }
        }
    }
}

internal interface ExternalOesTextureGlApi {
    fun createTexture(): Int
    fun configureExternalOesTexture(textureId: Int)
    fun deleteTexture(textureId: Int)
}

internal object AndroidExternalOesTextureGlApi : ExternalOesTextureGlApi {
    override fun createTexture(): Int {
        val textures = IntArray(1)
        GLES20.glGenTextures(1, textures, 0)
        val textureId = textures[0]
        if (textureId <= 0) {
            throw IllegalStateException("Failed to allocate external OES texture.")
        }
        return textureId
    }

    override fun configureExternalOesTexture(textureId: Int) {
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MIN_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_MAG_FILTER,
            GLES20.GL_LINEAR,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glTexParameteri(
            GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE,
        )
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw IllegalStateException("Failed to configure external OES texture: GL error $error.")
        }
    }

    override fun deleteTexture(textureId: Int) {
        if (textureId <= 0) {
            return
        }
        GLES20.glDeleteTextures(1, intArrayOf(textureId), 0)
    }
}
