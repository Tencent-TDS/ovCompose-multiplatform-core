package androidx.compose.ui.text

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isUnspecified
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Density
import kotlin.math.abs

/**
 * OHOSParagraphLayouter manages the layout and styling of text paragraphs in OpenHarmony OS.
 *
 * This class is responsible for:
 * Managing paragraph layout with support for max lines and ellipsis
 * Handling text styling including color, shadow, decoration, and brush effects
 * Caching paragraph instances for performance optimization
 * Providing width-based layout operations
 *
 * @param text The text content to be laid out
 * @param textDirection The resolved text direction for the paragraph
 * @param style The base text style to apply
 * @param spanStyles List of span styles to apply to specific text ranges
 * @param placeholders List of placeholder ranges for inline content
 * @param density The density context for converting dimensions
 * @param fontFamilyResolver Resolver for font family lookups
 **/
internal class OHOSParagraphLayouter(
    val text: String,
    textDirection: ResolvedTextDirection,
    style: TextStyle,
    spanStyles: List<AnnotatedString.Range<SpanStyle>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
) {
    private val builder = ParagraphBuilder(
        fontFamilyResolver = fontFamilyResolver,
        text = text,
        textStyle = style,
        spanStyles = spanStyles,
        placeholders = placeholders,
        density = density,
        textDirection = textDirection
    )
    private var paragraphCache: OHOSNativeParagraphProxy? = null
    private var width: Float = Float.NaN

    //    val defaultFont get() = builder.defaultFont
    val textStyle get() = builder.textStyle

//    internal fun emptyLineMetrics(paragraph: Paragraph): Array<LineMetrics> =
//        builder.emptyLineMetrics(paragraph)

    fun setParagraphStyle(
        maxLines: Int,
        ellipsis: String
    ) {
        if (builder.maxLines != maxLines ||
            builder.ellipsis != ellipsis
        ) {
            builder.maxLines = maxLines
            builder.ellipsis = ellipsis
            paragraphCache = null
        }
    }

    fun setTextStyle(
        color: Color,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        val actualColor = color.takeOrElse { builder.textStyle.color }
        if (builder.textStyle.color != actualColor ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            builder.textStyle = builder.textStyle.copy(
                color = actualColor,
                shadow = shadow,
                textDecoration = textDecoration
            )
            paragraphCache = null
        }
    }

    @ExperimentalTextApi
    fun setTextStyle(
        brush: Brush?,
        brushSize: Size,
        alpha: Float,
        shadow: Shadow?,
        textDecoration: TextDecoration?
    ) {
        val actualSize = builder.brushSize
        if (builder.textStyle.brush != brush ||
            actualSize.isUnspecified ||
            !actualSize.width.sameValueAs(brushSize.width) ||
            !actualSize.height.sameValueAs(brushSize.height) ||
            !builder.textStyle.alpha.sameValueAs(alpha) ||
            builder.textStyle.shadow != shadow ||
            builder.textStyle.textDecoration != textDecoration
        ) {
            builder.textStyle = builder.textStyle.copy(
                brush = brush,
                alpha = alpha,
                shadow = shadow,
                textDecoration = textDecoration
            )
            builder.brushSize = brushSize
            paragraphCache = null
        }
    }

    fun setDrawStyle(drawStyle: DrawStyle?) {
        if (builder.drawStyle != drawStyle) {
            builder.drawStyle = drawStyle
            paragraphCache = null
        }
    }

    fun setBlendMode(blendMode: BlendMode) {
        if (builder.blendMode != blendMode) {
            builder.blendMode = blendMode
            paragraphCache = null
        }
    }

    /**
     * Lays out the paragraph with the specified width.
     *
     * This method manages paragraph layout and caching:
     *  - If a cached paragraph exists and the width hasn't changed, it reuses the cache
     *  - If the width has changed, it re-layouts the existing paragraph with the new width
     *  - If no cache exists, it builds a new paragraph and caches it for future use
     *  @param width The width constraint for laying out the paragraph
     *  @return The laid out paragraph instance
     */
    fun layoutParagraph(width: Float): OHOSNativeParagraphProxy {
        val paragraph = paragraphCache
        return if (paragraph != null) {
            if (!this.width.sameValueAs(width)) {
                this.width = width
                paragraph.layout(width.toDouble())
            }
            paragraph
        } else {
            builder.build().apply {
                paragraphCache = this
                layout(width.toDouble())
            }
        }
    }
}

private fun Float.sameValueAs(other: Float): Boolean {
    return abs(this - other) < 0.00001f
}
