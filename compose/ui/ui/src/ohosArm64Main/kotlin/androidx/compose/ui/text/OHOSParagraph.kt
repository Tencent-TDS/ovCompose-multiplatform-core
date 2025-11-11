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

package androidx.compose.ui.text

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.platform.nativefoundation.AdaptiveCanvas
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Constraints

/**
 * 鸿蒙平台的Paragraph实现
 *
 * 设计思路：
 * 1. 使用OHOSParagraphIntrinsics和OHOSParagraphLayouter进行布局和样式管理
 * 2. 委托所有查询方法给NativeParagraphProxy，确保类型安全
 * 3. 在paint方法中更新样式并重新布局，确保渲染最新内容
 */
internal class OHOSParagraph(
    intrinsics: ParagraphIntrinsics,
    val maxLines: Int,
    val ellipsis: Boolean,
    val constraints: Constraints
) : PublicParagraph {

    private val ellipsisChar = if (ellipsis) "\u2026" else ""

    private val paragraphIntrinsics = intrinsics as OHOSParagraphIntrinsics

    private val layouter = paragraphIntrinsics.layouter().apply {
        setParagraphStyle(
            maxLines = maxLines,
            ellipsis = ellipsisChar
        )
    }

    /**
     * Native适配器：封装所有Native调用
     * 职责：FFI调用、类型转换、资源管理
     */
    private var nativeParagraph = layouter.layoutParagraph(
        width = width
    )

    init {
        nativeParagraph.layout(width.toDouble())
    }

    // ========== Paragraph接口实现 ==========
    // 所有方法委托给NativeParagraph

    /**
     * 宽度：直接从约束获取，无需Native调用
     */
    override val width: Float
        get() = constraints.maxWidth.toFloat()

    /**
     * 高度：调用Native函数
     * 性能：单次FFI调用
     */
    override val height: Float
        get() = nativeParagraph.getHeight().toFloat() ?: 0f

    override val minIntrinsicWidth: Float
        get() = nativeParagraph.getMinIntrinsicWidth().toFloat() ?: 0f

    override val maxIntrinsicWidth: Float
        get() = nativeParagraph.getMaxIntrinsicWidth().toFloat() ?: 0f

    override val firstBaseline: Float
        get() = nativeParagraph.getAlphabeticBaseline().toFloat() ?: 0f

    /**
     * 最后一行基线：需要先获取行数
     * 优化：使用局部变量减少重复调用
     */
    override val lastBaseline: Float
        get() {
            val count = nativeParagraph.getLineCount()
            if (count == 0) return 0f
            return nativeParagraph.getLineBaseline(count - 1).toFloat()
        }

    override val didExceedMaxLines: Boolean
        get() = nativeParagraph.didExceedMaxLines() ?: false

    override val lineCount: Int
        get() = nativeParagraph.getLineCount() ?: 0

    override val placeholderRects: List<Rect?>
        get() = emptyList()

    override fun getPathForRange(start: Int, end: Int): Path {
        val path = Path()
        val rects = nativeParagraph .getRectsForRange(start, end)
        rects.forEach { path.addRect(it) }
        return path
    }

    /**
     * 获取光标矩形
     */
    override fun getCursorRect(offset: Int): Rect =
        nativeParagraph.getCursorRect(offset) ?: Rect.Zero

    // ========== 行信息查询（inline优化） ==========

    override fun getLineLeft(lineIndex: Int): Float =
        nativeParagraph.getLineLeft(lineIndex).toFloat() ?: 0f

    override fun getLineRight(lineIndex: Int): Float =
        nativeParagraph.getLineRight(lineIndex).toFloat() ?: 0f

    override fun getLineTop(lineIndex: Int): Float =
        nativeParagraph.getLineTop(lineIndex).toFloat() ?: 0f

    override fun getLineBottom(lineIndex: Int): Float =
        nativeParagraph.getLineBottom(lineIndex).toFloat() ?: 0f

    override fun getLineHeight(lineIndex: Int): Float =
        nativeParagraph.getLineHeight(lineIndex).toFloat() ?: 0f

    override fun getLineWidth(lineIndex: Int): Float =
        nativeParagraph.getLineWidth(lineIndex).toFloat() ?: 0f

    override fun getLineStart(lineIndex: Int): Int =
        nativeParagraph.getLineStart(lineIndex) ?: 0

    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int =
        nativeParagraph.getLineEnd(lineIndex, visibleEnd) ?: 0

    override fun isLineEllipsized(lineIndex: Int): Boolean =
        lineIndex == maxLines - 1 && didExceedMaxLines

    override fun getLineForOffset(offset: Int): Int =
        nativeParagraph.getLineForOffset(offset) ?: 0

    override fun getLineForVerticalPosition(vertical: Float): Int =
        nativeParagraph.getLineForVerticalPosition(vertical.toDouble()) ?: 0

    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float =
        nativeParagraph.getHorizontalPosition(offset, usePrimaryDirection).toFloat() ?: 0f

    override fun getParagraphDirection(offset: Int): ResolvedTextDirection =
        paragraphIntrinsics.textDirection

    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection =
        paragraphIntrinsics.textDirection

    override fun getOffsetForPosition(position: Offset): Int =
        nativeParagraph.getOffsetForPosition(position.x.toDouble(), position.y.toDouble()) ?: 0

    override fun getBoundingBox(offset: Int): Rect {
        if (offset >= paragraphIntrinsics.text.length) return Rect.Zero
        val rects = nativeParagraph .getRectsForRange(offset, offset + 1)
        return rects.firstOrNull() ?: Rect.Zero
    }

    /**
     * 获取单词边界
     *
     * 内存安全：使用memScoped栈内存
     */
    override fun getWordBoundary(offset: Int): TextRange {
        val (start, end) = nativeParagraph.getWordBoundary(offset)
        return TextRange(start, end)
    }

    override fun fillBoundingBoxes(range: TextRange, array: FloatArray, arrayStart: Int) {
        TODO("Not yet implemented")
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        nativeParagraph = with(layouter) {
            setTextStyle(
                color = color,
                shadow = shadow,
                textDecoration = textDecoration
            )
            layoutParagraph(
                width = width
            )
        }

        // 获取Native Canvas指针
        nativeParagraph.paint(canvas as AdaptiveCanvas)
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        LogPrintUtil.verbose {
            "OHOSParagraph::paint, canvas=$canvas, color=$color, shadow=$shadow, " +
                    "textDecoration=$textDecoration, drawStyle=$drawStyle, blendMode=$blendMode"
        }
        nativeParagraph = with(layouter) {
            setTextStyle(
                color = color,
                shadow = shadow,
                textDecoration = textDecoration
            )
            layoutParagraph(
                width = width
            )
        }

        // 获取Native Canvas指针
        nativeParagraph.paint(canvas as AdaptiveCanvas)
    }

    @ExperimentalTextApi
    override fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        LogPrintUtil.verbose {
            "OHOSParagraph::paint with brush, canvas=$canvas, brush=$brush, " +
                    "alpha=$alpha, shadow=$shadow, textDecoration=$textDecoration, " +
                    "drawStyle=$drawStyle, blendMode=$blendMode"
        }

        nativeParagraph = with(layouter) {
            setTextStyle(
                brush = brush,
                brushSize = Size(width, height),
                alpha = alpha,
                shadow = shadow,
                textDecoration = textDecoration
            )
            setDrawStyle(drawStyle)
            setBlendMode(blendMode)
            layoutParagraph(
                width = width
            )
        }

        nativeParagraph.paint(canvas as AdaptiveCanvas)
    }
}
