package androidx.compose.ui.platform.v2.nativefoundation

import androidx.compose.runtime.DeleteRedundantGraphicsLayer
import androidx.compose.runtime.EnableIOSParagraph
import androidx.compose.runtime.EnableLocaleListCachedHashCode
import androidx.compose.runtime.monitor.ComposeDiagnosticMonitor
import androidx.compose.ui.graphics.NativePathMeasure
import androidx.compose.ui.graphics.setNativePathFactory
import androidx.compose.ui.graphics.setNativeShaderFactory
import androidx.compose.ui.graphics.setPathMeasureFactory
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.IOSParagraph
import androidx.compose.ui.text.IOSParagraphIntrinsics
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.PlatformParagraphFactory
import androidx.compose.ui.text.platform.platformParagraphFactory
import androidx.compose.ui.uikit.RenderBackend
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

private var didInject = false

internal fun injectForCompose(renderBackend: RenderBackend) {

    if (didInject) return
    didInject = true

    /* State 并发冲突修复开关 */
    ComposeDiagnosticMonitor.fallbackStateError = true
    ComposeDiagnosticMonitor.stateMergeConflictFix = true

    /* iOS 平台去掉多余的文本的 GraphicsLayer  */
    DeleteRedundantGraphicsLayer = true

    /* 开启 LocaleList hash 缓存 */
    EnableLocaleListCachedHashCode = true

    /* 注入 iOS 平台的 Path */
    setNativePathFactory {
        NativePathImpl()
    }

    /* 注入 iOS 平台的 Shader */
    setNativeShaderFactory(NativeShaderFactoryImpl)

    setPathMeasureFactory { NativePathMeasure() }


    /*注入 iOS 平台的 Paragraph */
    platformParagraphFactory = object : PlatformParagraphFactory {
        override fun createParagraph(
            intrinsics: ParagraphIntrinsics,
            maxLines: Int,
            ellipsis: Boolean,
            constraints: Constraints
        ): Paragraph? = if (renderBackend == RenderBackend.UIView && EnableIOSParagraph) IOSParagraph(
            intrinsics as IOSParagraphIntrinsics,
            maxLines,
            ellipsis,
            constraints
        ) else null

        override fun createParagraphIntrinsics(
            text: String,
            style: TextStyle,
            spanStyles: List<AnnotatedString.Range<SpanStyle>>,
            placeholders: List<AnnotatedString.Range<Placeholder>>,
            density: Density,
            fontFamilyResolver: FontFamily.Resolver
        ): ParagraphIntrinsics? =
            if (renderBackend == RenderBackend.UIView && EnableIOSParagraph) IOSParagraphIntrinsics(
                text,
                style,
                spanStyles,
                placeholders,
                density,
                fontFamilyResolver
            ) else null
    }
}