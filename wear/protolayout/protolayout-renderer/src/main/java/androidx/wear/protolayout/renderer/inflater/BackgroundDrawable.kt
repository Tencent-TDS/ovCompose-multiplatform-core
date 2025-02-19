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

package androidx.wear.protolayout.renderer.inflater

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable

/**
 * This class overrides [GradientDrawable] for drawing circular shapes.
 *
 * For circular shapes:
 * * The default behavior is that for circles, a circle is drawn first with anti-aliasing applied,
 *   then the view is clipped to the circular outline causing anti-aliasing to be applied a second
 *   time.
 * * The new behavior is drawing a square first and then applying anti-aliasing once when clipping
 *   the view to the circular outline.
 * * Note that the fix is only applied for circular shapes with no stroke (border).
 */
public class BackgroundDrawable : GradientDrawable() {
    private val customPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var isStrokeUsed = false
    private var cornerRadiiUsed = false
    private var fillColor: Int? = null

    override fun setCornerRadii(radii: FloatArray?) {
        cornerRadiiUsed = true
        super.setCornerRadii(radii)
    }

    override fun setStroke(width: Int, color: Int) {
        isStrokeUsed = width != 0
        super.setStroke(width, color)
    }

    override fun setColor(argb: Int) {
        fillColor = argb
        super.setColor(argb)
    }

    override fun draw(canvas: Canvas) {
        val width = bounds.width()
        val height = bounds.height()

        val isCircle = width == height && cornerRadius >= width * 0.5f && !cornerRadiiUsed
        if (isStrokeUsed || !isCircle) {
            // If the shape is not a circle or stroke is used, then use the default implementation
            // of draw();
            super.draw(canvas)
            return
        }

        if (fillColor != null) {
            customPaint.apply {
                reset()
                style = Paint.Style.FILL
                if (shader == null) {
                    fillColor?.let { color = it }
                }
            }
            // Draw a square.
            // The square will be clipped using the outline.
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), customPaint)
        }
    }
}
