/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.foundation.text

import androidx.compose.foundation.FastTraceTag
import androidx.compose.foundation.OVMPFastLogPhase
import androidx.compose.foundation.fastLog
import androidx.compose.foundation.fastLogAllocTraceId
import androidx.compose.foundation.text.modifiers.SelectableTextAnnotatedStringElement
import androidx.compose.foundation.text.modifiers.SelectionController
import androidx.compose.foundation.text.modifiers.TextAnnotatedStringElement
import androidx.compose.foundation.text.modifiers.TextStringSimpleElement
import androidx.compose.foundation.text.selection.LocalSelectionRegistrar
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionRegistrar
import androidx.compose.foundation.text.selection.hasSelection
import androidx.compose.foundation.utf16Head4AsLong
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.isDebugInspectorInfoEnabled
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastMapIndexedNotNull
import kotlin.math.floor
import kotlin.math.roundToInt

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param color Overrides the text color provided in [style]
 */
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    color: ColorProducer? = null
) {
    // region Tencent Code
    val traceId = if (ComposeTabService.composeFastLogEnable) {
        val id = fastLogAllocTraceId()
        fastLog(
            FastTraceTag.Text,
            id,
            text.utf16Head4AsLong(),
            OVMPFastLogPhase.TextFunc.value,
            text.hashCode().toLong(),
            text.length.toLong(),
        )
        id
    } else 0L
    // end region
    validateMinMaxLines(
        minLines = minLines,
        maxLines = maxLines
    )
    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController = if (selectionRegistrar != null) {
        val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
        val selectableId =
            rememberSaveable(selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
                selectionRegistrar.nextSelectableId()
            }
        remember(selectableId, selectionRegistrar, backgroundSelectionColor) {
            SelectionController(
                selectableId,
                selectionRegistrar,
                backgroundSelectionColor
            )
        }
    } else {
        null
    }
    val finalModifier = if (selectionController != null || onTextLayout != null) {
        // region Tencent Code Modify
        /*
        modifier
        .textModifier(
            AnnotatedString(text = text),
            style = style,
            onTextLayout = onTextLayout,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            fontFamilyResolver = LocalFontFamilyResolver.current,
            placeholders = null,
            onPlaceholderLayout = null,
            selectionController = selectionController,
            color = color
        )
         */
        modifier.run {
            if (isDebugInspectorInfoEnabled) modifier.graphicsLayer()
            else this
        }
            .textModifier(
                AnnotatedString(text = text),
                style = style,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                fontFamilyResolver = LocalFontFamilyResolver.current,
                placeholders = null,
                onPlaceholderLayout = null,
                selectionController = selectionController,
                color = color
            )
        // endregion
    } else {
        // region Tencent Code Modify
        /*
        modifier then TextStringSimpleElement(
            text = text,
            style = style,
            fontFamilyResolver = LocalFontFamilyResolver.current,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            color = color
        )
         */
        modifier.run {
            if (isDebugInspectorInfoEnabled) modifier.graphicsLayer()
            else this
        } then TextStringSimpleElement(
            text = text,
            traceId = traceId,
            style = style,
            fontFamilyResolver = LocalFontFamilyResolver.current,
            overflow = overflow,
            softWrap = softWrap,
            maxLines = maxLines,
            minLines = minLines,
            color = color
        )
        // end region
    }
    Layout(finalModifier, EmptyMeasurePolicy)
}

