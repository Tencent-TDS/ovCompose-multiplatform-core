package androidx.compose.ui.text

import androidx.compose.ui.platform.toNSTextAlignment
import androidx.compose.ui.platform.toSp
import androidx.compose.ui.platform.toTMMNativeDecorator
import androidx.compose.ui.platform.toTMMNativeItalicType
import androidx.compose.ui.platform.toUIFontWeight
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.uikit.utils.ITMMComposeTextProtocol
import androidx.compose.ui.uikit.utils.TMMComposeTextAttributes
import androidx.compose.ui.uikit.utils.TMMComposeTextCreate
import androidx.compose.ui.uikit.utils.TMMComposeTextSpanAttributes
import androidx.compose.ui.uikit.utils.TMMNativeItalicType
import androidx.compose.ui.uikit.utils.TMMNativeTextDecorator
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlinx.cinterop.useContents
import org.jetbrains.skia.Point
import platform.UIKit.NSLineBreakByTruncatingTail
import platform.UIKit.NSLineBreakByWordWrapping
import platform.UIKit.UIFontWeightRegular

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
 * 对齐SkiaParagraphIntrinsics的实现，完成文本测量，返回测量结果给到[maxIntrinsicWidth]以及[minIntrinsicWidth]提供文本最大宽度与最小宽度
 */
class IOSParagraphIntrinsics(
    val text: String,
    val style: TextStyle,
    private val spanStyles: List<Range<SpanStyle>>,
    private val placeholders: List<Range<Placeholder>>,
    val density: Density,
    private val fontFamilyResolver: FontFamily.Resolver,
) : ParagraphIntrinsics {

    internal val nativeParagraph: ITMMComposeTextProtocol = TMMComposeTextCreate()

    /**
     * 在[IOSParagraph]将外部约束提供给文本进行测量，将文本测量结果保存
     */
    internal val localSize: Point by lazy {
        nativeParagraph.measureAndLayout(
            style.toTMMComposeTextAttributes(text),
            MAX_BOUNDS,
            MAX_BOUNDS
        ).useContents {
            Point(
                x.toFloat() * density.density,
                y.toFloat() * density.density
            )
        }
    }

    override val minIntrinsicWidth: Float
        get() = 0f

    /**
     * 返回文本最大宽度
     */
    override val maxIntrinsicWidth: Float
        get() = localSize.x

    internal fun relayout(constraints: Constraints, maxLines: Int, ellipsis: Boolean) {
        nativeParagraph.relayoutWithMaxWidth(
            constraints.maxWidth.toFloat(),
            constraints.maxHeight.toFloat(),
            maxLines,
            if (ellipsis) NSLineBreakByTruncatingTail else NSLineBreakByWordWrapping
        )
    }

    /**
     * 将[TextStyle]转换成IOS上的[TMMComposeTextAttributes]
     */
    private fun TextStyle.toTMMComposeTextAttributes(content: String): TMMComposeTextAttributes {
        val textAttributes = TMMComposeTextAttributes()
        textAttributes.content = content
        textAttributes.fontSize = fontSize.value.toInt()
        textAttributes.fontWeight = fontWeight?.toUIFontWeight() ?: UIFontWeightRegular
        textAttributes.align = textAlign.toNSTextAlignment()
        textAttributes.lineHeight = lineHeight.value
        textAttributes.letterSpace = letterSpacing.toSp(textAttributes.fontSize)
        textAttributes.backgroundColor = background.value
        textAttributes.italicType =
            fontStyle?.toTMMNativeItalicType() ?: TMMNativeItalicType.TMMNativeItalicNone
        textAttributes.spanStyles = createSpanStyleList()
        textAttributes.textDecorator = textDecoration?.toTMMNativeDecorator()
            ?: TMMNativeTextDecorator.TMMNativeTextDecoratorNone
        return textAttributes
    }

    /**
     * 将富文本信息转换成[TMMComposeTextSpanAttributes]数组
     */
    private fun createSpanStyleList(): List<TMMComposeTextSpanAttributes> {
        if (spanStyles.isEmpty()) {
            return emptyList()
        }
        return MutableList(spanStyles.size) { index ->
            spanStyles[index].toTMMComposeTextSpanStyle()
        }
    }


    /**
     * 将[Range<SpanStyle>]转换成[TMMComposeTextSpanAttributes]
     */
    private fun Range<SpanStyle>.toTMMComposeTextSpanStyle(): TMMComposeTextSpanAttributes {
        return TMMComposeTextSpanAttributes(
            start = this.start,
            end = this.end,
            fontSize = this.item.fontSize.value.toInt(),
            fontWeight = this.item.fontWeight?.toUIFontWeight() ?: 0.0,
            // 默认的letterSpace是Unspecified，是Nan
            letterSpace = this.item.letterSpacing.value,
            fontFamily = null,
            foregroundColor = this.item.color.value,
            backgroundColor = this.item.background.value,
            // 由于富文本展示的斜体效果需要与整体文本设置结合展示，所以需要判断外部是否有设置斜体
            italicType = this.item.fontStyle?.toTMMNativeItalicType()
                ?: TMMNativeItalicType.TMMNativeItalicNone,
            textDecorator = this.item.textDecoration?.toTMMNativeDecorator()
                ?: TMMNativeTextDecorator.TMMNativeTextDecoratorNone
        )
    }

    companion object {
        // Compose允许的最大宽度与高度
        private const val MAX_BOUNDS = ((1 shl 24) - 1).toFloat()
    }
}
