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

import androidx.compose.ui.arkui.utils.ParagraphHandle_Handle
import androidx.compose.ui.arkui.utils.Paragraph_destroy
import androidx.compose.ui.arkui.utils.Paragraph_didExceedMaxLines
import androidx.compose.ui.arkui.utils.Paragraph_getAlphabeticBaseline
import androidx.compose.ui.arkui.utils.Paragraph_getBaseRenderNode
import androidx.compose.ui.arkui.utils.Paragraph_getCursorRect
import androidx.compose.ui.arkui.utils.Paragraph_getHeight
import androidx.compose.ui.arkui.utils.Paragraph_getHorizontalPosition
import androidx.compose.ui.arkui.utils.Paragraph_getIdeographicBaseline
import androidx.compose.ui.arkui.utils.Paragraph_getLineBaseline
import androidx.compose.ui.arkui.utils.Paragraph_getLineBottom
import androidx.compose.ui.arkui.utils.Paragraph_getLineCount
import androidx.compose.ui.arkui.utils.Paragraph_getLineEnd
import androidx.compose.ui.arkui.utils.Paragraph_getLineForOffset
import androidx.compose.ui.arkui.utils.Paragraph_getLineForVerticalPosition
import androidx.compose.ui.arkui.utils.Paragraph_getLineHeight
import androidx.compose.ui.arkui.utils.Paragraph_getLineLeft
import androidx.compose.ui.arkui.utils.Paragraph_getLineRight
import androidx.compose.ui.arkui.utils.Paragraph_getLineStart
import androidx.compose.ui.arkui.utils.Paragraph_getLineTop
import androidx.compose.ui.arkui.utils.Paragraph_getLineWidth
import androidx.compose.ui.arkui.utils.Paragraph_getLongestLine
import androidx.compose.ui.arkui.utils.Paragraph_getMaxIntrinsicWidth
import androidx.compose.ui.arkui.utils.Paragraph_getMinIntrinsicWidth
import androidx.compose.ui.arkui.utils.Paragraph_getOffsetForPosition
import androidx.compose.ui.arkui.utils.Paragraph_getRectsForRange
import androidx.compose.ui.arkui.utils.Paragraph_getRectsForRangeCount
import androidx.compose.ui.arkui.utils.Paragraph_getWidth
import androidx.compose.ui.arkui.utils.Paragraph_getWordBoundary
import androidx.compose.ui.arkui.utils.Paragraph_layout
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.nativefoundation.OHOSNativeCanvas
import kotlinx.cinterop.*
import kotlin.native.ref.createCleaner

/**
 * 鸿蒙平台的Native段落代理
 *
 * 封装对Native段落对象的操作，提供类型安全的查询和绘制接口
 *
 * 设计思路：
 * 1. 持有Native段落句柄，确保资源管理
 * 2. 提供丰富的查询方法，映射到Native函数调用
 * 3. 实现自动资源清理，防止内存泄漏
 */
