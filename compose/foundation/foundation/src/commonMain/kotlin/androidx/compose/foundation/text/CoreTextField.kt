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

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.SelectionHandleAnchor
import androidx.compose.foundation.text.selection.SelectionHandleInfo
import androidx.compose.foundation.text.selection.SelectionHandleInfoKey
import androidx.compose.foundation.text.selection.SimpleLayout
import androidx.compose.foundation.text.selection.TextFieldSelectionHandle
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.isSelectionHandleInVisibleBound
import androidx.compose.foundation.text.selection.textFieldMagnifier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.EnableIOSParagraph
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.RecomposeScope
import androidx.compose.runtime.currentRecomposeScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.FirstBaseline
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.LastBaseline
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalFontFamilyResolver
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalTextInputService
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.PlatformTextNodeFactory
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.semantics.copyText
import androidx.compose.ui.semantics.cutText
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.editableText
import androidx.compose.ui.semantics.getTextLayoutResult
import androidx.compose.ui.semantics.insertTextAtCursor
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onImeAction
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.password
import androidx.compose.ui.semantics.pasteText
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setSelection
import androidx.compose.ui.semantics.setText
import androidx.compose.ui.semantics.textSelectionRange
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteAllCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TextInputService
import androidx.compose.ui.text.input.TextInputSession
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Base composable that enables users to edit text via hardware or software keyboard.
 *
 * This composable provides basic text editing functionality, however does not include any
 * decorations such as borders, hints/placeholder.
 *
 * If the editable text is larger than the size of the container, the vertical scrolling
 * behaviour will be automatically applied. To enable a single line behaviour with horizontal
 * scrolling instead, set the [maxLines] parameter to 1, [softWrap] to false, and
 * [ImeOptions.singleLine] to true.
 *
 * Whenever the user edits the text, [onValueChange] is called with the most up to date state
 * represented by [TextFieldValue]. [TextFieldValue] contains the text entered by user, as well
 * as selection, cursor and text composition information. Please check [TextFieldValue] for the
 * description of its contents.
 *
 * It is crucial that the value provided in the [onValueChange] is fed back into [CoreTextField] in
 * order to have the final state of the text being displayed. Example usage:
 *
 * Please keep in mind that [onValueChange] is useful to be informed about the latest state of the
 * text input by users, however it is generally not recommended to modify the values in the
 * [TextFieldValue] that you get via [onValueChange] callback. Any change to the values in
 * [TextFieldValue] may result in a context reset and end up with input session restart. Such
 * a scenario would cause glitches in the UI or text input experience for users.
 *
 * @param value The [androidx.compose.ui.text.input.TextFieldValue] to be shown in the [CoreTextField].
 * @param onValueChange Called when the input service updates the values in [TextFieldValue].
 * @param modifier optional [Modifier] for this text field.
 * @param textStyle Style configuration that applies at character level such as color, font etc.
 * @param visualTransformation The visual transformation filter for changing the visual
 * representation of the input. By default no visual transformation is applied.
 * @param onTextLayout Callback that is executed when a new text layout is calculated. A
 * [TextLayoutResult] object that callback provides contains paragraph information, size of the
 * text, baselines and other details. The callback can be used to add additional decoration or
 * functionality to the text. For example, to draw a cursor or selection around the text.
 * @param interactionSource the [MutableInteractionSource] representing the stream of
 * [Interaction]s for this CoreTextField. You can create and pass in your own remembered
 * [MutableInteractionSource] if you want to observe [Interaction]s and customize the
 * appearance / behavior of this CoreTextField in different [Interaction]s.
 * @param cursorBrush [Brush] to paint cursor with. If [SolidColor] with [Color.Unspecified]
 * provided, there will be no cursor drawn
 * @param softWrap Whether the text should break at soft line breaks. If false, the glyphs in the
 * text will be positioned as if there was unlimited horizontal space.
 * @param maxLines The maximum height in terms of maximum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param minLines The minimum height in terms of minimum number of visible lines. It is required
 * that 1 <= [minLines] <= [maxLines].
 * @param imeOptions Contains different IME configuration options.
 * @param keyboardActions when the input service emits an IME action, the corresponding callback
 * is called. Note that this IME action may be different from what you specified in
 * [KeyboardOptions.imeAction].
 * @param enabled controls the enabled state of the text field. When `false`, the text
 * field will be neither editable nor focusable, the input of the text field will not be selectable
 * @param readOnly controls the editable state of the [CoreTextField]. When `true`, the text
 * field can not be modified, however, a user can focus it and copy text from it. Read-only text
 * fields are usually used to display pre-filled forms that user can not edit
 * @param decorationBox Composable lambda that allows to add decorations around text field, such
 * as icon, placeholder, helper messages or similar, and automatically increase the hit target area
 * of the text field. To allow you to control the placement of the inner text field relative to your
 * decorations, the text field implementation will pass in a framework-controlled composable
 * parameter "innerTextField" to the decorationBox lambda you provide. You must call
 * innerTextField exactly once.
 */
