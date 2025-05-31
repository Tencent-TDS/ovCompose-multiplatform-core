package androidx.compose.ui.text

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LocalPath
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.platform.v2.nativefoundation.AdaptiveCanvas
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.uikit.utils.ITMMComposeTextProtocol
import androidx.compose.ui.unit.Constraints
import kotlinx.cinterop.useContents
import platform.Foundation.NSValue
import platform.QuartzCore.CALayer
import platform.UIKit.CGRectValue

/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

/**
 * IOS平台的Paragraph接口，实现文本的测量与绘制
 */
open class IOSParagraph(
    private val intrinsics: IOSParagraphIntrinsics,
    maxLines: Int,
    ellipsis: Boolean,
    private val constraints: Constraints
) : PublicParagraph {

    private val nativeParagraph: ITMMComposeTextProtocol = intrinsics.nativeParagraph

    init {
        // 由于ParagraphLayoutCache中会重新计算约束大小，需要根据新的约束重新布局
        intrinsics.relayout(constraints, maxLines, ellipsis)
    }

    /**
     * 与Skia相同
     */
    override val width: Float
        get() = constraints.maxWidth.toFloat()

    /**
     * 测量后的文本高度
     */
    override val height: Float
        get() = intrinsics.localSize.y

    /**
     * 测量后文本最小宽度
     */
    override val minIntrinsicWidth: Float
        get() = 0f

    /**
     * 测量后的文本最大宽度
     */
    override val maxIntrinsicWidth: Float
        get() = intrinsics.localSize.x

    /**
     * 第一行文本的BaseLine
     */
    override val firstBaseline: Float
        get() = nativeParagraph.getFirstBaseline().toFloat()

    /**
     * 最后一行文本的BaseLine
     */
    override val lastBaseline: Float
        get() = nativeParagraph.getLastBaseline().toFloat()

    /**
     * [lineCount]是否会超过[maxLines]
     */
    override val didExceedMaxLines: Boolean
        get() = false

    /**
     * 文本的行数
     */
    override val lineCount: Int
        get() = nativeParagraph.lineCount().toInt()

    override val placeholderRects: List<Rect?>
        get() = emptyList()

    /**
     * 获取文本范围内的矩形区域,通过[Path.addRect]组成Path,用于选中的单词矩形框，以Skia数据为例：
     *
     * Start:0  End:71
     * Rect(_left=0.0, _top=-0.12, _right=395.82, _bottom=50.0)
     * Rect(_left=0.0, _top=49.88, _right=359.01, _bottom=100.0)
     * Rect(_left=0.0, _top=99.88, _right=284.65, _bottom=150.0)
     * Rect(_left=0.0, _top=149.88, _right=271.46, _bottom=200.0)
     */
    override fun getPathForRange(start: Int, end: Int): Path {
        val rectList = nativeParagraph.getRectsForRange(start, end)
        return LocalPath().apply {
            val density = intrinsics.density.density
            rectList.forEach {
                addRect((it as NSValue).CGRectValue.useContents {
                    Rect(
                        Offset(origin.x.toFloat() * density, origin.y.toFloat() * density),
                        Size(size.width.toFloat() * density, size.height.toFloat() * density)
                    )
                })
            }
        }
    }

    /**
     * 根据字符所在位置[offset]返回输入框光标所在的矩形范围
     *
     * 类比Skia的数据(以Don't)为例：
     * Offset:0  Rect:Rect.fromLTRB(0.0, -0.1, 0.0, 50.0)
     * Offset:1  Rect:Rect.fromLTRB(30.5, -0.1, 30.5, 50.0)
     * Offset:2  Rect:Rect.fromLTRB(55.3, -0.1, 55.3, 50.0)
     * Offset:3  Rect:Rect.fromLTRB(79.8, -0.1, 79.8, 50.0)
     * Offset:4  Rect:Rect.fromLTRB(92.3, -0.1, 92.3, 50.0)
     *
     * 返回的[Rect]值，后面再验证一下光标位置
     * IOSParagraph getCursorRect:0 Rect:Rect.fromLTRB(0.0, 0.0, 0.0, 50.1)
     * IOSParagraph getCursorRect:1 Rect:Rect.fromLTRB(30.1, 0.0, 30.1, 50.1)
     */
    override fun getCursorRect(offset: Int): Rect {
        nativeParagraph.getCursorRect(offset).useContents {
            return with(intrinsics.density.density) {
                Rect(
                    Offset(origin.x.toFloat() * this, origin.y.toFloat() * this),
                    // Skia的数据，Left和Right是相同的值，所以width是0
                    Size(0f, size.height.toFloat() * this)
                )
            }
        }
    }

    /**
     * 返回第[lineIndex]所在行的Left
     */
    override fun getLineLeft(lineIndex: Int): Float {
        return nativeParagraph.getLineLeft(lineIndex)
    }

    /**
     * 返回第[lineIndex]所在行的Right
     */
    override fun getLineRight(lineIndex: Int): Float {
        return nativeParagraph.getLineRight(lineIndex)
    }

    /**
     * 返回第[lineIndex]所在行的Top
     */
    override fun getLineTop(lineIndex: Int): Float {
        return nativeParagraph.getLineTop(lineIndex) * intrinsics.density.density
    }

    /**
     * 返回第[lineIndex]所在行的Bottom
     */
    override fun getLineBottom(lineIndex: Int): Float {
        return nativeParagraph.getLineBottom(lineIndex) * intrinsics.density.density
    }

    /**
     * 获取第[lineIndex]行的高度
     */
    override fun getLineHeight(lineIndex: Int): Float {
        return nativeParagraph.getLineHeight(lineIndex) * intrinsics.density.density
    }

    /**
     * 获取第[lineIndex]行的宽度
     */
    override fun getLineWidth(lineIndex: Int): Float {
        return nativeParagraph.getLineWidth(lineIndex) * intrinsics.density.density
    }

    /**
     * 返回第[lineIndex]行的第一个光标所在的Index，例如：
     *
     * aaa ： LineIndex: 0 , 返回值0
     * bbb ： LineIndex: 1 , 返回值4
     * ccc ： LineIndex: 2 , 返回值8
     * ddd ： LineIndex: 3 , 返回值12
     *
     */
    override fun getLineStart(lineIndex: Int): Int {
        return nativeParagraph.getLineStart(lineIndex).toInt()
    }

    /**
     * 返回第[lineIndex]行的最后一个光标所在的Index，例如：
     *
     * aaa ： LineIndex: 0 , 返回值3
     * bbb ： LineIndex: 1 , 返回值7
     * ccc ： LineIndex: 2 , 返回值11
     * ddd ： LineIndex: 3 , 返回值15
     *
     */
    override fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int {
        return nativeParagraph.getLineEnd(lineIndex, visibleEnd).toInt()
    }

    /**
     * 判断第[lineIndex]行是否被截断
     */
    override fun isLineEllipsized(lineIndex: Int): Boolean {
        return nativeParagraph.isLineEllipsized(lineIndex)
    }

    /**
     * 返回字符偏移量(即第[offset]个字符)所在的行
     */
    override fun getLineForOffset(offset: Int): Int {
        if (offset <= 0) {
            return 0
        }
        return nativeParagraph.getLineForOffset(offset)
    }

    /**
     * 获取第[offset]空格所在的横向坐标位置，例如：
     * 0 --> 文本最左边的位置
     * 1 --> 第一个字符和第二个字符之间的坐标
     * 2 --> 第二个字符和第三个字符之间的坐标
     */
    override fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Float {
        if (offset <= 0) {
            return 0f
        }
        with(intrinsics.density.density) {
            val prevCharRight = nativeParagraph.getCursorRect(offset - 1).useContents {
                (origin.x.toFloat() + size.width) * this@with
            }
            val nextCharLeft = nativeParagraph.getCursorRect(offset).useContents {
                origin.x.toFloat() * this@with
            }
            return if (prevCharRight > nextCharLeft) {
                // 如果出现错行的情况，则使用nextCharLeft即可
                nextCharLeft
            } else {
                ((nextCharLeft + prevCharRight) / 2).toFloat()
            }
        }
    }

    /**
     * 整个文本段的展示方向
     */
    override fun getParagraphDirection(offset: Int): ResolvedTextDirection {
        // TODO:后续处理阿拉伯文字和中英混排的方向
        return ResolvedTextDirection.Ltr
    }

    /**
     * 在双向文本中处理第[offset]个字符的展示方向
     */
    override fun getBidiRunDirection(offset: Int): ResolvedTextDirection {
        // TODO:后续处理阿拉伯文字和中英混排的方向
        return ResolvedTextDirection.Ltr
    }

    override fun getLineForVerticalPosition(vertical: Float): Int {
        TODO("Not yet implemented")
    }

    /**
     * 根据点击所反馈的[position]找到字符所在的Index，即点击到的字符的位置
     */
    override fun getOffsetForPosition(position: Offset): Int {
        val density = intrinsics.density.density
        return nativeParagraph.getOffsetForPositionX(position.x / density, position.y / density)
            .toInt()
    }

    /**
     * 根据字符所在的[offset]位置返回它所在的矩形
     */
    override fun getBoundingBox(offset: Int): Rect {
        nativeParagraph.getCursorRect(offset).useContents {
            return with(intrinsics.density.density) {
                Rect(
                    Offset(origin.x.toFloat() * this, origin.y.toFloat() * this),
                    Size(size.width.toFloat() * this, size.height.toFloat() * this)
                )
            }
        }
    }

    override fun fillBoundingBoxes(range: TextRange, array: FloatArray, arrayStart: Int) {
        TODO("Not yet implemented")
    }

    /**
     * 根据字符所在的[offset]偏移找到该字符单词的范围，例如：
     *
     * aaa ： offset: 1, 返回值 TextRange(0, 3)
     * bbb ： offset: 5 , 返回值 TextRange(4, 7)
     * ccc ： offset: 10 , 返回值 TextRange(8, 11)
     * ddd ： offset: 14 , 返回值 TextRange(12, 15)
     *
     */
    override fun getWordBoundary(offset: Int): TextRange {
        return nativeParagraph.getWordBoundary(offset).useContents {
            TextRange(location.toInt(), location.toInt() + length.toInt())
        }
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        TODO("Not yet implemented")
    }

    override fun paint(
        canvas: Canvas,
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        val adaptiveCanvas = canvas as AdaptiveCanvas
        nativeParagraph.paintWithColor(color.value)
        adaptiveCanvas.drawLayer(nativeParagraph as CALayer)
    }

    override fun paint(
        canvas: Canvas,
        brush: Brush,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?,
        drawStyle: DrawStyle?,
        blendMode: BlendMode
    ) {
        val adaptiveCanvas = canvas as AdaptiveCanvas
        // TODO: Brush是类似LinearGradient的渐变色，待支持
        nativeParagraph.paintWithColor(Color.Red.value)
        adaptiveCanvas.drawLayer(nativeParagraph as CALayer)
    }
}
