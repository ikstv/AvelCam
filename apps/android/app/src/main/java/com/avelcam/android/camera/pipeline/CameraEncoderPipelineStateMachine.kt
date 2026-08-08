package com.avelcam.android.camera.pipeline

data class CameraEncoderPipelineTransitionResult(
    val state: CameraEncoderPipelineState,
    val changed: Boolean,
    val error: String? = null
)

class CameraEncoderPipelineStateMachine(
    private val initialState: CameraEncoderPipelineState = CameraEncoderPipelineState.IDLE
) {
    private var state: CameraEncoderPipelineState = initialState

    fun getState(): CameraEncoderPipelineState = state

    fun transition(event: CameraEncoderPipelineEvent): CameraEncoderPipelineTransitionResult {
        val nextState = when (state) {
            CameraEncoderPipelineState.IDLE -> when (event) {
                is StartPreviewRequested -> CameraEncoderPipelineState.STARTING_PREVIEW
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                else -> state
            }

            CameraEncoderPipelineState.STARTING_PREVIEW -> when (event) {
                is PreviewStarted -> CameraEncoderPipelineState.PREVIEW_RUNNING
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.PREVIEW_RUNNING -> when (event) {
                is StartEncoderRequested -> CameraEncoderPipelineState.STARTING_ENCODER
                is StopPreviewRequested -> CameraEncoderPipelineState.STOPPING_PREVIEW
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.STARTING_ENCODER -> when (event) {
                is EncoderStarted -> CameraEncoderPipelineState.ENCODING
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is StopEncoderRequested -> CameraEncoderPipelineState.STOPPING_ENCODER
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.ENCODING -> when (event) {
                is StopEncoderRequested -> CameraEncoderPipelineState.STOPPING_ENCODER
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.STOPPING_ENCODER -> when (event) {
                is StopEncoderCompleted -> CameraEncoderPipelineState.PREVIEW_RUNNING
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.STOPPING_PREVIEW -> when (event) {
                is PreviewStopped -> CameraEncoderPipelineState.STOPPED
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.STOPPED -> when (event) {
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                is StartPreviewRequested -> CameraEncoderPipelineState.STARTING_PREVIEW
                is StartEncoderRequested -> state
                is CameraEncoderPipelineEvent.ErrorOccurred -> CameraEncoderPipelineState.ERROR
                else -> state
            }

            CameraEncoderPipelineState.ERROR -> when (event) {
                is CameraEncoderPipelineEvent.ErrorCleared -> state
                is ReleaseRequested -> CameraEncoderPipelineState.RELEASED
                else -> state
            }

            CameraEncoderPipelineState.RELEASED -> state
        }

        val error = if (nextState == state && !isIdentityTransitionAllowed(state, event)) {
            when (event) {
                is CameraEncoderPipelineEvent.ErrorOccurred -> event.category ?: "state_transition_denied"
                else -> "state_transition_denied"
            }
        } else null

        if (nextState != state) {
            state = nextState
            return CameraEncoderPipelineTransitionResult(state = state, changed = true)
        }
        return CameraEncoderPipelineTransitionResult(
            state = state,
            changed = false,
            error = error
        )
    }

    private fun isIdentityTransitionAllowed(state: CameraEncoderPipelineState, event: CameraEncoderPipelineEvent): Boolean {
        return when (state) {
            CameraEncoderPipelineState.IDLE -> event is ReleaseRequested
            CameraEncoderPipelineState.STARTING_PREVIEW -> event is CameraEncoderPipelineEvent.ErrorOccurred
            CameraEncoderPipelineState.PREVIEW_RUNNING ->
                event is StopEncoderRequested ||
                    event is CameraEncoderPipelineEvent.ErrorOccurred ||
                    event is StopPreviewRequested
            CameraEncoderPipelineState.STARTING_ENCODER,
            CameraEncoderPipelineState.ENCODING -> event is StopEncoderRequested
            CameraEncoderPipelineState.STOPPING_ENCODER,
            CameraEncoderPipelineState.STOPPING_PREVIEW,
            CameraEncoderPipelineState.STOPPED -> event is CameraEncoderPipelineEvent.ErrorOccurred
            CameraEncoderPipelineState.ERROR -> event is ReleaseRequested
            CameraEncoderPipelineState.RELEASED -> false
        }
    }
}

