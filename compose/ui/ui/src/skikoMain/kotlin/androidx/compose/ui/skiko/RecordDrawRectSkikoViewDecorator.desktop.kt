/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.skiko

import org.jetbrains.skia.Rect as SkRect
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.toComposeRect
import org.jetbrains.skia.Canvas
import org.jetbrains.skia.PictureRecorder
import org.jetbrains.skia.RTreeFactory
import org.jetbrains.skiko.SkikoView

internal class RecordDrawRectSkikoViewDecorator(
    private val decorated: SkikoView,
    private val onDrawRectChange: (Rect) -> Unit
) : SkikoView by decorated {
    private val pictureRecorder = PictureRecorder()
    private val bbhFactory = RTreeFactory()
    private var drawRect = Rect.Zero
        private set(value) {
            if (value != field) {
                field = value
                onDrawRectChange(value)
            }
        }

    fun close() {
        pictureRecorder.close()
        bbhFactory.close()
    }

    override fun onRender(canvas: Canvas, width: Int, height: Int, nanoTime: Long) {
        drawRect = canvas.recordCullRect {
            decorated.onRender(it, width, height, nanoTime)
        }.toComposeRect()
    }

    private inline fun Canvas.recordCullRect(
        block: (Canvas) -> Unit
    ): SkRect {
        val largestRect = SkRect.makeLTRB(
            Float.MIN_VALUE, Float.MIN_VALUE, Float.MAX_VALUE, Float.MAX_VALUE
        )
        val pictureCanvas = pictureRecorder.beginRecording(largestRect, bbhFactory)
        block(pictureCanvas)
        val picture = pictureRecorder.finishRecordingAsPicture()
        try {
            drawPicture(picture, null, null)
            return picture.cullRect
        } finally {
            picture.close()
        }
    }
}
