package androidx.compose.ui.text

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.ui.arkui.utils.ParagraphBuilder_addSpanStyle
import androidx.compose.ui.arkui.utils.ParagraphBuilder_build
import androidx.compose.ui.arkui.utils.ParagraphBuilder_create
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setColor
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setEllipsis
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setFontSize
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setFontStyle
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setFontWeight
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setLetterSpacing
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setLineHeight
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setMaxLines
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setNeedEllipsis
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setText
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setTextAlign
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setTextDirection
import androidx.compose.ui.arkui.utils.ParagraphBuilder_setWordSpacing
import androidx.compose.ui.arkui.utils.Paragraph_CreateSpanStyleRange
import androidx.compose.ui.arkui.utils.Paragraph_DestroySpanStyleRange
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setBackground
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setColor
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setDecoration
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setFontFamily
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setFontFeatureSettings
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setFontSize
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setFontStyle
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setFontWeight
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setLetterSpacing
import androidx.compose.ui.arkui.utils.Paragraph_SpanStyleRange_setShadow
import androidx.compose.ui.arkui.utils.SpanStyleRange_Handle
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.GenericFontFamily
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.unit.Density
import kotlinx.cinterop.memScoped
import kotlin.native.ref.createCleaner

/**
 * ParagraphBuilder负责构建Native段落适配器
 *
 * 设计思路：
 * 1. 使用构建者模式封装复杂的参数设置过程
 * 2. 提供类型安全的接口，避免参数顺序错误
 * 3. 支持富文本样式和占位符
 * 4. 使用工厂方法创建Native适配器实例
 *
 * 职责：
 * - 收集所有段落相关参数
 * - 转换Kotlin类型为Native可识别的格式
 * - 调用Native工厂方法创建段落适配器
 */
