package com.avelcam.android.camera

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner

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

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )

    DisposableEffect(selectedLens, lifecycleOwner) {
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val executor = ContextCompat.getMainExecutor(context)

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
                )
            },
            executor
        )

        onDispose {
            disposed = true
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
    onError: (String?) -> Unit
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

        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            selector,
            preview
        )
        onError(null)
    } catch (error: Exception) {
        onError(error.localizedMessage ?: "Failed to initialize camera preview.")
    }
}
