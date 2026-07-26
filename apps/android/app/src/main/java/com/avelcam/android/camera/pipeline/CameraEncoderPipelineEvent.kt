package com.avelcam.android.camera.pipeline

sealed interface CameraEncoderPipelineEvent {
    data class ErrorOccurred(val reason: String, val category: String? = null) : CameraEncoderPipelineEvent
    data class ErrorCleared(val reason: String? = null) : CameraEncoderPipelineEvent
}

data object StartPreviewRequested : CameraEncoderPipelineEvent
data object PreviewStarted : CameraEncoderPipelineEvent
data object StartEncoderRequested : CameraEncoderPipelineEvent
data object EncoderStarted : CameraEncoderPipelineEvent
data object StopEncoderRequested : CameraEncoderPipelineEvent
data object StopEncoderCompleted : CameraEncoderPipelineEvent
data object StopPreviewRequested : CameraEncoderPipelineEvent
data object PreviewStopped : CameraEncoderPipelineEvent
data object ReleaseRequested : CameraEncoderPipelineEvent