@Composable
@OptIn(InternalFoundationTextApi::class, ExperimentalFoundationApi::class)
internal fun CoreTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier,
    textStyle: TextStyle = TextStyle.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    onTextLayout: (TextLayoutResult) -> Unit = {},
    interactionSource: MutableInteractionSource? = null,
    cursorBrush: Brush = SolidColor(Color.Unspecified),
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
    minLines: Int = DefaultMinLines,
    imeOptions: ImeOptions = ImeOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    decorationBox: @Composable (innerTextField: @Composable () -> Unit) -> Unit =
        @Composable { innerTextField -> innerTextField() },
    textScrollerPosition: TextFieldScrollerPosition? = null,
) {
    val focusRequester = remember { FocusRequester() }
    /**
     * 平台文本的代理接口，用于代理原本Paragraph的实现
     */
    val platformTextDelegate =
        PlatformTextNodeFactory.instance.createPlatformDelegateTextNode()
    // CompositionLocals
    // If the text field is disabled or read-only, we should not deal with the input service
    val textInputService = LocalTextInputService.current
    val density = LocalDensity.current
    val fontFamilyResolver = LocalFontFamilyResolver.current
    val selectionBackgroundColor = LocalTextSelectionColors.current.backgroundColor
    val focusManager = LocalFocusManager.current
    val windowInfo = LocalWindowInfo.current
    val keyboardController = LocalSoftwareKeyboardController.current
    var localBitmap: ImageBitmap? = null
    var localCanvas: Canvas? = null
    // Scroll state
    val singleLine = maxLines == 1 && !softWrap && imeOptions.singleLine
    val orientation = if (singleLine) Orientation.Horizontal else Orientation.Vertical
    val scrollerPosition = textScrollerPosition ?: rememberSaveable(
        orientation,
        saver = TextFieldScrollerPosition.Saver
    ) { TextFieldScrollerPosition(orientation) }
    if (scrollerPosition.orientation != orientation) {
        throw IllegalArgumentException(
            "Mismatching scroller orientation; " + (
                if (orientation == Orientation.Vertical)
                    "only single-line, non-wrap text fields can scroll horizontally"
                else
                    "single-line, non-wrap text fields can only scroll horizontally"
                )
        )
    }

    // State
    val transformedText = remember(value, visualTransformation) {
        val transformed = visualTransformation.filterWithValidation(value.annotatedString)
        value.composition?.let {
            TextFieldDelegate.applyCompositionDecoration(it, transformed)
        } ?: transformed
    }

    val visualText = transformedText.text
    val offsetMapping = transformedText.offsetMapping

    // If developer doesn't pass new value to TextField, recompose won't happen but internal state
    // and IME may think it is updated. To fix this inconsistent state, enforce recompose.
    val scope = currentRecomposeScope
    val state = remember(keyboardController) {
        TextFieldState(
            TextDelegate(
                text = visualText,
                style = textStyle,
                softWrap = softWrap,
                density = density,
                fontFamilyResolver = fontFamilyResolver
            ),
            recomposeScope = scope,
            keyboardController = keyboardController
        )
    }
    state.update(
        value.annotatedString,
        visualText,
        textStyle,
        softWrap,
        density,
        fontFamilyResolver,
        onValueChange,
        keyboardActions,
        focusManager,
        selectionBackgroundColor
    )

    // notify the EditProcessor of value every recomposition
    state.processor.reset(value, state.inputSession)

    val undoManager = remember { UndoManager() }
    undoManager.snapshotIfNeeded(value)

    val manager = remember { TextFieldSelectionManager(undoManager) }
    manager.offsetMapping = offsetMapping
    manager.visualTransformation = visualTransformation
    manager.onValueChange = state.onValueChange
    manager.state = state
    manager.value = value
    manager.clipboardManager = LocalClipboardManager.current
    manager.textToolbar = LocalTextToolbar.current
    manager.hapticFeedBack = LocalHapticFeedback.current
    manager.focusRequester = focusRequester
    manager.editable = !readOnly

    val coroutineScope = rememberCoroutineScope()
    val bringIntoViewRequester = remember { BringIntoViewRequester() }

    // Focus
    val focusModifier = Modifier.textFieldFocusModifier(
        enabled = enabled,
        focusRequester = focusRequester,
        interactionSource = interactionSource
    ) {
        if (state.hasFocus == it.isFocused) {
            return@textFieldFocusModifier
        }
        state.hasFocus = it.isFocused

        if (textInputService != null) {
            if (state.hasFocus && enabled && !readOnly) {
                startInputSession(
                    textInputService,
                    state,
                    value,
                    imeOptions,
                    offsetMapping
                )
            } else {
                endInputSession(state)
            }

            // The focusable modifier itself will request the entire focusable be brought into view
            // when it gains focus – in this case, that's the decoration box. However, since text
            // fields may have their own internal scrolling, and the decoration box can do anything,
            // we also need to specifically request that the cursor itself be brought into view.
            // TODO(b/216790855) If this request happens after the focusable's request, the field
            //  will only be scrolled far enough to show the cursor, _not_ the entire decoration
            //  box.
            if (it.isFocused) {
                state.layoutResult?.let { layoutResult ->
                    coroutineScope.launch {
                        bringIntoViewRequester.bringSelectionEndIntoView(
                            value,
                            state.textDelegate,
                            layoutResult.value,
                            offsetMapping
                        )
                    }
                }
            }
        }
        if (!it.isFocused) manager.deselect()
    }

    // Hide the keyboard if made disabled or read-only while focused (b/237308379).
    if (textInputService != null) {
        val writeable by rememberUpdatedState(enabled && !readOnly)
        LaunchedEffect(Unit) {
            try {
                snapshotFlow { writeable }.collect { writeable ->
                    // When hasFocus changes, the session will be stopped/started in the focus
                    // handler so we don't need to handle its changes here.
                    if (writeable && state.hasFocus) {
                        startInputSession(
                            textInputService,
                            state,
                            manager.value,
                            imeOptions,
                            offsetMapping
                        )
                    } else {
                        endInputSession(state)
                    }
                }
            } finally {
                // TODO(b/230536793) This is a workaround since we don't get an explicit focus blur
                //  event when the text field is removed from the composition entirely.
                endInputSession(state)
            }
        }
    }

    val pointerModifier = Modifier.textFieldPointer(
        manager,
        enabled,
        interactionSource,
        state,
        focusRequester,
        readOnly,
        offsetMapping
    )

    fun paragraphHashCode(offsetMapping: OffsetMapping, value: TextFieldValue, layoutResult: TextLayoutResult): Int {
        var hashCode = layoutResult.hashCode()
        if (!value.selection.collapsed) {
            val start = offsetMapping.originalToTransformed(value.selection.min)
            val end = offsetMapping.originalToTransformed(value.selection.max)
            hashCode += 31 * hashCode + start
            hashCode += 31 * hashCode + end
        }
        return hashCode
    }

    val drawModifier = Modifier.drawBehind {
        state.layoutResult?.let { layoutResult ->
            drawIntoCanvas { canvas ->
                var currentParagraphHashCode = 0
                if (drawInSkia || EnableIOSParagraph) {
                    localCanvas = canvas
                } else {
                    currentParagraphHashCode = paragraphHashCode(offsetMapping, value, layoutResult.value)
                    val width = layoutResult.value.size.width
                    val height = layoutResult.value.size.height
                    if (platformTextDelegate?.needRedrawText(canvas, currentParagraphHashCode, width, height) == false) return@drawBehind
                    val newBitmap = ImageBitmap(width, height)
                    localCanvas = Canvas(newBitmap)
                    localBitmap = newBitmap
                }
                TextFieldDelegate.draw(
                    localCanvas!!,
                    value,
                    offsetMapping,
                    layoutResult.value,
                    state.selectionPaint
                )
                if (!drawInSkia && !EnableIOSParagraph) {
                    platformTextDelegate?.renderTextImage(
                        localBitmap,
                        layoutResult.value.size.width,
                        layoutResult.value.size.height,
                        currentParagraphHashCode,
                        canvas
                    )
                }

                localBitmap = null
                localCanvas = null
            }
        }
    }

    val onPositionedModifier = Modifier.onGloballyPositioned {
        state.layoutCoordinates = it
        state.layoutResult?.innerTextFieldCoordinates = it
        if (enabled) {
            if (state.handleState == HandleState.Selection) {
                if (state.showFloatingToolbar && isWindowFocusedBehindFlag(windowInfo)) {
                    manager.showSelectionToolbar()
                } else {
                    manager.hideSelectionToolbar()
                }
                state.showSelectionHandleStart =
                    manager.isSelectionHandleInVisibleBound(isStartHandle = true)
                state.showSelectionHandleEnd =
                    manager.isSelectionHandleInVisibleBound(isStartHandle = false)
                state.showCursorHandle = value.selection.collapsed
            } else if (state.handleState == HandleState.Cursor) {
                state.showCursorHandle =
                    manager.isSelectionHandleInVisibleBound(isStartHandle = true)
            }
            notifyFocusedRect(state, value, offsetMapping)
            state.layoutResult?.let { layoutResult ->
                state.inputSession?.let { inputSession ->
                    if (state.hasFocus) {
                        TextFieldDelegate.updateTextLayoutResult(
                            inputSession,
                            value,
                            offsetMapping,
                            layoutResult
                        )
                    }
                }
            }
        }
    }

    val isPassword = visualTransformation is PasswordVisualTransformation
    val semanticsModifier = Modifier.semantics(true) {
        // focused semantics are handled by Modifier.focusable()
        this.editableText = transformedText.text
        this.textSelectionRange = value.selection
        if (!enabled) this.disabled()
        if (isPassword) this.password()
        getTextLayoutResult {
            if (state.layoutResult != null) {
                it.add(state.layoutResult!!.value)
                true
            } else {
                false
            }
        }
        setText { text ->
            if (readOnly || !enabled) return@setText false

            // If the action is performed while in an active text editing session, treat this like
            // an IME command and update the text by going through the buffer. This keeps the buffer
            // state consistent if other IME commands are performed before the next recomposition,
            // and is used for the testing code path.
            state.inputSession?.let { session ->
                TextFieldDelegate.onEditCommand(
                    ops = listOf(DeleteAllCommand(), CommitTextCommand(text, 1)),
                    editProcessor = state.processor,
                    state.onValueChange,
                    session
                )
            } ?: run {
                state.onValueChange(TextFieldValue(text.text, TextRange(text.text.length)))
            }
            true
        }
        insertTextAtCursor { text ->
            if (readOnly || !enabled) return@insertTextAtCursor false

            // If the action is performed while in an active text editing session, treat this like
            // an IME command and update the text by going through the buffer. This keeps the buffer
            // state consistent if other IME commands are performed before the next recomposition,
            // and is used for the testing code path.
            state.inputSession?.let { session ->
                TextFieldDelegate.onEditCommand(
                    // Finish composing text first because when the field is focused the IME might
                    // set composition.
                    ops = listOf(FinishComposingTextCommand(), CommitTextCommand(text, 1)),
                    editProcessor = state.processor,
                    state.onValueChange,
                    session
                )
            } ?: run {
                val newText =
                    value.text.replaceRange(value.selection.start, value.selection.end, text)
                val newCursor = TextRange(value.selection.start + text.length)
                state.onValueChange(TextFieldValue(newText, newCursor))
            }
            true
        }
        setSelection { selectionStart, selectionEnd, relativeToOriginalText ->
            // in traversal mode we get selection from the `textSelectionRange` semantics which is
            // selection in original text. In non-traversal mode selection comes from the Talkback
            // and indices are relative to the transformed text
            val start = if (relativeToOriginalText) {
                selectionStart
            } else {
                offsetMapping.transformedToOriginal(selectionStart)
            }
            val end = if (relativeToOriginalText) {
                selectionEnd
            } else {
                offsetMapping.transformedToOriginal(selectionEnd)
            }

            if (!enabled) {
                false
            } else if (start == value.selection.start && end == value.selection.end) {
                false
            } else if (minOf(start, end) >= 0 &&
                maxOf(start, end) <= value.annotatedString.length
            ) {
                // Do not show toolbar if it's a traversal mode (with the volume keys), or
                // if the cursor just moved to beginning or end.
                if (relativeToOriginalText || start == end) {
                    manager.exitSelectionMode()
                } else {
                    manager.enterSelectionMode()
                }
                state.onValueChange(
                    TextFieldValue(
                        value.annotatedString,
                        TextRange(start, end)
                    )
                )
                true
            } else {
                manager.exitSelectionMode()
                false
            }
        }
        onImeAction(imeOptions.imeAction) {
            // This will perform the appropriate default action if no handler has been specified, so
            // as far as the platform is concerned, we always handle the action and never want to
            // defer to the default _platform_ implementation.
            state.onImeActionPerformed(imeOptions.imeAction)
            true
        }
        onClick {
            // according to the documentation, we still need to provide proper semantics actions
            // even if the state is 'disabled'
            requestFocusAndShowKeyboardIfNeeded(state, focusRequester, !readOnly)
            true
        }
        onLongClick {
            manager.enterSelectionMode()
            true
        }
        if (!value.selection.collapsed && !isPassword) {
            copyText {
                manager.copy()
                true
            }
            if (enabled && !readOnly) {
                cutText {
                    manager.cut()
                    true
                }
            }
        }
        if (enabled && !readOnly) {
            pasteText {
                manager.paste()
                true
            }
        }
    }

    val showCursor = enabled && !readOnly && isWindowFocusedBehindFlag(windowInfo)
    val cursorModifier = Modifier.cursor(state, value, offsetMapping, cursorBrush, showCursor)

    DisposableEffect(manager) {
        onDispose { manager.hideSelectionToolbar() }
    }

    DisposableEffect(imeOptions) {
        if (textInputService != null && state.hasFocus) {
            state.inputSession = TextFieldDelegate.restartInput(
                textInputService = textInputService,
                value = value,
                editProcessor = state.processor,
                imeOptions = imeOptions,
                onValueChange = state.onValueChange,
                onImeActionPerformed = state.onImeActionPerformed
            )
        }
        onDispose { /* do nothing */ }
    }

    val textKeyInputModifier =
        Modifier.textFieldKeyInput(
            state = state,
            manager = manager,
            value = value,
            onValueChange = state.onValueChange,
            editable = !readOnly,
            singleLine = maxLines == 1,
            offsetMapping = offsetMapping,
            undoManager = undoManager,
            imeAction = imeOptions.imeAction,
        )

    val overscrollEffect = rememberTextFieldOverscrollEffect()

    // Modifiers that should be applied to the outer text field container. Usually those include
    // gesture and semantics modifiers.
    val decorationBoxModifier = modifier
        .then(focusModifier)
        .interceptDPadAndMoveFocus(state, focusManager)
        .previewKeyEventToDeselectOnBack(state, manager)
        .then(textKeyInputModifier)
        .textFieldScrollable(scrollerPosition, interactionSource, enabled, overscrollEffect)
        .then(pointerModifier)
        .then(semanticsModifier)
        .onGloballyPositioned {
            state.layoutResult?.decorationBoxCoordinates = it
        }

    val showHandleAndMagnifier =
        enabled && state.hasFocus && state.isInTouchMode && isWindowFocusedBehindFlag(windowInfo)
    val magnifierModifier = if (showHandleAndMagnifier) {
        Modifier.textFieldMagnifier(manager)
    } else {
        Modifier
    }

    CoreTextFieldRootBox(decorationBoxModifier, manager) {
        decorationBox {
            fun Modifier.overscroll(): Modifier =
                overscrollEffect?.let {
                    this then it.effectModifier
                } ?: this

            // Modifiers applied directly to the internal input field implementation. In general,
            // these will most likely include draw, layout and IME related modifiers.
            val coreTextFieldModifier = Modifier
                // min height is set for maxLines == 1 in order to prevent text cuts for single line
                // TextFields
                .heightIn(min = state.minHeightForSingleLineField)
                .heightInLines(
                    textStyle = textStyle,
                    minLines = minLines,
                    maxLines = maxLines
                )
                .overscroll()
                .textFieldScroll(
                    scrollerPosition = scrollerPosition,
                    textFieldValue = value,
                    visualTransformation = visualTransformation,
                    textLayoutResultProvider = { state.layoutResult },
                )
                .then(cursorModifier)
                .then(drawModifier)
                .textFieldMinSize(textStyle)
                .then(onPositionedModifier)
                .then(magnifierModifier)
                .bringIntoViewRequester(bringIntoViewRequester)

            SimpleLayout(coreTextFieldModifier) {
                Layout(
                    content = { },
                    measurePolicy = object : MeasurePolicy {
                        override fun MeasureScope.measure(
                            measurables: List<Measurable>,
                            constraints: Constraints
                        ): MeasureResult {
                            val prevProxy =
                                Snapshot.withoutReadObservation { state.layoutResult }
                            val prevResult = prevProxy?.value
                            val (width, height, result) = TextFieldDelegate.layout(
                                state.textDelegate,
                                constraints,
                                layoutDirection,
                                prevResult
                            )
                            if (prevResult != result) {
                                state.layoutResult = TextLayoutResultProxy(
                                    value = result,
                                    decorationBoxCoordinates =
                                        prevProxy?.decorationBoxCoordinates,
                                )
                                onTextLayout(result)
                                notifyFocusedRect(state, value, offsetMapping)
                            }

                            // calculate the min height for single line text to prevent text cuts.
                            // for single line text maxLines puts in max height constraint based on
                            // constant characters therefore if the user enters a character that is
                            // longer (i.e. emoji or a tall script) the text is cut
                            state.minHeightForSingleLineField = with(density) {
                                when (maxLines) {
                                    1 -> result.getLineBottom(0).ceilToIntPx()
                                    else -> 0
                                }.toDp()
                            }

                            return layout(
                                width = width,
                                height = height,
                                alignmentLines = mapOf(
                                    FirstBaseline to result.firstBaseline.roundToInt(),
                                    LastBaseline to result.lastBaseline.roundToInt()
                                )
                            ) {}
                        }

                        override fun IntrinsicMeasureScope.maxIntrinsicWidth(
                            measurables: List<IntrinsicMeasurable>,
                            height: Int
                        ): Int {
                            state.textDelegate.layoutIntrinsics(layoutDirection)
                            return state.textDelegate.maxIntrinsicWidth
                        }
                    }
                )

                SelectionToolbarAndHandles(
                    manager = manager,
                    show = state.handleState != HandleState.None &&
                        state.layoutCoordinates != null &&
                        state.layoutCoordinates!!.isAttached &&
                        showHandleAndMagnifier
                )

                if (
                    state.handleState == HandleState.Cursor &&
                    !readOnly &&
                    showHandleAndMagnifier
                ) {
                    TextFieldCursorHandle(manager = manager)
                }
            }
        }
    }
}