internal class OHOSNativeParagraphProxy(
    private val handle: ParagraphHandle_Handle
) {

    /**
     * 自动资源清理器
     * 使用createCleaner确保Native资源在对象被GC时自动释放
     */
    @Suppress("unused")
    private val cleaner = createCleaner(handle) { ptr ->
        Paragraph_destroy(ptr)
    }

    // ========== 查询接口 ==========

    /**
     * 获取段落宽度
     * @return 宽度值（像素）
     */
    fun getWidth(): Double = Paragraph_getWidth(handle)

    /**
     * 获取段落高度
     * @return 高度值（像素）
     */
    fun getHeight(): Double = Paragraph_getHeight(handle)

    /**
     * 获取最小固有宽度
     * @return 最小宽度（像素）
     */
    fun getMinIntrinsicWidth(): Double = Paragraph_getMinIntrinsicWidth(handle)

    /**
     * 获取最大固有宽度
     * @return 最大宽度（像素）
     */
    fun getMaxIntrinsicWidth(): Double = Paragraph_getMaxIntrinsicWidth(handle)

    /**
     * 获取字母基线位置
     * @return 基线位置（像素）
     */
    fun getAlphabeticBaseline(): Double = Paragraph_getAlphabeticBaseline(handle)

    /**
     * 获取表意文字基线位置
     * @return 基线位置（像素）
     */
    fun getIdeographicBaseline(): Double = Paragraph_getIdeographicBaseline(handle)

    /**
     * 获取最长行宽度
     * @return 最长行宽度（像素）
     */
    fun getLongestLine(): Double = Paragraph_getLongestLine(handle)

    /**
     * 检查是否超出最大行数
     * @return true如果超出，false否则
     */
    fun didExceedMaxLines(): Boolean = Paragraph_didExceedMaxLines(handle)

    /**
     * 获取行数
     * @return 总行数
     */
    fun getLineCount(): Int = Paragraph_getLineCount(handle).toInt()

    // ========== 行信息查询 ==========

    /**
     * 获取指定行的左边界
     * @param lineIndex 行索引（从0开始）
     * @return 左边界位置（像素）
     */
    fun getLineLeft(lineIndex: Int): Double =
        Paragraph_getLineLeft(handle, lineIndex.toUInt())

    /**
     * 获取指定行的右边界
     * @param lineIndex 行索引
     * @return 右边界位置（像素）
     */
    fun getLineRight(lineIndex: Int): Double =
        Paragraph_getLineRight(handle, lineIndex.toUInt())

    /**
     * 获取指定行的顶部位置
     * @param lineIndex 行索引
     * @return 顶部位置（像素）
     */
    fun getLineTop(lineIndex: Int): Double =
        Paragraph_getLineTop(handle, lineIndex.toUInt())

    /**
     * 获取指定行的底部位置
     * @param lineIndex 行索引
     * @return 底部位置（像素）
     */
    fun getLineBottom(lineIndex: Int): Double =
        Paragraph_getLineBottom(handle, lineIndex.toUInt())

    /**
     * 获取指定行的宽度
     * @param lineIndex 行索引
     * @return 行宽度（像素）
     */
    fun getLineWidth(lineIndex: Int): Double =
        Paragraph_getLineWidth(handle, lineIndex.toUInt())

    /**
     * 获取指定行的高度
     * @param lineIndex 行索引
     * @return 行高度（像素）
     */
    fun getLineHeight(lineIndex: Int): Double =
        Paragraph_getLineHeight(handle, lineIndex.toUInt())

    /**
     * 获取指定行的起始字符位置
     * @param lineIndex 行索引
     * @return 起始字符索引
     */
    fun getLineStart(lineIndex: Int): Int =
        Paragraph_getLineStart(handle, lineIndex.toUInt()).toInt()

    /**
     * 获取指定行的结束字符位置
     * @param lineIndex 行索引
     * @param visibleEnd 是否只计算可见字符（排除空白）
     * @return 结束字符索引
     */
    fun getLineEnd(lineIndex: Int, visibleEnd: Boolean): Int =
        Paragraph_getLineEnd(handle, lineIndex.toUInt(), visibleEnd).toInt()

    /**
     * 获取指定行的基线位置
     * @param lineIndex 行索引
     * @return 基线位置（像素）
     */
    fun getLineBaseline(lineIndex: Int): Double =
        Paragraph_getLineBaseline(handle, lineIndex.toUInt())

    // ========== 位置查询 ==========

    /**
     * 获取字符偏移量的水平位置
     * @param offset 字符偏移量
     * @param usePrimaryDirection 是否使用主方向
     * @return 水平位置（像素）
     */
    fun getHorizontalPosition(offset: Int, usePrimaryDirection: Boolean): Double =
        Paragraph_getHorizontalPosition(handle, offset.toUInt(), usePrimaryDirection)

    /**
     * 根据坐标获取字符偏移量
     * @param dx X坐标
     * @param dy Y坐标
     * @return 字符偏移量
     */
    fun getOffsetForPosition(dx: Double, dy: Double): Int =
        Paragraph_getOffsetForPosition(handle, dx, dy).toInt()

    /**
     * 获取光标矩形
     * @param offset 字符偏移量
     * @return 光标矩形
     */
    fun getCursorRect(offset: Int): Rect = memScoped {
        val left = alloc<DoubleVar>()
        val top = alloc<DoubleVar>()
        val right = alloc<DoubleVar>()
        val bottom = alloc<DoubleVar>()

        Paragraph_getCursorRect(handle, offset.toUInt(), left.ptr, top.ptr, right.ptr, bottom.ptr)

        Rect(
            left = left.value.toFloat(),
            top = top.value.toFloat(),
            right = right.value.toFloat(),
            bottom = bottom.value.toFloat()
        )
    }

    /**
     * 获取单词边界
     * @param offset 字符偏移量
     * @return Pair(起始位置, 结束位置)
     */
    fun getWordBoundary(offset: Int): Pair<Int, Int> = memScoped {
        val start = alloc<UIntVar>()
        val end = alloc<UIntVar>()

        Paragraph_getWordBoundary(handle, offset.toUInt(), start.ptr, end.ptr)

        Pair(start.value.toInt(), end.value.toInt())
    }

    /**
     * 获取指定范围的矩形列表
     * @param start 起始字符位置
     * @param end 结束字符位置
     * @return 矩形列表
     */
    fun getRectsForRange(start: Int, end: Int): List<Rect> {
        // 先获取数量
        val count = Paragraph_getRectsForRangeCount(handle, start.toUInt(), end.toUInt())
        if (count == 0u) return emptyList()

        // 批量获取
        return memScoped {
            val rectCount = count.toInt()
            val rectsArray = allocArray<DoubleVar>(rectCount * 4)

            val actualCount = Paragraph_getRectsForRange(
                handle,
                start.toUInt(),
                end.toUInt(),
                rectsArray,
                (rectCount * 4).toUInt()
            ).toInt()

            List(actualCount) { i ->
                val base = i * 4
                Rect(
                    left = rectsArray[base].toFloat(),
                    top = rectsArray[base + 1].toFloat(),
                    right = rectsArray[base + 2].toFloat(),
                    bottom = rectsArray[base + 3].toFloat()
                )
            }
        }
    }

    /**
     * 根据偏移量获取所在行
     * @param offset 字符偏移量
     * @return 行索引
     */
    fun getLineForOffset(offset: Int): Int =
        Paragraph_getLineForOffset(handle, offset.toUInt()).toInt()

    /**
     * 根据垂直位置获取所在行
     * @param vertical 垂直位置（像素）
     * @return 行索引
     */
    fun getLineForVerticalPosition(vertical: Double): Int =
        Paragraph_getLineForVerticalPosition(handle, vertical).toInt()

    // ========== 绘制 ==========

    /**
     * 绘制到Canvas
     * @param canvas Native Canvas指针
     * @param x X坐标
     * @param y Y坐标
     */
    fun paint(canvas: OHOSNativeCanvas) {
        val renderNodeHandle = Paragraph_getBaseRenderNode(handle);
        require(renderNodeHandle != null) { "Paragraph render node is null." }
        canvas.drawLayer(renderNodeHandle)
    }

    // ========== 重新布局 ==========

    /**
     * 重新执行布局
     * @param maxWidth 最大宽度
     */
    fun layout(maxWidth: Double) {
        Paragraph_layout(handle, maxWidth)
    }
}