internal class ParagraphBuilder(
    val fontFamilyResolver: FontFamily.Resolver,
    val text: String,
    var textStyle: TextStyle,
    var brushSize: Size = Size.Unspecified,
    var ellipsis: String = "",
    var maxLines: Int = Int.MAX_VALUE,
    val spanStyles: List<Range<SpanStyle>>,
    val placeholders: List<Range<Placeholder>>,
    val density: Density,
    val textDirection: ResolvedTextDirection,
    var drawStyle: DrawStyle? = null,
    var blendMode: BlendMode = DrawScope.DefaultBlendMode
) {


    fun build(): OHOSNativeParagraphProxy {
        val param = createParagraphParams()
        return create(param)
            ?: throw IllegalStateException("Failed to create OHOSNativeParagraphProxy with params: $param")
    }

    /**
     * 创建Native段落适配器
     *
     * @param params 段落参数
     * @return Native适配器实例，创建失败返回null
     */
    fun create(params: NativeParagraphParams): OHOSNativeParagraphProxy? {
        val handle = memScoped {
            // 在这个作用域内：
            // - encodeToByteArray() 可能会分配临时 native 内存
            // - C 函数调用可能需要临时缓冲区
            ParagraphBuilder_create()?.let {
                ParagraphBuilder_setText(
                    it, params.text, params.text.encodeToByteArray().size.toUInt()
                )
                ParagraphBuilder_setFontSize(it, params.fontSize)
                ParagraphBuilder_setFontWeight(it, params.fontWeight.toUInt())
                ParagraphBuilder_setFontStyle(it, params.fontStyle)
                ParagraphBuilder_setColor(it, params.color)
                ParagraphBuilder_setTextAlign(it, params.textAlign)
                ParagraphBuilder_setMaxLines(it, params.maxLines.toUInt())
                ParagraphBuilder_setTextDirection(it, params.textDirection)
                ParagraphBuilder_setNeedEllipsis(it, params.needEllipsis)
                ParagraphBuilder_setEllipsis(
                    it,
                    params.ellipsis,
                    params.ellipsis.encodeToByteArray().size.toUInt()
                )
                ParagraphBuilder_setLetterSpacing(it, params.letterSpacing)
                ParagraphBuilder_setWordSpacing(it, params.wordSpacing)
                ParagraphBuilder_setLineHeight(it, params.lineHeight)
                params.spanStyles.forEach { spanStyle ->
                    ParagraphBuilder_addSpanStyle(it, spanStyle.handle)
                }
                ParagraphBuilder_build(it)
            }
        }
        LogPrintUtil.verbose("[Paragraph] OHOSNativeParagraphProxy::create, Creating NativeParagraph: $handle with params: $params")
        return handle?.let { OHOSNativeParagraphProxy(it) }
    }

    /**
     * 构建 NativeParagraphParams 参数对象
     * 负责将 Kotlin 类型和样式信息转换为 Native 可识别的参数格式
     */
    private fun createParagraphParams(): NativeParagraphParams {
        // 转换字体大小（考虑density）
        val densityValue = density.density
        val fontSizeInPx = if (textStyle.fontSize.value > 0) {
            textStyle.fontSize.value * densityValue
        } else {
            16.0f * densityValue  // 默认字体大小
        }
        // 转换行高（考虑density）
        val lineHeightInPx = if (textStyle.lineHeight.value > 0) {
            textStyle.lineHeight.value * densityValue
        } else {
            0.0f  // 0表示使用默认行高
        }

        // 转换字母间距（安全处理无限值）
        val letterSpacingValue = if (textStyle.letterSpacing.value.isFinite()) {
            textStyle.letterSpacing.value
        } else {
            0.0f
        }


        var needEllipsis = false
        val maxLinesCount = maxLines
        if (maxLinesCount != Int.MAX_VALUE) {
            needEllipsis = false
        } else {
            Int.MAX_VALUE
        }

        // TODO: 处理字体族（使用 fontFamilyResolver 解析）
        val resolvedFontFamily = textStyle.fontFamily?.let { family ->
            // 使用 fontFamilyResolver 解析字体族名称
            // 注意：这里简化处理，实际应该通过 resolver 获取字体资源
            when (family) {
                is GenericFontFamily -> family.name
                is FontListFontFamily -> {
                    // 对于字体列表，取第一个字体的family name
                    // 实际项目中应该通过 fontFamilyResolver 加载字体
                    family.fonts.firstOrNull()?.toString() ?: "sans-serif"
                }

                else -> "sans-serif"
            }
        }

        // 转换 spanStyles 为 Native 可识别的格式
        val nativeSpanStyles = spanStyles.map { range ->
            val spanStyle = range.item
            LogPrintUtil.verbose("ParagraphBuilder::createParagraphParams, Converting SpanStyle: $spanStyle in range: $range")
            val nativeSpanStyleRange = NativeSpanStyleRange(range.start, range.end).apply {
                fontWeight =
                    spanStyle.fontWeight?.let { StyleMapperRegistry.fontWeight.map(it) } ?: 0
                fontStyle = spanStyle.fontStyle?.let { StyleMapperRegistry.fontStyle.map(it) } ?: 0
                fontSize = (spanStyle.fontSize.takeIf { it.value > 0 }
                    ?.let { it.value.toDouble() * densityValue }) ?: 0.0
                fontFamily = spanStyle.fontFamily?.let { family ->
                    when (family) {
                        is GenericFontFamily -> family.name
                        is FontListFontFamily -> {
                            family.fonts.firstOrNull()?.toString() ?: "sans-serif"
                        }

                        else -> "sans-serif"
                    }
                } ?: ""
                color = spanStyle.color.toUInt()
                background = spanStyle.background.toUInt()
                letterSpacing =
                    spanStyle.letterSpacing.takeIf { it.value.isFinite() }?.value?.toDouble() ?: 0.0
                textDecoration = spanStyle.textDecoration?.let {
                    StyleMapperRegistry.textDecoration.map(
                        it
                    )
                } ?: 0
                shadow = spanStyle.shadow?.let { shadow ->
                    OHOSNativeShadow(
                        color = shadow.color.toUInt(),
                        offsetX = shadow.offset.x,
                        offsetY = shadow.offset.y,
                        blurRadius = shadow.blurRadius.toDouble()
                    )
                }
            }
            LogPrintUtil.verbose("ParagraphBuilder::createParagraphParams,  NativeSpanStyle: $nativeSpanStyleRange")
            nativeSpanStyleRange
        }

        // 转换 placeholders 为 Native 可识别的格式
        val nativePlaceholders = placeholders.map { range ->
            val placeholder = range.item
            PlaceholderRange(
                start = range.start,
                end = range.end,
                width = placeholder.width.value * densityValue,
                height = placeholder.height.value * densityValue,
                placeholderVerticalAlign = when (placeholder.placeholderVerticalAlign) {
                    PlaceholderVerticalAlign.AboveBaseline -> 0
                    PlaceholderVerticalAlign.Top -> 1
                    PlaceholderVerticalAlign.Bottom -> 2
                    PlaceholderVerticalAlign.Center -> 3
                    PlaceholderVerticalAlign.TextTop -> 4
                    PlaceholderVerticalAlign.TextBottom -> 5
                    PlaceholderVerticalAlign.TextCenter -> 6
                    else -> 3 // 默认居中
                }
            )
        }

        return NativeParagraphParams(
            text = text,
            fontSize = fontSizeInPx.toDouble(),
            fontWeight = StyleMapperRegistry.fontWeight.map(textStyle.fontWeight),
            fontStyle = StyleMapperRegistry.fontStyle.map(textStyle.fontStyle),
            color = textStyle.color.toUInt(),
            textAlign = StyleMapperRegistry.textAlign.map(textStyle.textAlign),
            textDirection = StyleMapperRegistry.textDirection.map(textDirection),
            needEllipsis = needEllipsis,
            ellipsis = ellipsis,
            letterSpacing = letterSpacingValue.toDouble(),
            wordSpacing = 0.0,
            lineHeight = lineHeightInPx.toDouble(),
            spanStyles = nativeSpanStyles,  // 传递富文本样式
            placeholders = nativePlaceholders,  // 传递占位符
            maxLines = maxLinesCount,
            fontFamily = resolvedFontFamily  // 传递解析后的字体族
        )
    }
}

