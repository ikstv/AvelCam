package com.avelcam.android.camera.pipeline.surface

internal class ObservableCameraInputSurfaceFactory(
    private val delegate: CameraInputSurfaceFactory = DefaultCameraInputSurfaceFactory(),
) : CameraInputSurfaceFactory {
    private var onSurfaceCreated: ((CameraInputSurface) -> Unit)? = null
    private var lastSurface: CameraInputSurface? = null

    override fun create(resolution: CameraSurfaceRequestResolution): CameraInputSurface {
        val surface = try {
            delegate.create(resolution)
        } catch (failure: CameraInputSurfaceFailure) {
            throw failure
        } as? CameraInputSurface ?: throw CameraInputSurfaceFailure.AllocationFailure(
            width = resolution.width,
            height = resolution.height,
            reason = "Surface factory did not return CameraInputSurface.",
            failure = IllegalStateException("Surface factory did not return CameraInputSurface."),
        )
        onSurfaceCreated?.invoke(surface)
        lastSurface = surface
        return surface
    }

    fun setListener(listener: (CameraInputSurface) -> Unit) {
        onSurfaceCreated = listener
        lastSurface?.let(listener)
    }

    fun clearListener() {
        onSurfaceCreated = null
    }

    fun clearSurface() {
        // CameraX owns the supplied Surface after provideSurface(). The provider
        // releases it only after the SurfaceRequest result reaches a terminal state.
        lastSurface = null
    }
}
