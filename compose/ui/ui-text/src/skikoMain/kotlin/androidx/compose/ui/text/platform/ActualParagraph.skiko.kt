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

@file:JvmName("SkiaParagraph_skikoKt")
@file:JvmMultifileClass

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.AnnotatedString.Range
import androidx.compose.ui.text.Paragraph
import androidx.compose.ui.text.ParagraphIntrinsics
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.SkiaParagraph
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.ceilToInt
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.jvm.JvmMultifileClass
import kotlin.jvm.JvmName

@Suppress("DEPRECATION")
@Deprecated(
    "Font.ResourceLoader is deprecated, instead pass FontFamily.Resolver",
    replaceWith =
        ReplaceWith(
            "ActualParagraph(text, style, spanStyles, placeholders, " +
                "maxLines, ellipsis, width, density, fontFamilyResolver)"
        ),
)
internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    ellipsis: Boolean,
    width: Float,
    density: Density,
    @Suppress("DEPRECATION") resourceLoader: Font.ResourceLoader
): Paragraph =
    SkiaParagraph(
        SkiaParagraphIntrinsics(
            text = text,
            style = style,
            placeholders = placeholders,
            annotations = annotations,
            fontFamilyResolver = createFontFamilyResolver(resourceLoader),
            density = density
        ),
        maxLines,
        if (ellipsis) TextOverflow.Ellipsis else TextOverflow.Clip,
        Constraints(maxWidth = width.ceilToInt())
    )

internal actual fun ActualParagraph(
    text: String,
    style: TextStyle,
    annotations: List<AnnotatedString.Range<out AnnotatedString.Annotation>>,
    placeholders: List<AnnotatedString.Range<Placeholder>>,
    maxLines: Int,
    overflow: TextOverflow,
    constraints: Constraints,
    density: Density,
    fontFamilyResolver: FontFamily.Resolver
): Paragraph =
    SkiaParagraph(
        SkiaParagraphIntrinsics(
            text = text,
            style = style,
            placeholders = placeholders,
            annotations = annotations,
            fontFamilyResolver = fontFamilyResolver,
            density = density
        ),
        maxLines,
        overflow,
        constraints
    )

internal actual fun ActualParagraph(
    paragraphIntrinsics: ParagraphIntrinsics,
    maxLines: Int,
    overflow: TextOverflow,
    constraints: Constraints
): Paragraph =
    SkiaParagraph(
        paragraphIntrinsics as SkiaParagraphIntrinsics,
        maxLines,
        overflow,
        constraints
    )