@Composable
private fun CoreTextFieldRootBox(
    modifier: Modifier,
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    Box(modifier, propagateMinConstraints = true) {
        ContextMenuArea(manager, content)
    }
}

/**
 * The selection handle state of the TextField. It can be None, Selection or Cursor.
 * It determines whether the selection handle, cursor handle or only cursor is shown. And how
 * TextField handles gestures.
 */
internal enum class HandleState {
    /**
     * No selection is active in this TextField. This is the initial state of the TextField.
     * If the user long click on the text and start selection, the TextField will exit this state
     * and enters [HandleState.Selection] state. If the user tap on the text, the TextField
     * will exit this state and enters [HandleState.Cursor] state.
     */
    None,

    /**
     * Selection handle is displayed for this TextField. User can drag the selection handle to
     * change the selected text. If the user start editing the text, the TextField will exit this
     * state and enters [HandleState.None] state. If the user tap on the text, the TextField
     * will exit this state and enters [HandleState.Cursor] state.
     */
    Selection,

    /**
     * Cursor handle is displayed for this TextField. User can drag the cursor handle to change
     * the cursor position. If the user start editing the text, the TextField will exit this
     * state and enters [HandleState.None] state. If the user long click on the text and start
     * selection, the TextField will exit this state and enters [HandleState.Selection] state.
     * Also notice that TextField won't enter this state if the current input text is empty.
     */
    Cursor
}

