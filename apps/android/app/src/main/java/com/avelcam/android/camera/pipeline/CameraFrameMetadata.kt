package com.avelcam.android.camera.pipeline

data class CameraFrameMetadata(
    val sourceTimestampNs: Long,
    val mappedTimestampNs: Long,
    val rotationDegrees: Int,
    val isFrontCamera: Boolean,
    val surfaceTextureTransformMatrix: FloatArray,
) {
    init {
        require(surfaceTextureTransformMatrix.size == 16) { "SurfaceTexture transform matrix must have 16 elements." }
        require(rotationDegrees == 0 || rotationDegrees == 90 || rotationDegrees == 180 || rotationDegrees == 270) {
            "rotationDegrees must be one of 0,90,180,270."
        }
    }
}
