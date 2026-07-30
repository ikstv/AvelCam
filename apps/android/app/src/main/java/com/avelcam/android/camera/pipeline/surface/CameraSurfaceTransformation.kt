package com.avelcam.android.camera.pipeline.surface

data class CameraSurfaceTransformation(
    val rotationDegrees: Int,
    val cropRect: CameraSurfaceCropRect,
    val hasCameraTransform: Boolean,
) {
    init {
        require(rotationDegrees in listOf(0, 90, 180, 270)) {
            "Unsupported rotation: $rotationDegrees"
        }
    }
}

data class CameraSurfaceCropRect(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
