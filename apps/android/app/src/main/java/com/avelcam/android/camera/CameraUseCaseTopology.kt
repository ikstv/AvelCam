package com.avelcam.android.camera

internal enum class CameraUseCaseRole {
    FANOUT_PREVIEW,
    IMAGE_ANALYSIS,
}

/** CameraX topology for the GL fan-out path: never bind a second direct display Preview. */
internal fun fanoutCameraUseCaseRoles(): List<CameraUseCaseRole> = listOf(
    CameraUseCaseRole.FANOUT_PREVIEW,
    CameraUseCaseRole.IMAGE_ANALYSIS,
)
