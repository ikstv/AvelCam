package com.avelcam.android.camera.pipeline

enum class CameraEncoderPipelineState {
    IDLE,
    STARTING_PREVIEW,
    PREVIEW_RUNNING,
    STARTING_ENCODER,
    ENCODING,
    STOPPING_ENCODER,
    STOPPING_PREVIEW,
    STOPPED,
    ERROR,
    RELEASED
}

