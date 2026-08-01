package com.avelcam.android.camera

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.avelcam.android.camera.pipeline.CameraEncoderOutputManager
import com.avelcam.android.camera.pipeline.CameraFrameCoalescer
import com.avelcam.android.camera.pipeline.CameraFrameMetadata
import com.avelcam.android.camera.pipeline.CameraGlFanoutController
import com.avelcam.android.camera.pipeline.CameraGlFanoutRuntime
import com.avelcam.android.camera.pipeline.CameraGlFanoutRuntimeConfig
import com.avelcam.android.camera.pipeline.CameraGlFanoutOutputRole
import com.avelcam.android.camera.pipeline.CameraGlFanoutOutputSpec
import com.avelcam.android.camera.pipeline.EncoderSurfaceGlDestination
import com.avelcam.android.camera.pipeline.PreviewSurfaceGlDestination
import com.avelcam.android.camera.pipeline.asContract
import com.avelcam.android.camera.pipeline.surface.CameraInputSurface
import com.avelcam.android.camera.pipeline.surface.CameraInputSurfaceMode
import com.avelcam.android.camera.pipeline.surface.CameraInputSurfaceFactoryOwner
import com.avelcam.android.camera.pipeline.surface.CameraSurfaceProvider
import com.avelcam.android.encoder.gl.EglInputSurface
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference

