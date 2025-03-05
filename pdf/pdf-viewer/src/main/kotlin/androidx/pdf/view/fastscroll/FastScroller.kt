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

package androidx.pdf.view.fastscroll

import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Rect
import android.util.Range

/**
 * Manages and draws the fast scroller UI element.
 *
 * This class is responsible for controlling the behavior and rendering of the fast scroller, which
 * provides a visual indicator of the current scroll position and allows for quick navigation within
 * a scrollable view. It collaborates with a [FastScrollDrawer] to handle the drawing and a
 * [FastScrollCalculator] to perform scroll calculations.
 *
 * @param fastScrollDrawer The [FastScrollDrawer] that handles the actual drawing of the fast
 *   scroller UI.
 * @param scrollCalculator The [FastScrollCalculator] that performs scroll-related calculations.
 */
internal class FastScroller(
    val fastScrollDrawer: FastScrollDrawer,
    private val scrollCalculator: FastScrollCalculator
) {
    internal var fastScrollY: Int = 0

    // This is used to optimize performance. If the scroll position has already been updated
    // by another method the calculation is skipped.
    private var lastScrollY: Int = 0

    private var hideValueAnimator: ValueAnimator? = null

    /**
     * Draws the fast scroller on the canvas.
     *
     * This method handles the drawing of the fast scroller, which provides a visual indication of
     * the current scroll position and allows for quick navigation within a scrollable view. It
     * calculates the vertical position of the scroller and then delegates the actual drawing to the
     * `renderer`.
     *
     * @param canvas The canvas on which to draw the scroller.
     * @param scrollY The raw vertical scroll position in pixels.
     * @param zoom The current zoom level.
     * @param viewHeight The height of the view in pixels.
     * @param visibleArea The rectangular area of the view that is currently visible.
     * @param visiblePages The range of pages that are currently visible.
     */
    fun drawScroller(
        canvas: Canvas,
        scrollY: Int,
        zoom: Float,
        viewHeight: Int,
        visibleArea: Rect,
        visiblePages: Range<Int>,
        estimatedFullHeight: Int
    ) {
        if (scrollY != lastScrollY) {
            fastScrollY =
                scrollCalculator.computeThumbPosition(
                    scrollY,
                    zoom,
                    viewHeight,
                    fastScrollDrawer.thumbHeightPx,
                    estimatedFullHeight
                )
            lastScrollY = scrollY
        }

        fastScrollDrawer.draw(canvas, zoom, fastScrollY, visibleArea, visiblePages)
    }

    /**
     * Calculates the content scroll position based on the fast scroller position.
     *
     * This method determines the new vertical scroll position for the displayed content based on
     * the current position of the fast scroller. It uses the `ScrollCalculator` to constrain the
     * fast scroller position and then compute the corresponding content scroll position, taking
     * into account the zoom level.
     *
     * @param scrollY The raw vertical scroll position of the fast scroller in pixels.
     * @param zoom The current zoom level.
     * @param viewHeight The height of the view in pixels.
     * @return The calculated content scroll position in pixels.
     */
    fun viewScrollPositionFromFastScroller(
        scrollY: Float,
        zoom: Float,
        viewHeight: Int,
        estimatedFullHeight: Int
    ): Int {
        fastScrollY =
            scrollCalculator.constrainScrollPosition(
                scrollY,
                viewHeight,
                fastScrollDrawer.thumbHeightPx
            )

        return scrollCalculator.computeViewScroll(
            fastScrollY,
            viewHeight,
            zoom,
            estimatedFullHeight
        )
    }

    fun show(onAnimationUpdate: () -> Unit) {
        hideValueAnimator?.cancel()
        fastScrollDrawer.alpha = FastScrollDrawer.VISIBLE_ALPHA
        animate(onAnimationUpdate)
    }

    private fun animate(onAnimationUpdate: () -> Unit) {
        hideValueAnimator =
            ValueAnimator.ofInt(FastScrollDrawer.VISIBLE_ALPHA, FastScrollDrawer.GONE_ALPHA).apply {
                startDelay = HIDE_DELAY_MS
                duration = HIDE_ANIMATION_DURATION_MILLIS
                addUpdateListener { animation ->
                    fastScrollDrawer.alpha = animation.animatedValue as Int
                    onAnimationUpdate()
                }
                start()
            }
    }

    companion object {
        private const val HIDE_ANIMATION_DURATION_MILLIS = 200L
        private const val HIDE_DELAY_MS = 1300L
    }
}