/**
 * Basic element that displays text and provides semantics / accessibility information.
 * Typically you will instead want to use [androidx.compose.material.Text], which is
 * a higher level Text element that contains semantics and consumes style information from a theme.
 *
 * @param text The text to be displayed.
 * @param modifier [Modifier] to apply to this layout node.
 * @param style Style configuration for the text such as color, font, line height etc.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw selection around the text.
 * @param overflow How visual overflow should be handled.
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space. If [softWrap] is false,
 * [overflow] and TextAlign may have unexpected effects.
 * @param maxLines An optional maximum number of lines for the text to span, wrapping if
 * necessary. If the text exceeds the given number of lines, it will be truncated according to
 * [overflow] and [softWrap]. It is required that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param inlineContent A map store composables that replaces certain ranges of the text. It's
 * used to insert composables into text layout. Check [InlineTextContent] for more information.
 * @param color Overrides the text color provided in [style]
 */
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
    color: ColorProducer? = null
) {
    validateMinMaxLines(
        minLines = minLines,
        maxLines = maxLines
    )
    val selectionRegistrar = LocalSelectionRegistrar.current
    val selectionController = if (selectionRegistrar != null) {
        val backgroundSelectionColor = LocalTextSelectionColors.current.backgroundColor
        val selectableId =
            rememberSaveable(selectionRegistrar, saver = selectionIdSaver(selectionRegistrar)) {
                selectionRegistrar.nextSelectableId()
            }
        remember(selectableId, selectionRegistrar, backgroundSelectionColor) {
            SelectionController(
                selectableId,
                selectionRegistrar,
                backgroundSelectionColor
            )
        }
    } else {
        null
    }
    if (!text.hasInlineContent()) {
        // this is the same as text: String, use all the early exits
        // region Tencent Code Modify
        /*
        modifier = modifier
                .textModifier(
                    text = text,
                    style = style,
                    onTextLayout = onTextLayout,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    fontFamilyResolver = LocalFontFamilyResolver.current,
                    placeholders = null,
                    onPlaceholderLayout = null,
                    selectionController = selectionController,
                    color = color
                )
         */
        Layout(
            modifier = modifier.run {
                if (isDebugInspectorInfoEnabled) modifier.graphicsLayer()
                else this
            }
                .textModifier(
                    text = text,
                    style = style,
                    onTextLayout = onTextLayout,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    fontFamilyResolver = LocalFontFamilyResolver.current,
                    placeholders = null,
                    onPlaceholderLayout = null,
                    selectionController = selectionController,
                    color = color
                ),
            EmptyMeasurePolicy
        )
        // endregion
    } else {
        // do the inline content allocs
        val (placeholders, inlineComposables) = text.resolveInlineContent(
            inlineContent = inlineContent
        )
        val measuredPlaceholderPositions = remember<MutableState<List<Rect?>?>> {
            mutableStateOf(null)
        }
        // region Tencent Code Modify
        /*
        modifier = modifier
                .textModifier(
                text = text,
                style = style,
                onTextLayout = onTextLayout,
                overflow = overflow,
                softWrap = softWrap,
                maxLines = maxLines,
                minLines = minLines,
                fontFamilyResolver = LocalFontFamilyResolver.current,
                placeholders = placeholders,
                onPlaceholderLayout = { measuredPlaceholderPositions.value = it },
                selectionController = selectionController,
                color = color
            ),
            measurePolicy = TextMeasurePolicy { measuredPlaceholderPositions.value }
        )
         */
        Layout(
            content = { InlineChildren(text = text, inlineContents = inlineComposables) },
            modifier = modifier.run {
                if (isDebugInspectorInfoEnabled) modifier.graphicsLayer()
                else this
            }
                .textModifier(
                    text = text,
                    style = style,
                    onTextLayout = onTextLayout,
                    overflow = overflow,
                    softWrap = softWrap,
                    maxLines = maxLines,
                    minLines = minLines,
                    fontFamilyResolver = LocalFontFamilyResolver.current,
                    placeholders = placeholders,
                    onPlaceholderLayout = { measuredPlaceholderPositions.value = it },
                    selectionController = selectionController,
                    color = color
                ),
            measurePolicy = TextMeasurePolicy { measuredPlaceholderPositions.value }
        )
        // endregion
    }
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        minLines = 1,
        maxLines = maxLines
    )
}

