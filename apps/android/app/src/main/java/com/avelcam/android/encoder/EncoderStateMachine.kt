package com.avelcam.android.encoder

sealed interface EncoderEvent

object StartEvent : EncoderEvent
object StopEvent : EncoderEvent
object ReleaseEvent : EncoderEvent
object ErrorEvent : EncoderEvent

class EncoderStateMachine {
    private var state: EncoderState = EncoderState.IDLE

    fun getState(): EncoderState = state

    fun apply(event: EncoderEvent): EncoderState {
        state = when (state) {
            EncoderState.IDLE -> when (event) {
                is StartEvent -> EncoderState.CONFIGURING
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.CONFIGURING -> when (event) {
                is ErrorEvent -> EncoderState.ERROR
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.CONFIGURED -> when (event) {
                is StartEvent -> EncoderState.RUNNING
                is ErrorEvent -> EncoderState.ERROR
                is StopEvent -> EncoderState.STOPPED
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.RUNNING -> when (event) {
                is StopEvent -> EncoderState.STOPPING
                is ErrorEvent -> EncoderState.ERROR
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.STOPPING -> when (event) {
                is StopEvent -> EncoderState.STOPPED
                is ReleaseEvent -> EncoderState.RELEASED
                is ErrorEvent -> EncoderState.ERROR
                else -> state
            }
            EncoderState.STOPPED -> when (event) {
                is StartEvent -> EncoderState.CONFIGURING
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.ERROR -> when (event) {
                is ReleaseEvent -> EncoderState.RELEASED
                else -> state
            }
            EncoderState.RELEASED -> when (event) {
                else -> state
            }
        }
        return state
    }
}