/**
 * Indicates which handle is being dragged when the user is dragging on a text field handle.
 * @see TextFieldState.handleState
 */
internal enum class Handle {
    Cursor,
    SelectionStart,
    SelectionEnd
}

/**
 * Modifier to intercept back key presses, when supported by the platform, and deselect selected
 * text and clear selection popups.
 */
private fun Modifier.previewKeyEventToDeselectOnBack(
    state: TextFieldState,
    manager: TextFieldSelectionManager
) = onPreviewKeyEvent { keyEvent ->
    if (state.handleState == HandleState.Selection && keyEvent.cancelsTextSelection()) {
        manager.deselect()
        true
    } else {
        false
    }
}

@OptIn(InternalFoundationTextApi::class)
internal class TextFieldState(
    var textDelegate: TextDelegate,
    val recomposeScope: RecomposeScope,
    val keyboardController: SoftwareKeyboardController?,
) {
    val processor = EditProcessor()
    var inputSession: TextInputSession? = null

    /**
     * This should be a state as every time we update the value we need to redraw it.
     * state observation during onDraw callback will make it work.
     */
    var hasFocus by mutableStateOf(false)

    /**
     * Set to a non-zero value for single line TextFields in order to prevent text cuts.
     */
    var minHeightForSingleLineField by mutableStateOf(0.dp)

    /**
     * The last layout coordinates for the inner text field LayoutNode, used by selection and
     * notifyFocusedRect. Since this layoutCoordinates only used for relative position calculation,
     * we are guarding ourselves from using it when it's not attached.
     */
    private var _layoutCoordinates: LayoutCoordinates? = null
    var layoutCoordinates: LayoutCoordinates?
        get() = _layoutCoordinates?.takeIf { it.isAttached }
        set(value) {
            _layoutCoordinates = value
        }

    /**
     * You should be using proxy type [TextLayoutResultProxy] if you need to translate touch
     * offset into text's coordinate system. For example, if you add a gesture on top of the
     * decoration box and want to know the character in text for the given touch offset on
     * decoration box.
     * When you don't need to shift the touch offset, you should be using `layoutResult.value`
     * which omits the proxy and calls the layout result directly. This is needed when you work
     * with the text directly, and not the decoration box. For example, cursor modifier gets
     * position using the [TextFieldValue.selection] value which corresponds to the text directly,
     * and therefore does not require the translation.
     */
    private val layoutResultState: MutableState<TextLayoutResultProxy?> = mutableStateOf(null)
    var layoutResult: TextLayoutResultProxy?
        get() = layoutResultState.value
        set(value) {
            layoutResultState.value = value
            isLayoutResultStale = false
        }

    /**
     * [textDelegate] keeps a reference to the visually transformed text that is visible to the
     * user. TextFieldState needs to have access to the underlying value that is not transformed
     * while making comparisons that test whether the user input actually changed.
     *
     * This field contains the real value that is passed by the user before it was visually
     * transformed.
     */
    var untransformedText: AnnotatedString? = null

    /**
     * The gesture detector state, to indicate whether current state is selection, cursor
     * or editing.
     *
     * In the none state, no selection or cursor handle is shown, only the cursor is shown.
     * TextField is initially in this state. To enter this state, input anything from the
     * keyboard and modify the text.
     *
     * In the selection state, there is no cursor shown, only selection is shown. To enter
     * the selection mode, just long press on the screen. In this mode, finger movement on the
     * screen changes selection instead of moving the cursor.
     *
     * In the cursor state, no selection is shown, and the cursor and the cursor handle are shown.
     * To enter the cursor state, tap anywhere within the TextField.(The TextField will stay in the
     * edit state if the current text is empty.) In this mode, finger movement on the screen
     * moves the cursor.
     */
    var handleState by mutableStateOf(HandleState.None)

    /**
     * A flag to check if the floating toolbar should show.
     *
     * This state is meant to represent the floating toolbar status regardless of if all touch
     * behaviors are disabled (like if the user is using a mouse). This is so that when touch
     * behaviors are re-enabled, the toolbar status will still reflect whether it should be shown
     * at that point.
     */
    var showFloatingToolbar by mutableStateOf(false)

    /**
     * True if the position of the selection start handle is within a visible part of the window
     * (i.e. not scrolled out of view) and the handle should be drawn.
     */
    var showSelectionHandleStart by mutableStateOf(false)

    /**
     * True if the position of the selection end handle is within a visible part of the window
     * (i.e. not scrolled out of view) and the handle should be drawn.
     */
    var showSelectionHandleEnd by mutableStateOf(false)

    /**
     * True if the position of the cursor is within a visible part of the window (i.e. not scrolled
     * out of view) and the handle should be drawn.
     */
    var showCursorHandle by mutableStateOf(false)

    /**
     * TextFieldState holds both TextDelegate and layout result. However, these two values are not
     * updated at the same time. TextDelegate is updated during composition according to new
     * arguments while layoutResult is updated during layout phase. Therefore, [layoutResult] might
     * not indicate the result of [textDelegate] at a given time during composition. This variable
     * indicates whether layout result is lacking behind the latest TextDelegate.
     */
    var isLayoutResultStale: Boolean = true
        private set

    var isInTouchMode: Boolean by mutableStateOf(true)

    private val keyboardActionRunner: KeyboardActionRunner =
        KeyboardActionRunner(keyboardController)

    /**
     * DO NOT USE, use [onValueChange] instead. This is original callback provided to the TextField.
     * In order the CoreTextField to work, the recompose.invalidate() has to be called when we call
     * the callback and [onValueChange] is a wrapper that mainly does that.
     */
    private var onValueChangeOriginal: (TextFieldValue) -> Unit = {}

    val onValueChange: (TextFieldValue) -> Unit = {
        if (it.text != untransformedText?.text) {
            // Text has been changed, enter the HandleState.None and hide the cursor handle.
            handleState = HandleState.None
        }
        onValueChangeOriginal(it)
        recomposeScope.invalidate()
    }

    val onImeActionPerformed: (ImeAction) -> Unit = { imeAction ->
        keyboardActionRunner.runAction(imeAction)
    }

    /** The paint used to draw highlight background for selected text. */
    val selectionPaint: Paint = Paint()

    fun update(
        untransformedText: AnnotatedString,
        visualText: AnnotatedString,
        textStyle: TextStyle,
        softWrap: Boolean,
        density: Density,
        fontFamilyResolver: FontFamily.Resolver,
        onValueChange: (TextFieldValue) -> Unit,
        keyboardActions: KeyboardActions,
        focusManager: FocusManager,
        selectionBackgroundColor: Color
    ) {
        this.onValueChangeOriginal = onValueChange
        this.selectionPaint.color = selectionBackgroundColor
        this.keyboardActionRunner.apply {
            this.keyboardActions = keyboardActions
            this.focusManager = focusManager
        }
        this.untransformedText = untransformedText

        val newTextDelegate = updateTextDelegate(
            current = textDelegate,
            text = visualText,
            style = textStyle,
            softWrap = softWrap,
            density = density,
            fontFamilyResolver = fontFamilyResolver,
            placeholders = emptyList(),
        )

        if (textDelegate !== newTextDelegate) isLayoutResultStale = true
        textDelegate = newTextDelegate
    }
}

