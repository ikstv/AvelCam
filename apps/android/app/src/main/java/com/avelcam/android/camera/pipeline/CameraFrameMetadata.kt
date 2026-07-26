package com.avelcam.android.camera.pipeline

data class CameraFrameMetadata(
    val sourceTimestampNs: Long,
    val mappedTimestampNs: Long,
    val rotationDegrees: Int,
    val isFrontCamera: Boolean,
    matrix: FloatArray
) {
    init {
        require(matrix.size == 16) { "SurfaceTexture transform matrix must have 16 elements." }
        require(rotationDegrees == 0 || rotationDegrees == 90 || rotationDegrees == 180 || rotationDegrees == 270) {
            "rotationDegrees must be one of 0,90,180,270."
        }
    }

    val surfaceTextureTransformMatrix: FloatArray = matrix.copyOf()
}
