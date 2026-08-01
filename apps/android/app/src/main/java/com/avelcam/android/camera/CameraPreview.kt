package com.avelcam.android.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.avelcam.android.camera.pipeline.CameraFrameMetadata
import com.avelcam.android.camera.pipeline.CameraGlFanoutRuntime
import com.avelcam.android.camera.pipeline.CameraGlFanoutRuntimeConfig
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun CameraPreview(
    selectedLens: Int,
    onAvailabilityUpdated: (hasRearCamera: Boolean, hasFrontCamera: Boolean) -> Unit,
    onError: (String?) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val runtime = remember { CameraGlFanoutRuntime() }
    val latestOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )

    DisposableEffect(selectedLens, lifecycleOwner) {
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                bindPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    previewView = previewView,
                    selectedLens = selectedLens,
                    onAvailabilityUpdated = onAvailabilityUpdated,
                    onError = onError,
                    runtime = runtime,
                    analysisExecutor = analysisExecutor,
                    onRuntimeError = { message ->
                        latestOnError(message)
                    },
                )
            },
            executor
        )

        onDispose {
            disposed = true
            runtime.stop()
            runtime.release()
            analysisExecutor.shutdown()
            if (providerFuture.isDone) {
                try {
                    providerFuture.get().unbindAll()
                } catch (_: Exception) {
                }
            }
        }
    }
}

private fun bindPreview(
    context: Context,
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    selectedLens: Int,
    onAvailabilityUpdated: (hasRearCamera: Boolean, hasFrontCamera: Boolean) -> Unit,
    onError: (String?) -> Unit,
    runtime: CameraGlFanoutRuntime,
    analysisExecutor: Executor,
    onRuntimeError: (String) -> Unit,
) {
    try {
        val provider = ProcessCameraProvider.getInstance(context).get()
        val hasRear = provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFront = provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
        onAvailabilityUpdated(hasRear, hasFront)

        if (!hasRear && !hasFront) {
            onError("No camera devices are available.")
            return
        }

        val selector = if (selectedLens == CameraSelector.LENS_FACING_FRONT) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        if (!provider.hasCamera(selector)) {
            onError("Selected camera is not available.")
            return
        }

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(analysisExecutor, FanoutFrameAnalyzer(
                    selectedLens = selectedLens,
                    runtime = runtime,
                    onError = onRuntimeError,
                ))
            }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview,
            imageAnalysis,
        )
        onError(null)
    } catch (error: Exception) {
        onError(error.localizedMessage ?: "Failed to initialize camera preview.")
    }
}

private class FanoutFrameAnalyzer(
    private val selectedLens: Int,
    private val runtime: CameraGlFanoutRuntime,
    private val onError: (String) -> Unit,
) : ImageAnalysis.Analyzer {
    override fun analyze(image: ImageProxy) {
        try {
            ensureRuntimeRunning(image.width, image.height)
            runtime.onCameraFrame(
                sourceWidth = image.width,
                sourceHeight = image.height,
                metadata = CameraFrameMetadata(
                    sourceTimestampNs = normalizeTimestamp(image.imageInfo.timestamp),
                    mappedTimestampNs = 0L,
                    rotationDegrees = normalizeRotation(image.imageInfo.rotationDegrees),
                    isFrontCamera = selectedLens == CameraSelector.LENS_FACING_FRONT,
                    surfaceTextureTransformMatrix = IDENTITY_SURFACE_TEXTURE_MATRIX,
                )
            )
        } catch (error: Throwable) {
            onError(error.localizedMessage ?: "Frame processing failed")
        } finally {
            image.close()
        }
    }

    private fun ensureRuntimeRunning(sourceWidth: Int, sourceHeight: Int) {
        if (!runtime.isConfigured()) {
            runtime.configure(
                CameraGlFanoutRuntimeConfig(
                    cameraWidth = sourceWidth,
                    cameraHeight = sourceHeight,
                    previewWidth = sourceWidth,
                    previewHeight = sourceHeight,
                    encoderWidth = selectEven(sourceWidth.coerceAtMost(1280)),
                    encoderHeight = selectEven(sourceHeight.coerceAtMost(720)),
                    frontCameraPreviewMirrored = selectedLens == CameraSelector.LENS_FACING_FRONT,
                    frontCameraEncoderMirrored = selectedLens == CameraSelector.LENS_FACING_FRONT,
                )
            )
        }

        if (!runtime.isRunning()) {
            val startResult = runtime.start()
            if (startResult.isFailure) {
                throw startResult.exceptionOrNull() ?: IllegalStateException("Runtime failed to start.")
            }
        }
    }

    private fun normalizeTimestamp(timestampNs: Long): Long {
        return if (timestampNs > 0L) timestampNs else System.nanoTime()
    }

    private fun normalizeRotation(rawRotation: Int): Int {
        return when (rawRotation) {
            0, 90, 180, 270 -> rawRotation
            else -> 0
        }
    }

    private fun selectEven(value: Int): Int {
        val safeValue = value.coerceAtLeast(2)
        return if (safeValue % 2 == 0) safeValue else safeValue + 1
    }
}

private val IDENTITY_SURFACE_TEXTURE_MATRIX = FloatArray(16) { index -> if (index % 5 == 0) 1f else 0f }

