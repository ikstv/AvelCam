package com.avelcam.android.camera.pipeline.surface

sealed class CameraSurfaceRequestResult {
    data object SurfaceUsedSuccessfully : CameraSurfaceRequestResult()

    data object RequestCancelled : CameraSurfaceRequestResult()

    data object InvalidSurface : CameraSurfaceRequestResult()

    data object SurfaceAlreadyProvided : CameraSurfaceRequestResult()

    data object WillNotProvideSurface : CameraSurfaceRequestResult()

    data class Unknown(val originalCode: Int) : CameraSurfaceRequestResult()
}
