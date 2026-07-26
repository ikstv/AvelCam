package com.avelcam.android.camera.pipeline

data class CameraPipelineStatistics(
    val state: CameraEncoderPipelineState,
    val cameraCallbacksReceived: Long,
    val renderTasksScheduled: Long,
    val callbacksCoalesced: Long,
    val framesRendered: Long,
    val previewFramesRendered: Long,
    val encoderFramesRendered: Long,
    val timestampCorrections: Long,
    val renderFailures: Long,
    val previewSwapFailures: Long,
    val encoderSwapFailures: Long,
    val lastCameraTimestampNs: Long,
    val lastMappedTimestampNs: Long,
    val lastError: String?
) {
    companion object {
        val Empty = CameraPipelineStatistics(
            state = CameraEncoderPipelineState.IDLE,
            cameraCallbacksReceived = 0,
            renderTasksScheduled = 0,
            callbacksCoalesced = 0,
            framesRendered = 0,
            previewFramesRendered = 0,
            encoderFramesRendered = 0,
            timestampCorrections = 0,
            renderFailures = 0,
            previewSwapFailures = 0,
            encoderSwapFailures = 0,
            lastCameraTimestampNs = -1,
            lastMappedTimestampNs = -1,
            lastError = null
        )
    }
}

