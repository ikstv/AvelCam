package com.avelcam.android.encoder.diagnostic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun EncoderDiagnosticPanel() {
    var message by remember { mutableStateOf("Encoder diagnostic is idle.") }
    var running by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf(EncoderDiagnosticState(selectedCodec = "not started")) }
    val harness = remember { EncoderDiagnosticHarness() }

    LaunchedEffect(running) {
        if (running) {
            while (running) {
                delay(250L)
                snapshot = harness.getState()
                message = "Last error: ${snapshot.lastError ?: "none"}"
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Encoder diagnostic")
        Card(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = """
                    Codec: ${snapshot.selectedCodec}
                    State: ${if (running) "running" else "stopped"}
                    Submitted: ${snapshot.submittedFrames}
                    Encoded units: ${snapshot.encodedUnits}
                    Keyframes: ${snapshot.keyframes}
                    Codec-config units: ${snapshot.codecConfigUnits}
                    Encoded bytes: ${snapshot.encodedBytes}
                    Output FPS: ${"%.1f".format(snapshot.outputFps)}
                    First output latency: ${snapshot.firstOutputLatencyUs?.toString() ?: "n/a"} us
                    $message
                """.trimIndent().replace("\n", "\n")
            )
        }
        Button(onClick = {
            if (!running) {
                val result = harness.start()
                running = result.isSuccess
                message = if (result.isSuccess) {
                    "Encoder started."
                } else {
                    result.exceptionOrNull()?.message ?: "Start failed."
                }
                snapshot = harness.getState()
            } else {
                val result = harness.stop()
                running = false
                message = if (result.isSuccess) "Encoder stopped." else (result.exceptionOrNull()?.message ?: "Stop failed.")
                snapshot = harness.getState()
            }
        }) {
            Text(if (running) "Stop encoder test" else "Start encoder test")
        }
        Button(onClick = {
            if (!running) {
                harness.release()
            }
        }) {
            Text("Reset harness")
        }
    }
}

