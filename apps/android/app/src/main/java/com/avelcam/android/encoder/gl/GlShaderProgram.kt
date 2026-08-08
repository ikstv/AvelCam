package com.avelcam.android.encoder.gl

import android.opengl.GLES20

class GlShaderProgram(
    private val vertexSource: String,
    private val fragmentSource: String
) : AutoCloseable {
    private val program: Int

    init {
        val vert = compileShader(GLES20.GL_VERTEX_SHADER, vertexSource)
        val frag = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
        program = GLES20.glCreateProgram()
        GLES20.glAttachShader(program, vert)
        GLES20.glAttachShader(program, frag)
        GLES20.glLinkProgram(program)
        val status = IntArray(1)
        GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetProgramInfoLog(program)
            GLES20.glDeleteProgram(program)
            throw IllegalStateException("Failed to link shader program: $log")
        }
        GLES20.glDetachShader(program, vert)
        GLES20.glDetachShader(program, frag)
        GLES20.glDeleteShader(vert)
        GLES20.glDeleteShader(frag)
    }

    private fun compileShader(type: Int, source: String): Int {
        val shader = GLES20.glCreateShader(type)
        GLES20.glShaderSource(shader, source)
        GLES20.glCompileShader(shader)
        val status = IntArray(1)
        GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, status, 0)
        if (status[0] == 0) {
            val log = GLES20.glGetShaderInfoLog(shader)
            GLES20.glDeleteShader(shader)
            throw IllegalStateException("Failed to compile shader: $log")
        }
        return shader
    }

    fun useProgram() {
        GLES20.glUseProgram(program)
    }

    fun getUniformLocation(name: String): Int = GLES20.glGetUniformLocation(program, name)
    fun getAttributeLocation(name: String): Int = GLES20.glGetAttribLocation(program, name)

    override fun close() {
        GLES20.glDeleteProgram(program)
    }
}

