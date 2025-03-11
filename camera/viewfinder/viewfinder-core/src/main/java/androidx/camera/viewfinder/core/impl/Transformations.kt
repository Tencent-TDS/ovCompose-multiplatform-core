/*
 * Copyright 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.camera.viewfinder.core.impl

import android.graphics.Matrix
import android.graphics.RectF
import android.util.LayoutDirection
import android.util.Size
import android.util.SizeF
import android.view.Surface
import android.view.TextureView
import androidx.camera.viewfinder.core.ScaleType
import androidx.camera.viewfinder.core.TransformationInfo

// Normalized space (-1, -1) - (1, 1).
private val NORMALIZED_RECT = RectF(-1f, -1f, 1f, 1f)

object Transformations {
    /**
     * Creates a matrix that makes [TextureView]'s rotation matches the display rotation.
     *
     * The value should be applied by calling [TextureView.setTransform].
     */
    @JvmStatic
    fun getTextureViewCorrectionMatrix(
        displayRotationDegrees: Int,
        width: Int,
        height: Int
    ): Matrix {
        val surfaceRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        return getRectToRect(surfaceRect, surfaceRect, -displayRotationDegrees)
    }

    /** Converts [Surface] rotation to rotation degrees: 90, 180, 270 or 0. */
    @JvmStatic
    fun surfaceRotationToRotationDegrees(@RotationValue rotationValue: Int): Int =
        when (rotationValue) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else ->
                throw UnsupportedOperationException("Unsupported surface rotation: $rotationValue")
        }

    @JvmStatic
    fun getSurfaceToViewfinderMatrix(
        viewfinderSize: Size,
        surfaceResolution: Size,
        transformationInfo: TransformationInfo,
        layoutDirection: Int,
        scaleType: ScaleType
    ): Matrix {
        val rotatedViewportSize = transformationInfo.rotatedViewportFor(surfaceResolution)
        // Get the target of the mapping, the coordinates of the crop rect in view finder.
        val viewfinderCropRect: RectF =
            if (isViewportAspectRatioMatchViewfinder(rotatedViewportSize, viewfinderSize)) {
                // If crop rect has the same aspect ratio as view finder, scale the crop rect to
                // fill the entire view finder. This happens if the scale type is FILL_* AND a
                // view-finder-based viewport is used.
                RectF(0f, 0f, viewfinderSize.width.toFloat(), viewfinderSize.height.toFloat())
            } else {
                // If the aspect ratios don't match, it could be 1) scale type is FIT_*, 2) the
                // Viewport is not based on the view finder or 3) both.
                getViewfinderViewportRectForMismatchedAspectRatios(
                    rotatedViewportSize = rotatedViewportSize,
                    viewfinderSize = viewfinderSize,
                    layoutDirection = layoutDirection,
                    scaleType = scaleType
                )
            }

        val surfaceCropRect = transformationInfo.cropRectFor(surfaceResolution)

        val matrix =
            getRectToRect(surfaceCropRect, viewfinderCropRect, transformationInfo.sourceRotation)

        if (transformationInfo.isSourceMirroredHorizontally) {
            matrix.preScale(-1f, 1f, surfaceCropRect.centerX(), surfaceCropRect.centerY())
        }

        if (transformationInfo.isSourceMirroredVertically) {
            matrix.preScale(1f, -1f, surfaceCropRect.centerX(), surfaceCropRect.centerY())
        }
        return matrix
    }

    private fun getViewfinderViewportRectForMismatchedAspectRatios(
        rotatedViewportSize: SizeF,
        viewfinderSize: Size,
        layoutDirection: Int,
        scaleType: ScaleType
    ): RectF {
        val viewfinderRect =
            RectF(0f, 0f, viewfinderSize.width.toFloat(), viewfinderSize.height.toFloat())
        val rotatedViewportRect =
            RectF(0f, 0f, rotatedViewportSize.width, rotatedViewportSize.height)
        val matrix = Matrix()
        setMatrixRectToRect(matrix, rotatedViewportRect, viewfinderRect, scaleType)
        matrix.mapRect(rotatedViewportRect)
        return if (layoutDirection == LayoutDirection.RTL) {
            return flipHorizontally(rotatedViewportRect, viewfinderSize.width / 2f)
        } else {
            rotatedViewportRect
        }
    }

    internal fun isViewportAspectRatioMatchViewfinder(
        rotatedViewportSize: SizeF,
        viewfinderSize: Size
    ): Boolean =
        isAspectRatioMatchingWithRoundingError(
            viewfinderSize.toSizeF(),
            true,
            rotatedViewportSize,
            false
        )

    /**
     * Gets the transform from one {@link Rect} to another with rotation degrees.
     *
     * <p> Following is how the source is mapped to the target with a 90° rotation. The rect <a, b,
     * c, d> is mapped to <a', b', c', d'>.
     * <pre>
     *  a----------b               d'-----------a'
     *  |  source  |    -90°->     |            |
     *  d----------c               |   target   |
     *                             |            |
     *                             c'-----------b'
     * </pre>
     */
    private fun getRectToRect(source: RectF, target: RectF, rotationDegrees: Int): Matrix =
        Matrix().apply {
            // Map source to normalized space.
            setRectToRect(source, NORMALIZED_RECT, Matrix.ScaleToFit.FILL)
            // Add rotation.
            postRotate(rotationDegrees.toFloat())
            // Restore the normalized space to target's coordinates.
            postConcat(getNormalizedToBuffer(target))
        }

    /** Returns true if the rotation degrees is 90 or 270. */
    private fun is90or270(rotationDegrees: Int) =
        when (rotationDegrees) {
            90,
            270 -> true
            0,
            180 -> false
            else -> throw IllegalArgumentException("Invalid rotation degrees: $rotationDegrees")
        }

    private fun setMatrixRectToRect(
        matrix: Matrix,
        source: RectF,
        destination: RectF,
        scaleType: ScaleType
    ) {
        val matrixScaleType =
            when (scaleType) {
                ScaleType.FIT_CENTER,
                ScaleType.FILL_CENTER -> Matrix.ScaleToFit.CENTER
                ScaleType.FIT_END,
                ScaleType.FILL_END -> Matrix.ScaleToFit.END
                ScaleType.FIT_START,
                ScaleType.FILL_START -> Matrix.ScaleToFit.START
            }

        val isFitType =
            scaleType in setOf(ScaleType.FIT_CENTER, ScaleType.FIT_END, ScaleType.FIT_START)
        if (isFitType) {
            matrix.setRectToRect(source, destination, matrixScaleType)
        } else {
            // android.graphics.Matrix doesn't support fill scale types. The workaround is
            // mapping inversely from destination to source, then invert the matrix.
            matrix.setRectToRect(destination, source, matrixScaleType)
            matrix.invert(matrix)
        }
    }

    private fun flipHorizontally(original: RectF, flipLineX: Float): RectF =
        RectF(
            flipLineX + flipLineX - original.right,
            original.top,
            flipLineX + flipLineX - original.left,
            original.bottom
        )

    /**
     * Checks if aspect ratio matches while tolerating rounding error.
     *
     * One example of the usage is comparing the viewport-based crop rect from different use cases.
     * The crop rect is rounded because pixels are integers, which may introduce an error when we
     * check if the aspect ratio matches. For example, when Viewfinder's width/height are prime
     * numbers 601x797, the crop rect from other use cases cannot have a matching aspect ratio even
     * if they are based on the same viewport. This method checks the aspect ratio while tolerating
     * a rounding error.
     *
     * @param size1 the rounded size1
     * @param isAccurate1 if size1 is accurate. e.g. it's true if it's the PreviewView's dimension
     *   which viewport is based on
     * @param size2 the rounded size2
     * @param isAccurate2 if size2 is accurate.
     */
    private fun isAspectRatioMatchingWithRoundingError(
        size1: SizeF,
        isAccurate1: Boolean,
        size2: SizeF,
        isAccurate2: Boolean
    ): Boolean {
        // The crop rect coordinates are rounded values. Each value is at most .5 away from their
        // true values. So the width/height, which is the difference of 2 coordinates, are at most
        // 1.0 away from their true value.
        // First figure out the possible range of the aspect ratio's true value.
        val ratio1UpperBound: Float
        val ratio1LowerBound: Float
        if (isAccurate1) {
            ratio1UpperBound = size1.width / size1.height
            ratio1LowerBound = ratio1UpperBound
        } else {
            ratio1UpperBound = (size1.width + 1f) / (size1.height - 1f)
            ratio1LowerBound = (size1.width - 1f) / (size1.height + 1f)
        }
        val ratio2UpperBound: Float
        val ratio2LowerBound: Float
        if (isAccurate2) {
            ratio2UpperBound = size2.width / size2.height
            ratio2LowerBound = ratio2UpperBound
        } else {
            ratio2UpperBound = (size2.width + 1f) / (size2.height - 1f)
            ratio2LowerBound = (size2.width - 1f) / (size2.height + 1f)
        }
        // Then we check if the true value range overlaps.
        return ratio1UpperBound >= ratio2LowerBound && ratio2UpperBound >= ratio1LowerBound
    }

    /** Gets the transform from a normalized space (-1, -1) - (1, 1) to the given rect. */
    private fun getNormalizedToBuffer(viewPortRect: RectF): Matrix =
        Matrix().apply { setRectToRect(NORMALIZED_RECT, viewPortRect, Matrix.ScaleToFit.FILL) }

    private fun Size.toSizeF(): SizeF = SizeF(width.toFloat(), height.toFloat())

    /** Transforms the resolution into a crop rect, replacing any NaN values with real values. */
    private fun TransformationInfo.cropRectFor(resolution: Size): RectF =
        RectF(
            cropRectLeft.let { if (it.isNaN()) 0f else it },
            cropRectTop.let { if (it.isNaN()) 0f else it },
            cropRectRight.let { if (it.isNaN()) resolution.width.toFloat() else it },
            cropRectBottom.let { if (it.isNaN()) resolution.height.toFloat() else it }
        )

    private fun TransformationInfo.rotatedViewportFor(resolution: Size): SizeF =
        cropRectFor(resolution).let {
            if (is90or270(sourceRotation)) {
                SizeF(it.height(), it.width())
            } else {
                SizeF(it.width(), it.height())
            }
        }
}
