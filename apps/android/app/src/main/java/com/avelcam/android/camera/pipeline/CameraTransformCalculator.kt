package com.avelcam.android.camera.pipeline

import kotlin.math.max
import kotlin.math.min

/**
 * Texture transform math for CameraX SurfaceTexture matrices + logical rotation/mirror.
 *
 * Convention:
 * - Matrices are stored in row-major order.
 * - Points are column vectors [x, y, 0, 1]^T.
 * - Matrix order follows left-to-right composition through multiply(left, right).
 * - Multiplication result multiplies matrices so right-most transform applies first.
 */

data class CameraTransformInput(
    val sourceWidth: Int,
    val sourceHeight: Int,
    val destinationWidth: Int,
    val destinationHeight: Int,
    val rotationDegrees: Int,
    val mirror: Boolean,
    val cropMode: CameraPipelineCropMode,
    val surfaceTextureMatrix: FloatArray
)

class CameraTransformCalculator {
    fun calculate(input: CameraTransformInput): CameraTextureTransform {
        require(input.sourceWidth > 0)
        require(input.sourceHeight > 0)
        require(input.destinationWidth > 0)
        require(input.destinationHeight > 0)
        require(input.rotationDegrees == 0 || input.rotationDegrees == 90 || input.rotationDegrees == 180 || input.rotationDegrees == 270)
        require(input.surfaceTextureMatrix.size == 16)

        val isRotated = input.rotationDegrees == 90 || input.rotationDegrees == 270
        val sourceAspect = if (isRotated) {
            input.sourceHeight.toDouble() / input.sourceWidth.toDouble()
        } else {
            input.sourceWidth.toDouble() / input.sourceHeight.toDouble()
        }
        val destinationAspect = input.destinationWidth.toDouble() / input.destinationHeight.toDouble()
        val scales = calculateCropScales(sourceAspect, destinationAspect, input.cropMode)

        var matrix = identityMatrix4()
        matrix = multiply(matrix, scaleMatrix(scales.second[0], scales.second[1]))
        matrix = multiply(matrix, rotateMatrix(input.rotationDegrees))
        if (input.mirror) {
            matrix = multiply(matrix, mirrorMatrix())
        }
        matrix = multiply(matrix, input.surfaceTextureMatrix.copyOf())

        val sourceCrop = if (input.cropMode == CameraPipelineCropMode.FIT) {
            CropRect(0f, 0f, 1f, 1f)
        } else {
            CropRect(scales.first.first.toFloat(), scales.first.second.toFloat(), scales.first.third.toFloat(), scales.first.fourth.toFloat())
        }

        return CameraTextureTransform(
            matrix = normalizeMatrix(matrix),
            sourceCrop = sourceCrop,
            destinationCrop = CropRect(0f, 0f, 1f, 1f)
        )
    }

    private fun calculateCropScales(
        sourceAspect: Double,
        destinationAspect: Double,
        mode: CameraPipelineCropMode
    ): Pair<Quad, DoubleArray> {
        return when (mode) {
            CameraPipelineCropMode.FIT -> {
                val ratio = if (sourceAspect > destinationAspect) {
                    destinationAspect / sourceAspect
                } else {
                    sourceAspect / destinationAspect
                }
                val scale = min(1.0, ratio)
                Pair(Quad(0.0, 0.0, 1.0, 1.0), doubleArrayOf(1.0, scale))
            }

            CameraPipelineCropMode.FILL_CENTER_CROP -> {
                if (sourceAspect > destinationAspect) {
                    val visibleWidth = destinationAspect / sourceAspect
                    val x0 = (1.0 - visibleWidth) / 2.0
                    val sourceCrop = Quad(x0, 0.0, x0 + visibleWidth, 1.0)
                    Pair(sourceCrop, doubleArrayOf(1.0, 1.0))
                } else {
                    val visibleHeight = sourceAspect / destinationAspect
                    val y0 = (1.0 - visibleHeight) / 2.0
                    val sourceCrop = Quad(0.0, y0, 1.0, y0 + visibleHeight)
                    Pair(sourceCrop, doubleArrayOf(1.0, 1.0))
                }
            }
        }
    }

    private fun scaleMatrix(scaleX: Double, scaleY: Double): FloatArray {
        return floatArrayOf(
            scaleX.toFloat(), 0f, 0f, 0f,
            0f, scaleY.toFloat(), 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f
        )
    }

    private fun rotateMatrix(rotationDegrees: Int): FloatArray {
        return when (rotationDegrees) {
            0 -> identityMatrix4()
            90 -> floatArrayOf(
                0f, 1f, 0f, 0f,
                -1f, 0f, 0f, 1f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
            180 -> floatArrayOf(
                -1f, 0f, 0f, 1f,
                0f, -1f, 0f, 1f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
            270 -> floatArrayOf(
                0f, -1f, 0f, 1f,
                1f, 0f, 0f, 0f,
                0f, 0f, 1f, 0f,
                0f, 0f, 0f, 1f
            )
            else -> identityMatrix4()
        }
    }

    private fun mirrorMatrix(): FloatArray = floatArrayOf(
        -1f, 0f, 0f, 1f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun identityMatrix4(): FloatArray = floatArrayOf(
        1f, 0f, 0f, 0f,
        0f, 1f, 0f, 0f,
        0f, 0f, 1f, 0f,
        0f, 0f, 0f, 1f
    )

    private fun normalizeMatrix(matrix: FloatArray): FloatArray {
        return matrix.map {
            require(it.isFinite())
            it
        }.toFloatArray()
    }

    private fun multiply(left: FloatArray, right: FloatArray): FloatArray {
        val result = FloatArray(16)
        for (row in 0..3) {
            for (col in 0..3) {
                var value = 0f
                for (i in 0..3) {
                    value += left[row * 4 + i] * right[i * 4 + col]
                }
                result[row * 4 + col] = value
            }
        }
        return result
    }
}

internal fun mapPoint(matrix: FloatArray, x: Float, y: Float): Pair<Float, Float> {
    val mappedX = matrix[0] * x + matrix[1] * y + matrix[3]
    val mappedY = matrix[4] * x + matrix[5] * y + matrix[7]
    return mappedX to mappedY
}

private data class Quad(val first: Double, val second: Double, val third: Double, val fourth: Double)
