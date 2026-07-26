package com.avelcam.android.camera.pipeline

data class CameraTextureTransform(
    val matrix: FloatArray,
    val sourceCrop: CropRect,
    val destinationCrop: CropRect
) {
    init {
        require(matrix.size == 16) { "Transform matrix must contain 16 values." }
        require(sourceCrop.isValid())
        require(destinationCrop.isValid())
    }

    val matrixSnapshot: FloatArray = matrix.copyOf()
}

data class CropRect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    fun isValid(): Boolean = left >= 0f && top >= 0f && right <= 1f && bottom <= 1f && right > left && bottom > top
}