/**
 * Native段落参数数据类
 *
 * 设计目的：
 * 1. 封装所有创建参数，避免过长的参数列表
 * 2. 提供默认值，简化调用
 * 3. 类型安全，避免参数顺序错误
 * 4. 支持富文本样式（spanStyles）和占位符（placeholders）
 */
internal data class NativeParagraphParams(
    val text: String,
    val fontSize: Double,
    val fontWeight: Int,
    val fontStyle: Int,
    val color: UInt,
    val textAlign: Int,
    val textDirection: Int,
    val needEllipsis: Boolean,
    val letterSpacing: Double = 0.0,
    val wordSpacing: Double = 0.0,
    val lineHeight: Double = 0.0,
    // 新增：支持富文本样式
    val spanStyles: List<NativeSpanStyleRange> = emptyList(),
    // 支持占位符
    val placeholders: List<PlaceholderRange> = emptyList(),
    // 字体家族名称（由fontFamilyResolver解析后传入）
    val fontFamily: String? = null,
    val maxLines: Int,
    val ellipsis: String
)

/**
 * SpanStyle范围数据类
 * 用于表示文本中的局部样式
 */
internal class NativeSpanStyleRange(val start: Int, val end: Int) {

    val handle: SpanStyleRange_Handle? = Paragraph_CreateSpanStyleRange(
        start,
        end
    ).apply {
        requireNotNull(this) { "Paragraph_CreateSpanStyleRange failed" }
    }

    /**
     * 自动资源清理器
     * 使用createCleaner确保Native资源在对象被GC时自动释放
     */
    @Suppress("unused")
    private val cleaner = createCleaner(handle) { ptr ->
        if (ptr != null) {
            Paragraph_DestroySpanStyleRange(ptr)
        }
    }

    var fontWeight: Int = 0
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setFontWeight(it, value) }
            field = value
        }
    var fontStyle: Int = 0
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setFontStyle(it, value) }
            field = value
        }
    var fontSize: Double = 0.0
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setFontSize(it, value) }
            field = value
        }

    // TODO: 暂不支持字体合成
    val fontSynthesis: Int = 0
    var fontFamily: String? = null
        set(value) {
            if (value != null) {
                handle?.let {
                    Paragraph_SpanStyleRange_setFontFamily(it, value)
                }
            }
            field = value
        }

    var fontFeatureSetting: String? = null
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setFontFeatureSettings(it, value) }
            field = value
        }

    var color: UInt? = null
        set(value) {
            handle?.let {
                if (value != null) {
                    Paragraph_SpanStyleRange_setColor(it, value)
                }
            }
            field = value
        }

    var background: UInt? = null
        set(value) {
            handle?.let {
                if (value != null) {
                    Paragraph_SpanStyleRange_setBackground(it, value)
                }
            }
            field = value
        }

    var letterSpacing: Double = 0.0
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setLetterSpacing(it, value) }
            field = value
        }

    var textDecoration: Int = 0
        set(value) {
            handle?.let { Paragraph_SpanStyleRange_setDecoration(it, value) }
            field = value
        }

    var shadow: OHOSNativeShadow? = null
        set(value) {
            if (value != null) {
                handle?.let {
                    Paragraph_SpanStyleRange_setShadow(
                        it,
                        value.offsetX,
                        value.offsetY,
                        value.blurRadius,
                        value.color
                    )
                }
            }
            field = value
        }

    override fun toString(): String {
        return "NativeSpanStyleRange(start=$start, end=$end, fontWeight=$fontWeight, " +
                "fontStyle=$fontStyle, fontSize=$fontSize, fontFamily=$fontFamily, " +
                "fontFeatureSetting=$fontFeatureSetting, color=$color, background=$background, " +
                "letterSpacing=$letterSpacing, textDecoration=$textDecoration, shadow=$shadow)"
    }
}

data class OHOSNativeShadow(
    val color: UInt,
    val offsetX: Float,
    val offsetY: Float,
    val blurRadius: Double
)

/**
 * Placeholder范围数据类
 * 用于表示文本中的内联占位符（如图片）
 */
data class PlaceholderRange(
    val start: Int,
    val end: Int,
    val width: Float,
    val height: Float,
    val placeholderVerticalAlign: Int
)
