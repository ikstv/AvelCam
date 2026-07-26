package com.avelcam.android.encoder

import org.junit.Assert.assertEquals
import org.junit.Test

class EncoderStateMachineTest {
    @Test
    fun startFromIdleTransitionsToConfiguring() {
        val machine = EncoderStateMachine()
        machine.apply(StartEvent)
        assertEquals(EncoderState.CONFIGURING, machine.getState())
    }

    @Test
    fun stopFromRunningTransitionsToStopping() {
        val machine = EncoderStateMachine()
        machine.apply(StartEvent)
        machine.apply(ErrorEvent)
        assertEquals(EncoderState.ERROR, machine.getState())
        machine.apply(ReleaseEvent)
        assertEquals(EncoderState.RELEASED, machine.getState())
    }

    @Test
    fun stopFromStoppedTransitionsToReleased() {
        val machine = EncoderStateMachine()
        machine.apply(ReleaseEvent)
        assertEquals(EncoderState.RELEASED, machine.getState())
        machine.apply(StartEvent)
        assertEquals(EncoderState.RELEASED, machine.getState())
    }
}