@Deprecated("Maintained for binary compatibility", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    inlineContent: Map<String, InlineTextContent> = mapOf(),
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        onTextLayout = onTextLayout,
        overflow = overflow,
        softWrap = softWrap,
        minLines = 1,
        maxLines = maxLines,
        inlineContent = inlineContent
    )
}

@Deprecated("Maintained for binary compat", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1
) = BasicText(text, modifier, style, onTextLayout, overflow, softWrap, maxLines, minLines)

@Deprecated("Maintained for binary compat", level = DeprecationLevel.HIDDEN)
@Composable
fun BasicText(
    text: AnnotatedString,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    onTextLayout: ((TextLayoutResult) -> Unit)? = null,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = 1,
    inlineContent: Map<String, InlineTextContent> = mapOf()
) = BasicText(
    text = text,
    modifier = modifier,
    style = style,
    onTextLayout = onTextLayout,
    overflow = overflow,
    softWrap = softWrap,
    maxLines = maxLines,
    minLines = minLines,
    inlineContent = inlineContent
)

/**
 * A custom saver that won't save if no selection is active.
 */
private fun selectionIdSaver(selectionRegistrar: SelectionRegistrar?) = Saver<Long, Long>(
    save = { if (selectionRegistrar.hasSelection(it)) it else null },
    restore = { it }
)

internal expect fun Modifier.textPointerHoverIcon(selectionRegistrar: SelectionRegistrar?): Modifier

private object EmptyMeasurePolicy : MeasurePolicy {
    private val placementBlock: Placeable.PlacementScope.() -> Unit = {}
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        return layout(constraints.maxWidth, constraints.maxHeight, placementBlock = placementBlock)
    }
}

private class TextMeasurePolicy(
    private val placements: () -> List<Rect?>?
) : MeasurePolicy {
    override fun MeasureScope.measure(
        measurables: List<Measurable>,
        constraints: Constraints
    ): MeasureResult {
        val toPlace = placements()?.fastMapIndexedNotNull { index, rect ->
            // PlaceholderRect will be null if it's ellipsized. In that case, the corresponding
            // inline children won't be measured or placed.
            rect?.let {
                Pair(
                    measurables[index].measure(
                        Constraints(
                            maxWidth = floor(it.width).toInt(),
                            maxHeight = floor(it.height).toInt()
                        )
                    ),
                    IntOffset(it.left.roundToInt(), it.top.roundToInt())
                )
            }
        }
        return layout(
            constraints.maxWidth,
            constraints.maxHeight,
        ) {
            toPlace?.fastForEach { (placeable, position) ->
                placeable.place(position)
            }
        }
    }
}

private fun Modifier.textModifier(
    text: AnnotatedString,
    style: TextStyle,
    onTextLayout: ((TextLayoutResult) -> Unit)?,
    overflow: TextOverflow,
    softWrap: Boolean,
    maxLines: Int,
    minLines: Int,
    fontFamilyResolver: FontFamily.Resolver,
    placeholders: List<AnnotatedString.Range<Placeholder>>?,
    onPlaceholderLayout: ((List<Rect?>) -> Unit)?,
    selectionController: SelectionController?,
    color: ColorProducer?
): Modifier {
    if (selectionController == null) {
        val staticTextModifier = TextAnnotatedStringElement(
            text,
            style,
            fontFamilyResolver,
            onTextLayout,
            overflow,
            softWrap,
            maxLines,
            minLines,
            placeholders,
            onPlaceholderLayout,
            null,
            color
        )
        return this then Modifier /* selection position */ then staticTextModifier
    } else {
        val selectableTextModifier = SelectableTextAnnotatedStringElement(
            text,
            style,
            fontFamilyResolver,
            onTextLayout,
            overflow,
            softWrap,
            maxLines,
            minLines,
            placeholders,
            onPlaceholderLayout,
            selectionController,
            color
        )
        return this then selectionController.modifier then selectableTextModifier
    }
}
