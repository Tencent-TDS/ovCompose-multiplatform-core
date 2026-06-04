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

package androidx.compose.ui.platform.v2.nativefoundation

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