/**
 * Request focus on tap. If already focused, makes sure the keyboard is requested.
 */
internal fun requestFocusAndShowKeyboardIfNeeded(
    state: TextFieldState,
    focusRequester: FocusRequester,
    allowKeyboard: Boolean
) {
    if (!state.hasFocus) {
        focusRequester.requestFocus()
    } else if (allowKeyboard) {
        state.keyboardController?.show()
    }
}

private fun startInputSession(
    textInputService: TextInputService,
    state: TextFieldState,
    value: TextFieldValue,
    imeOptions: ImeOptions,
    offsetMapping: OffsetMapping
) {
    state.inputSession = TextFieldDelegate.onFocus(
        textInputService,
        value,
        state.processor,
        imeOptions,
        state.onValueChange,
        state.onImeActionPerformed
    )
    notifyFocusedRect(state, value, offsetMapping)
}

private fun endInputSession(state: TextFieldState) {
    state.inputSession?.let { session ->
        TextFieldDelegate.onBlur(session, state.processor, state.onValueChange)
    }
    state.inputSession = null
}

/**
 * Calculates the location of the end of the current selection and requests that it be brought into
 * view using [bringIntoView][BringIntoViewRequester.bringIntoView].
 *
 * Text fields have a lot of different edge cases where they need to make sure they stay visible:
 *
 * 1. Focusable node newly receives focus – always bring entire node into view.
 * 2. Unfocused text field is tapped – always bring cursor area into view (conflicts with above, see
 *    b/216790855).
 * 3. Focused text field is tapped – always bring cursor area into view.
 * 4. Text input occurs – always bring cursor area into view.
 * 5. Scrollable parent resizes and the currently-focused item is now hidden – bring entire node
 *    into view if it was also in view before the resize. This handles the case of
 *    `softInputMode=ADJUST_RESIZE`. See b/216842427.
 * 6. Entire window is panned due to `softInputMode=ADJUST_PAN` – report the correct focused rect to
 *    the view system, and the view system itself will keep the focused area in view.
 *    See aosp/1964580.
 *
 * This function is used to handle 2, 3, and 4, and the others are automatically handled by the
 * focus system.
 */
