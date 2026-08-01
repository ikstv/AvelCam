package com.avelcam.android.camera.pipeline

import android.opengl.GLES11Ext
import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

private const val POSITION_COMPONENT_SIZE = 2
private const val TEXTURE_COMPONENT_SIZE = 2
private const val VERTEX_STRIDE_BYTES = (POSITION_COMPONENT_SIZE + TEXTURE_COMPONENT_SIZE) * 4

private val TRIANGLE_VERTICES = floatArrayOf(
    -1f, -1f, 0f, 1f,
    1f, -1f, 1f, 1f,
    -1f, 1f, 0f, 0f,
    1f, 1f, 1f, 0f,
)

private val VERTEX_SHADER = """
    attribute vec4 aPosition;
    attribute vec2 aTexCoord;
    uniform mat4 uTextureMatrix;
    varying vec2 vTexCoord;
    void main() {
        gl_Position = aPosition;
        vTexCoord = (uTextureMatrix * vec4(aTexCoord, 0.0, 1.0)).xy;
    }
""".trimIndent()

private val FRAGMENT_SHADER = """
    #extension GL_OES_EGL_image_external : require
    precision mediump float;
    varying vec2 vTexCoord;
    uniform samplerExternalOES uTexture;
    void main() {
        gl_FragColor = texture2D(uTexture, vTexCoord);
    }
""".trimIndent()

internal class PreviewGlRenderer : AutoCloseable {
    private var program = 0
    private var aPosition = -1
    private var aTexCoord = -1
    private var uTextureMatrix = -1
    private var uTextureSampler = -1
    private val vertexBuffer: FloatBuffer = ByteBuffer
        .allocateDirect(TRIANGLE_VERTICES.size * 4)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
        .apply {
            put(TRIANGLE_VERTICES)
            position(0)
        }

    init {
        program = createProgram(VERTEX_SHADER, FRAGMENT_SHADER)
        aPosition = GLES20.glGetAttribLocation(program, "aPosition")
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord")
        uTextureMatrix = GLES20.glGetUniformLocation(program, "uTextureMatrix")
        uTextureSampler = GLES20.glGetUniformLocation(program, "uTexture")
        if (aPosition < 0 || aTexCoord < 0 || uTextureMatrix < 0 || uTextureSampler < 0) {
            throw IllegalStateException("Failed to resolve OES shader attributes/uniforms.")
        }
    }

    fun render(textureId: Int, transform: FloatArray, width: Int, height: Int): Boolean {
        if (textureId <= 0) {
            return false
        }

        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureId)
        GLES20.glUniform1i(uTextureSampler, 0)
        GLES20.glUniformMatrix4fv(uTextureMatrix, 1, false, transform, 0)

        vertexBuffer.position(0)
        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(
            aPosition,
            POSITION_COMPONENT_SIZE,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            vertexBuffer
        )

        vertexBuffer.position(POSITION_COMPONENT_SIZE)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(
            aTexCoord,
            TEXTURE_COMPONENT_SIZE,
            GLES20.GL_FLOAT,
            false,
            VERTEX_STRIDE_BYTES,
            vertexBuffer
        )

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, 0)
        GLES20.glUseProgram(0)

        val error = GLES20.glGetError()
        if (error != GLES20.GL_NO_ERROR) {
            throw IllegalStateException("OES preview render GL error: $error")
        }
        return true
    }

    private fun createProgram(vertexSource: String, fragmentSource: String): Int {
        val vertexShader = com.avelcam.android.encoder.gl.GlShaderProgram(vertexSource, fragmentSource)
        return try {
            val field = vertexShader.javaClass.getDeclaredField("program")
            field.isAccessible = true
            field.getInt(vertexShader)
        } catch (error: Throwable) {
            vertexShader.close()
            throw IllegalStateException("Failed to create OES preview program.", error)
        }
    }

    override fun close() {
        if (program != 0) {
            GLES20.glDeleteProgram(program)
            program = 0
        }
    }
}