@Composable
internal fun CameraPreview(
    selectedLens: Int,
    onAvailabilityUpdated: (hasRearCamera: Boolean, hasFrontCamera: Boolean) -> Unit,
    onError: (String?) -> Unit,
    cameraInputSurfaceMode: CameraInputSurfaceMode = CameraInputSurfaceMode.DEFAULT,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val destinationSurfaceFactory = remember {
        AtomicReference<(Surface) -> EglInputSurface> { surface ->
            EglInputSurface(surface)
        }
    }
    val runtime = remember(destinationSurfaceFactory) {
        CameraGlFanoutRuntime(
            controller = CameraGlFanoutController(
                outputManagerFactory = { coordinator, config, sink, _ ->
                    CameraEncoderOutputManager(
                        encoderConfig = config,
                        sink = sink,
                        coordinator = coordinator,
                        destinationFactory = { spec, surface ->
                            EncoderSurfaceGlDestination(
                                spec = spec,
                                surface = surface,
                                createEglSurface = { destinationSurface ->
                                    destinationSurfaceFactory.get().invoke(destinationSurface)
                                },
                            )
                        },
                    ).asContract
                }
            )
        )
    }
    val latestOnError by rememberUpdatedState(onError)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView }
    )

    DisposableEffect(selectedLens, lifecycleOwner, cameraInputSurfaceMode) {
        var disposed = false
        val providerFuture = ProcessCameraProvider.getInstance(context)
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val analysisExecutor = Executors.newSingleThreadExecutor()
        val previewDestination = FanoutPreviewDestinationRegistry(destinationSurfaceFactory)
        val frameBridge = FanoutSurfaceFrameBridge(runtime = runtime, analysisExecutor = analysisExecutor)
        var frameAnalyzer: FanoutFrameAnalyzer? = null
        var surfaceFactoryOwner: CameraInputSurfaceFactoryOwner? = null
        var surfaceProvider: CameraSurfaceProvider? = null

        providerFuture.addListener(
            {
                if (disposed) return@addListener
                bindPreview(
                    context = context,
                    lifecycleOwner = lifecycleOwner,
                    selectedLens = selectedLens,
                    onAvailabilityUpdated = onAvailabilityUpdated,
                    onError = onError,
                    runtime = runtime,
                    analysisExecutor = analysisExecutor,
                    previewDestination = previewDestination,
                    frameBridge = frameBridge,
                    onAnalyzerCreated = { analyzer -> frameAnalyzer = analyzer },
                    onSurfaceFactoryOwnerCreated = { owner ->
                        surfaceFactoryOwner = owner
                        destinationSurfaceFactory.set(
                            owner.eglContext?.let { eglContext ->
                                { destinationSurface ->
                                    eglContext.createInputSurface(destinationSurface)
                                }
                            } ?: { destinationSurface -> EglInputSurface(destinationSurface) }
                        )
                    },
                    onSurfaceProviderCreated = { provider ->
                        surfaceProvider = provider
                    },
                    cameraInputSurfaceMode = cameraInputSurfaceMode,
                    onRuntimeError = { message ->
                        latestOnError(message)
                    },
                )
            },
            mainExecutor
        )

        onDispose {
            disposed = true
            runtime.stop()
            frameBridge.release()
            frameAnalyzer?.release()
            frameAnalyzer = null
            previewDestination.release(runtime)
            surfaceFactoryOwner?.close()
            surfaceFactoryOwner = null
            surfaceProvider?.release()
            surfaceProvider = null
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
    selectedLens: Int,
    onAvailabilityUpdated: (hasRearCamera: Boolean, hasFrontCamera: Boolean) -> Unit,
    onError: (String?) -> Unit,
    runtime: CameraGlFanoutRuntime,
    analysisExecutor: Executor,
    previewDestination: FanoutPreviewDestinationRegistry,
    frameBridge: FanoutSurfaceFrameBridge,
    onAnalyzerCreated: (FanoutFrameAnalyzer) -> Unit,
    onSurfaceProviderCreated: (CameraSurfaceProvider) -> Unit,
    cameraInputSurfaceMode: CameraInputSurfaceMode,
    onSurfaceFactoryOwnerCreated: (CameraInputSurfaceFactoryOwner) -> Unit,
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

        val surfaceProvider = CameraSurfaceProvider(
            callbackExecutor = analysisExecutor,
            surfaceFactory = surfaceFactory,
            transformationObserver = { _, transform ->
                frameBridge.updateSurfaceRotation(transform.rotationDegrees)
            },
        )
        onSurfaceProviderCreated(surfaceProvider)

        val preview = androidx.camera.core.Preview.Builder().build().also {
            it.setSurfaceProvider(surfaceProvider)
        }

        val surfaceFactoryOwner = runCatching {
            CameraInputSurfaceFactoryOwner.create(cameraInputSurfaceMode)
        }.getOrElse { error ->
            if (cameraInputSurfaceMode != CameraInputSurfaceMode.EGL) {
                throw error
            }
            onRuntimeError(
                "EGL camera input surface mode failed, falling back to default. " +
                    "Reason: ${error.message ?: "unknown"}"
            )
            CameraInputSurfaceFactoryOwner.create(CameraInputSurfaceMode.DEFAULT)
        }
        val surfaceFactory = surfaceFactoryOwner.factory
        onSurfaceFactoryOwnerCreated(surfaceFactoryOwner)
        surfaceFactory.setListener { surface ->
            frameBridge.bindSurface(surface, selectedLens == CameraSelector.LENS_FACING_FRONT)
            frameBridge.start()
        }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                val analyzer = FanoutFrameAnalyzer(
                    selectedLens = selectedLens,
                    runtime = runtime,
                    previewDestination = previewDestination,
                    onError = onRuntimeError,
                    analysisExecutor = analysisExecutor,
                    onMetadata = { metadata ->
                        frameBridge.updateMetadata(metadata)
                    },
                )
                onAnalyzerCreated(analyzer)
                it.setAnalyzer(analysisExecutor, analyzer)
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
    private val previewDestination: FanoutPreviewDestinationRegistry,
    private val onError: (String) -> Unit,
    analysisExecutor: Executor,
    private val onMetadata: (CameraFrameMetadata) -> Unit,
) : ImageAnalysis.Analyzer {
    private val lock = Any()
    private val frameCoalescer = CameraFrameCoalescer { analysisExecutor.execute { configureRuntimeIfNeeded() } }
    private var latestFrame: CameraGlFanoutFramePayload? = null

    private data class CameraGlFanoutFramePayload(
        val sourceWidth: Int,
        val sourceHeight: Int,
        val metadata: CameraFrameMetadata,
    )

    override fun analyze(image: ImageProxy) {
        try {
            previewDestination.ensureDestinationRegistered(runtime, image.width, image.height)
            val metadata = CameraFrameMetadata(
                sourceTimestampNs = normalizeTimestamp(image.imageInfo.timestamp),
                mappedTimestampNs = 0L,
                rotationDegrees = normalizeRotation(image.imageInfo.rotationDegrees),
                isFrontCamera = selectedLens == CameraSelector.LENS_FACING_FRONT,
                surfaceTextureTransformMatrix = IDENTITY_SURFACE_TEXTURE_MATRIX,
            )
            onMetadata(metadata)

            synchronized(lock) {
                latestFrame = CameraGlFanoutFramePayload(
                    sourceWidth = image.width,
                    sourceHeight = image.height,
                    metadata = metadata,
                )
            }
            frameCoalescer.onFrameAvailable()
        } catch (error: Throwable) {
            onError(error.localizedMessage ?: "Frame processing failed")
        } finally {
            image.close()
        }
    }

    private fun configureRuntimeIfNeeded() {
        val frame = synchronized(lock) {
            val pending = latestFrame
            latestFrame = null
            pending
        } ?: return

        try {
            ensureRuntimeRunning(frame.sourceWidth, frame.sourceHeight)
        } catch (error: Throwable) {
            onError(error.localizedMessage ?: "Frame processing failed")
        } finally {
            frameCoalescer.onRenderCompleted()
        }
    }

    fun release() {
        frameCoalescer.release()
    }

    private fun ensureRuntimeRunning(sourceWidth: Int, sourceHeight: Int) {
        synchronized(lock) {
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

private class FanoutSurfaceFrameBridge(
    private val runtime: CameraGlFanoutRuntime,
    private val analysisExecutor: Executor,
) {
    private val lock = Any()
    private val frameCoalescer = CameraFrameCoalescer { analysisExecutor.execute { onFrameAvailable() } }
    private var activeSurfaceTexture: SurfaceTexture? = null
    private var latestMetadata: CameraFrameMetadata? = null
    private var latestSurfaceTransform: FloatArray = IDENTITY_SURFACE_TEXTURE_MATRIX.copyOf()
    private var surfaceRotationDegrees: Int = 0
    private var lastSourceWidth: Int = 0
    private var lastSourceHeight: Int = 0
    private var activeSourceTextureId: Int = 0
    private var isFrontCamera: Boolean = false

    fun bindSurface(surface: CameraInputSurface, isFrontCamera: Boolean) {
        synchronized(lock) {
            activeSurfaceTexture?.setOnFrameAvailableListener(null)
            this.isFrontCamera = isFrontCamera
            activeSurfaceTexture = surface.surfaceTexture
            activeSourceTextureId = surface.sourceTextureId
            lastSourceWidth = surface.resolution.width
            lastSourceHeight = surface.resolution.height
            activeSurfaceTexture?.setOnFrameAvailableListener {
                val nextTransform = FloatArray(16)
                activeSurfaceTexture?.getTransformMatrix(nextTransform)
                synchronized(lock) {
                    latestSurfaceTransform = nextTransform
                }
                frameCoalescer.onFrameAvailable()
            }
        }
    }

    fun updateMetadata(metadata: CameraFrameMetadata) {
        synchronized(lock) {
            val rotation = if (surfaceRotationDegrees == 0) metadata.rotationDegrees else surfaceRotationDegrees
            val surfaceTransform = latestSurfaceTransform.copyOf()
            latestMetadata = CameraFrameMetadata(
                sourceTimestampNs = metadata.sourceTimestampNs,
                mappedTimestampNs = metadata.mappedTimestampNs,
                rotationDegrees = normalizeRotation(rotation),
                isFrontCamera = metadata.isFrontCamera,
                surfaceTextureTransformMatrix = surfaceTransform,
            )
        }
    }

    fun updateSurfaceRotation(rotationDegrees: Int) {
        synchronized(lock) {
            surfaceRotationDegrees = normalizeRotation(rotationDegrees)
        }
    }

    fun start() {
        synchronized(lock) {
            // frame processing will run when CameraSurface delivers frames
        }
    }

    private fun onFrameAvailable() {
        val texture = synchronized(lock) {
            activeSurfaceTexture ?: return
        }

        if (!runtime.isRunning()) {
            frameCoalescer.onRenderCompleted()
            return
        }

        val metadata = synchronized(lock) {
            latestMetadata
        } ?: run {
            frameCoalescer.onRenderCompleted()
            return
        }

        try {
            texture.updateTexImage()
            val textureTimestamp = texture.timestamp
            runtime.onCameraFrame(
                sourceWidth = lastSourceWidth.coerceAtLeast(1),
                sourceHeight = lastSourceHeight.coerceAtLeast(1),
                metadata = CameraFrameMetadata(
                    sourceTimestampNs = textureTimestamp.takeIf { it > 0L } ?: metadata.sourceTimestampNs,
                    mappedTimestampNs = metadata.mappedTimestampNs,
                    rotationDegrees = metadata.rotationDegrees,
                    isFrontCamera = isFrontCamera,
                    surfaceTextureTransformMatrix = latestSurfaceTransform.copyOf(),
                ),
                sourceTextureId = activeSourceTextureId,
            )
        } finally {
            frameCoalescer.onRenderCompleted()
        }
    }

    fun release() {
        synchronized(lock) {
            activeSurfaceTexture?.setOnFrameAvailableListener(null)
            activeSurfaceTexture = null
            activeSourceTextureId = 0
            frameCoalescer.release()
        }
    }
}

private fun normalizeRotation(rawRotation: Int): Int {
    return when (rawRotation) {
        0, 90, 180, 270 -> rawRotation
        else -> 0
    }
}

private class FanoutPreviewDestinationRegistry(
    private val destinationSurfaceFactory: AtomicReference<(Surface) -> EglInputSurface>,
) {
    private val lock = Any()
    private var surfaceTexture: SurfaceTexture? = null
    private var destination: PreviewSurfaceGlDestination? = null
    private var destinationWidth: Int = 0
    private var destinationHeight: Int = 0

    fun ensureDestinationRegistered(runtime: CameraGlFanoutRuntime, width: Int, height: Int) {
        synchronized(lock) {
            val safeWidth = width.coerceAtLeast(1)
            val safeHeight = height.coerceAtLeast(1)

            if (destination != null && destinationWidth == safeWidth && destinationHeight == safeHeight) {
                return
            }

            val currentDestination = destination
            if (currentDestination != null) {
                runtime.unregisterPreviewDestination(currentDestination)
                currentDestination.release()
                destination = null
                surfaceTexture?.release()
                surfaceTexture = null
            }

            val nextTexture = SurfaceTexture(0).also {
                it.setDefaultBufferSize(safeWidth, safeHeight)
            }
            val nextDestination = PreviewSurfaceGlDestination(
                CameraGlFanoutOutputSpec(
                    role = CameraGlFanoutOutputRole.PREVIEW,
                    width = safeWidth,
                    height = safeHeight,
                ),
                Surface(nextTexture),
                createEglSurface = { surface ->
                    destinationSurfaceFactory.get().invoke(surface)
                },
            )

            runtime.registerPreviewDestination(nextDestination)
            this.surfaceTexture = nextTexture
            this.destination = nextDestination
            this.destinationWidth = safeWidth
            this.destinationHeight = safeHeight
        }
    }

    fun release(runtime: CameraGlFanoutRuntime) {
        synchronized(lock) {
            val currentDestination = destination
            if (currentDestination == null) {
                return
            }

            runtime.unregisterPreviewDestination(currentDestination)
            currentDestination.release()
            surfaceTexture?.release()
            destination = null
            surfaceTexture = null
            destinationWidth = 0
            destinationHeight = 0
        }
    }
}

private val IDENTITY_SURFACE_TEXTURE_MATRIX = FloatArray(16) { index -> if (index % 5 == 0) 1f else 0f }
