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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.intl.LocaleList
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Density
import kotlin.math.ceil

/**
 * 鸿蒙平台的ParagraphIntrinsics实现
 *
 * 负责计算文本的固有宽度（最小和最大）
 *
 * 设计思路：
 * 1. 使用OHOSParagraphLayouter进行布局计算
 * 2. 在初始化时计算并缓存固有宽度，避免重复计算
 * 3. 提供layouter()方法获取可重用的Layouter实例，减少对象创建开销
 */
internal class OHOSParagraphIntrinsics(
    val text: String,
    val style: TextStyle,
    val spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    val placeholders: List<AnnotatedString.Range<Placeholder>>,
    val density: Density,
    val fontFamilyResolver: FontFamily.Resolver,
) : ParagraphIntrinsics {
    val textDirection = resolveTextDirection(
        text,
        style.textDirection,
        style.localeList
    )

    private var layouter: OHOSParagraphLayouter? = newLayouter()

    fun layouter(): OHOSParagraphLayouter {
        val layouter = this.layouter ?: newLayouter()
        this.layouter = null
        return layouter
    }

    private fun newLayouter() = OHOSParagraphLayouter(
        text = text,
        textDirection = textDirection,
        style = style,
        spanStyles = spanStyles,
        placeholders = placeholders,
        density = density,
        fontFamilyResolver = fontFamilyResolver
    )

    /**
     * 最小固有宽度：文本在所有软换行机会都使用时的宽度
     */
    override var minIntrinsicWidth = 0f
        private set

    /**
     * 最大固有宽度：文本在单行显示时的宽度
     */
    override var maxIntrinsicWidth = 0f
        private set

    /**
     * 初始化块，在对象创建时执行：
     * 1. 使用 layouter 计算段落布局，宽度为无限大（单行显示）
     * 2. 通过 para 获取并缓存最小和最大固有宽度，避免后续重复计算
     */
    init {
        val para = layouter!!.layoutParagraph(Float.POSITIVE_INFINITY)
        minIntrinsicWidth = ceil(para.getMinIntrinsicWidth()).toFloat()
        maxIntrinsicWidth = ceil(para.getMaxIntrinsicWidth()).toFloat()
    }
}

fun Color.toUInt(): UInt {
    val a = (alpha * 255.0f).toInt() and 0xFF
    val r = (red * 255.0f).toInt() and 0xFF
    val g = (green * 255.0f).toInt() and 0xFF
    val b = (blue * 255.0f).toInt() and 0xFF
    return ((a shl 24) or (r shl 16) or (g shl 8) or b).toUInt()
}

internal fun resolveTextDirection(
    text: String,
    textDirection: TextDirection? = null,
    localeList: LocaleList? = null
): ResolvedTextDirection {
    return when (textDirection ?: TextDirection.Content) {
        // 如果方向是明确的
        TextDirection.Ltr -> ResolvedTextDirection.Ltr
        TextDirection.Rtl -> ResolvedTextDirection.Rtl
        // TODO Content方向需要根据文本和localeList进行推断
        else -> error("Invalid TextDirection.")
    }
}
