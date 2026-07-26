package com.avelcam.android.encoder.gl

class SyntheticFrameRenderer : AutoCloseable {
    fun render(frameIndex: Int, width: Int, height: Int) {
        val red = ((frameIndex % 360) / 360f)
        val phase = (frameIndex % 360) / 360f
        val blue = 1f - phase
        android.opengl.GLES20.glViewport(0, 0, width, height)
        android.opengl.GLES20.glClearColor(red, phase * 0.3f, blue, 1f)
        android.opengl.GLES20.glClear(android.opengl.GLES20.GL_COLOR_BUFFER_BIT)
        android.opengl.GLES20.glFlush()
    }

    override fun close() {
    }
}
