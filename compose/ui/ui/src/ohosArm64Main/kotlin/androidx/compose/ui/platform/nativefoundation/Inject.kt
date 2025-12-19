package androidx.compose.ui.platform.nativefoundation

import androidx.compose.common.interop.LogPrintUtil
import androidx.compose.common.interop.TraceUtil
import androidx.compose.runtime.EnableOHOSParagraph
import androidx.compose.ui.arkui.RenderingBackend
import androidx.compose.ui.graphics.setNativePathFactory
import androidx.compose.ui.graphics.setNativeShaderFactory
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.OHOSParagraph
import androidx.compose.ui.text.OHOSParagraphIntrinsics
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.platform.PlatformParagraphFactory
import androidx.compose.ui.text.platform.platformParagraphFactory
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density

private var didInject = false

internal fun injectForCompose(renderBackend: RenderingBackend) {

    if (didInject) return
    didInject = true

    /* 注入 OHOS 平台的 Shader */
    setNativeShaderFactory(NativeShaderFactoryImpl)

    setNativePathFactory {
        NativePathImpl()
    }

    EnableOHOSParagraph = false

    // TODO 暂时先将Compose在鸿蒙平台的日志开关放在此处，后续可以通过配置编译选项来控制
    LogPrintUtil.isLogEnabled = true
    TraceUtil.isTraceEnabled = true

    /*注入 OHOS 平台的 Paragraph */
    platformParagraphFactory = object : PlatformParagraphFactory {
        override fun createParagraph(
            intrinsics: ParagraphIntrinsics,
            maxLines: Int,
            ellipsis: Boolean,
            constraints: Constraints
        ): Paragraph? =
            if (RenderingBackendContext.current() == RenderingBackend.ArkUIRenderNode
                && EnableOHOSParagraph
            ) OHOSParagraph(
                intrinsics as OHOSParagraphIntrinsics,
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
            if (RenderingBackendContext.current() == RenderingBackend.ArkUIRenderNode && EnableOHOSParagraph) {
                OHOSParagraphIntrinsics(
                    text,
                    style,
                    spanStyles,
                    placeholders,
                    density,
                    fontFamilyResolver
                )
            } else {
                null
            }
    }
}
