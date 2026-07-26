package com.avelcam.android.camera.pipeline

data class CameraEncoderPipelineConfig(
    val cameraWidth: Int,
    val cameraHeight: Int,
    val previewWidth: Int,
    val previewHeight: Int,
    val encoderWidth: Int,
    val encoderHeight: Int,
    val frontCameraPreviewMirrored: Boolean,
    val frontCameraEncoderMirrored: Boolean,
    val cropMode: CameraPipelineCropMode
) {
    init {
        require(cameraWidth > 0) { "cameraWidth must be > 0." }
        require(cameraHeight > 0) { "cameraHeight must be > 0." }
        require(previewWidth > 0) { "previewWidth must be > 0." }
        require(previewHeight > 0) { "previewHeight must be > 0." }
        require(encoderWidth > 0) { "encoderWidth must be > 0." }
        require(encoderHeight > 0) { "encoderHeight must be > 0." }
        require(encoderWidth % 2 == 0) { "encoderWidth must be even." }
        require(encoderHeight % 2 == 0) { "encoderHeight must be even." }

        val previewAspect = previewWidth.toDouble() / previewHeight.toDouble()
        val encoderAspect = encoderWidth.toDouble() / encoderHeight.toDouble()
        require(previewAspect.isFinite()) { "preview aspect ratio must be finite." }
        require(encoderAspect.isFinite()) { "encoder aspect ratio must be finite." }
        require(cameraWidth.toDouble() > 0.0 && cameraHeight.toDouble() > 0.0)
        require(cropMode == CameraPipelineCropMode.FILL_CENTER_CROP || cropMode == CameraPipelineCropMode.FIT)
    }
}

enum class CameraPipelineCropMode {
    FIT,
    FILL_CENTER_CROP
}
