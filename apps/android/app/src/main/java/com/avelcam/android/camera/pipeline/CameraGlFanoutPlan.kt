package com.avelcam.android.camera.pipeline

data class CameraGlFanoutPlan(
    val outputs: List<CameraGlFanoutOutputSpec>
) {
    init {
        require(outputs.isNotEmpty()) { "At least one fan-out output is required." }

        val hasPreview = outputs.any { it.role == CameraGlFanoutOutputRole.PREVIEW }
        val hasEncoder = outputs.any { it.role == CameraGlFanoutOutputRole.ENCODER }
        require(hasPreview) { "Preview output is required." }
        require(hasEncoder) { "Encoder output is required." }
    }
}

fun CameraEncoderPipelineConfig.toGlFanoutOutputSpecs(): CameraGlFanoutPlan {
    return CameraGlFanoutPlan(
        outputs = listOf(
            CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.PREVIEW,
                width = previewWidth,
                height = previewHeight
            ),
            CameraGlFanoutOutputSpec(
                role = CameraGlFanoutOutputRole.ENCODER,
                width = encoderWidth,
                height = encoderHeight
            )
        )
    )
}