@OptIn(ExperimentalFoundationApi::class, InternalFoundationTextApi::class)
internal suspend fun BringIntoViewRequester.bringSelectionEndIntoView(
    value: TextFieldValue,
    textDelegate: TextDelegate,
    textLayoutResult: TextLayoutResult,
    offsetMapping: OffsetMapping
) {
    val selectionEndInTransformed = offsetMapping.originalToTransformed(value.selection.max)
    val selectionEndBounds = when {
        selectionEndInTransformed < textLayoutResult.layoutInput.text.length -> {
            textLayoutResult.getBoundingBox(selectionEndInTransformed)
        }

        selectionEndInTransformed != 0 -> {
            textLayoutResult.getBoundingBox(selectionEndInTransformed - 1)
        }

        else -> { // empty text.
            val defaultSize = computeSizeForDefaultText(
                textDelegate.style,
                textDelegate.density,
                textDelegate.fontFamilyResolver
            )
            Rect(0f, 0f, 1.0f, defaultSize.height.toFloat())
        }
    }
    bringIntoView(selectionEndBounds)
}

@Composable
private fun SelectionToolbarAndHandles(manager: TextFieldSelectionManager, show: Boolean) {
    with(manager) {
        if (show) {
            // Check whether text layout result became stale. A stale text layout might be
            // completely unrelated to current TextFieldValue, causing offset errors.
            state?.layoutResult?.value?.takeIf { !(state?.isLayoutResultStale ?: true) }?.let {
                if (!value.selection.collapsed) {
                    val startOffset = offsetMapping.originalToTransformed(value.selection.start)
                    val endOffset = offsetMapping.originalToTransformed(value.selection.end)
                    val startDirection = it.getBidiRunDirection(startOffset)
                    val endDirection = it.getBidiRunDirection(max(endOffset - 1, 0))
                    if (manager.state?.showSelectionHandleStart == true) {
                        TextFieldSelectionHandle(
                            isStartHandle = true,
                            direction = startDirection,
                            manager = manager
                        )
                    }
                    if (manager.state?.showSelectionHandleEnd == true) {
                        TextFieldSelectionHandle(
                            isStartHandle = false,
                            direction = endDirection,
                            manager = manager
                        )
                    }
                }

                state?.let { textFieldState ->
                    // If in selection mode (when the floating toolbar is shown) a new symbol
                    // from the keyboard is entered, text field should enter the editing mode
                    // instead.
                    if (isTextChanged()) textFieldState.showFloatingToolbar = false
                    if (textFieldState.hasFocus) {
                        if (textFieldState.showFloatingToolbar) showSelectionToolbar()
                        else hideSelectionToolbar()
                    }
                }
            }
        } else hideSelectionToolbar()
    }
}

@Composable
internal fun TextFieldCursorHandle(manager: TextFieldSelectionManager) {
    if (manager.state?.showCursorHandle == true && manager.transformedText?.isNotEmpty() == true) {
        val observer = remember(manager) { manager.cursorDragObserver() }
        val position = manager.getCursorPosition(LocalDensity.current)
        CursorHandle(
            handlePosition = position,
            modifier = Modifier
                .pointerInput(observer) {
                    coroutineScope {
                        // UNDISPATCHED because this runs upon first pointer event and
                        // without it the event would pass before the handler is ready
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            detectDownAndDragGesturesWithObserver(observer)
                        }
                        launch(start = CoroutineStart.UNDISPATCHED) {
                            detectTapGestures { manager.showSelectionToolbar() }
                        }
                    }
                }
                .semantics {
                    this[SelectionHandleInfoKey] = SelectionHandleInfo(
                        handle = Handle.Cursor,
                        position = position,
                        anchor = SelectionHandleAnchor.Middle,
                        visible = true,
                    )
                },
            content = null
        )
    }
}

@Composable
internal expect fun CursorHandle(
    handlePosition: Offset,
    modifier: Modifier,
    content: @Composable (() -> Unit)?
)

// TODO(b/262648050) Try to find a better API.
@OptIn(InternalFoundationTextApi::class)
private fun notifyFocusedRect(
    state: TextFieldState,
    value: TextFieldValue,
    offsetMapping: OffsetMapping
) {
    // If this reports state reads it causes an invalidation cycle.
    // This function doesn't need to be invalidated anyway because it's already explicitly called
    // after updating text layout or position.
    Snapshot.withoutReadObservation {
        val layoutResult = state.layoutResult ?: return
        val inputSession = state.inputSession ?: return
        val layoutCoordinates = state.layoutCoordinates ?: return
        TextFieldDelegate.notifyFocusedRect(
            value,
            state.textDelegate,
            layoutResult.value,
            layoutCoordinates,
            inputSession,
            state.hasFocus,
            offsetMapping
        )
    }
}

// (b/308895081) Temporary disable use of Window Focus for cursor blinking state
internal const val USE_WINDOW_FOCUS_ENABLED = false
internal fun isWindowFocusedBehindFlag(windowInfo: WindowInfo) =
    if (USE_WINDOW_FOCUS_ENABLED) windowInfo.isWindowFocused else true
