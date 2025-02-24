/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.FinishComposingTextCommand
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.SetComposingRegionCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.SetSelectionCommand
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toSize
import androidx.compose.ui.unit.width
import androidx.compose.ui.window.FocusStack
import androidx.compose.ui.window.IntermediateTextInputUIView
import kotlin.math.absoluteValue
import kotlin.math.min
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import platform.UIKit.NSStringFromCGPoint
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIColor
import platform.UIKit.UIPress
import platform.UIKit.UIScrollView
import platform.UIKit.UIView
import platform.UIKit.reloadInputViews
import platform.darwin.dispatch_async

internal class UIKitTextInputService(
    private val updateView: () -> Unit,
    private val rootView: UIView,
    private val viewConfiguration: ViewConfiguration,
    private val focusStack: FocusStack?,
    private val onInputStarted: () -> Unit,
    /**
     * Callback to handle keyboard presses. The parameter is a [Set] of [UIPress] objects.
     * Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    private val onKeyboardPresses: (Set<*>) -> Unit,
    private val focusManager: () -> ComposeSceneFocusManager
) : PlatformTextInputService, TextToolbar {

    private var currentInput: CurrentInput? = null
    private var currentImeOptions: ImeOptions? = null
    private var currentImeActionHandler: ((ImeAction) -> Unit)? = null
    private var textUIView: IntermediateTextInputUIView? = null
    private var textLayoutResult: TextLayoutResult? = null
        set(value) {
            textUIView?.selectionWillChange()
            field = value
            textUIView?.selectionDidChange()
        }

    /**
     * Workaround to prevent calling textWillChange, textDidChange, selectionWillChange, and
     * selectionDidChange when the value of the current input is changed by the system (i.e., by the user
     * input) not by the state change of the Compose side. These 4 functions call methods of
     * UITextInputDelegateProtocol, which notifies the system that the text or the selection of the
     * current input has changed.
     *
     * This is to properly handle multi-stage input methods that depend on text selection, required by
     * languages such as Korean (Chinese and Japanese input methods depend on text marking). The writing
     * system of these languages contains letters that can be broken into multiple parts, and each keyboard
     * key corresponds to those parts. Therefore, the input system holds an internal state to combine these
     * parts correctly. However, the methods of UITextInputDelegateProtocol reset this state, resulting in
     * incorrect input. (e.g., 컴포즈 becomes ㅋㅓㅁㅍㅗㅈㅡ when not handled properly)
     *
     * @see _tempCurrentInputSession holds the same text and selection of the current input. It is used
     * instead of the old value passed to updateState. When the current value change is due to the
     * user input, updateState is not effective because _tempCurrentInputSession holds the same value.
     * However, when the current value change is due to the change of the user selection or to the
     * state change in the Compose side, updateState calls the 4 methods because the new value holds
     * these changes.
     */
    private var _tempCurrentInputSession: EditProcessor? = null

    /**
     * Workaround to prevent IME action from being called multiple times with hardware keyboards.
     * When the hardware return key is held down, iOS sends multiple newline characters to the application,
     * which makes UIKitTextInputService call the current IME action multiple times without an additional
     * debouncing logic.
     *
     * @see _tempHardwareReturnKeyPressed is set to true when the return key is pressed with a
     * hardware keyboard.
     * @see _tempImeActionIsCalledWithHardwareReturnKey is set to true when the
     * current IME action has been called within the current hardware return key press.
     */
    private var _tempHardwareReturnKeyPressed: Boolean = false
    private var _tempImeActionIsCalledWithHardwareReturnKey: Boolean = false

    private val mainScope = MainScope()

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(value, onEditCommand)
        _tempCurrentInputSession = EditProcessor().apply {
            reset(value, null)
        }
        currentImeOptions = imeOptions
        currentImeActionHandler = onImeActionPerformed

        attachIntermediateTextInputView()
        showSoftwareKeyboard()
    }

    fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        editProcessor: EditProcessor?,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        currentInput = CurrentInput(value, onEditCommand)
        _tempCurrentInputSession = editProcessor
        currentImeOptions = imeOptions
        currentImeActionHandler = onImeActionPerformed

        attachIntermediateTextInputView()
        showSoftwareKeyboard()
    }

    override fun stopInput() {
        flushEditCommandsIfNeeded(force = true)

        detachIntermediateTextInputView()

        currentInput = null
        _tempCurrentInputSession = null
        currentImeOptions = null
        currentImeActionHandler = null
        textLayoutResult = null
        hideSoftwareKeyboard()
    }

    override fun showSoftwareKeyboard() {
        textUIView?.let {
            focusStack?.pushAndFocus(it)
        }
    }

    override fun hideSoftwareKeyboard() {
        textUIView?.let {
            focusStack?.popUntilNext(it)
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        val internalOldValue = _tempCurrentInputSession?.toTextFieldValue()
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged =
            textChanged || internalOldValue == null || internalOldValue.selection != newValue.selection
        if (textChanged) {
            textUIView?.textWillChange()
        }
        if (selectionChanged) {
            textUIView?.selectionWillChange()
        }
        _tempCurrentInputSession?.reset(newValue, null)
        currentInput?.let { input ->
            input.value = newValue
        }
        if (textChanged) {
            textUIView?.textDidChange()
        }
        if (selectionChanged) {
            textUIView?.selectionDidChange()
        }
        if (textChanged || selectionChanged) {
            updateView()
            textUIView?.reloadInputViews()
        }
        attachIfNeeded()
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (event.key) {
            Key.Enter -> handleEnterKey(event)
            Key.Backspace -> handleBackspace(event)
            Key.Escape -> handleEscape(event)
            else -> false
        }
    }

    override fun updateTextLayoutResult(
        textFieldValue: TextFieldValue,
        offsetMapping: OffsetMapping,
        textLayoutResult: TextLayoutResult,
        textFieldToRootTransform: (Matrix) -> Unit,
        innerTextFieldBounds: Rect,
        decorationBoxBounds: Rect
    ) {
        super.updateTextLayoutResult(
            textFieldValue,
            offsetMapping,
            textLayoutResult,
            textFieldToRootTransform,
            innerTextFieldBounds,
            decorationBoxBounds
        )

        this.textLayoutResult = textLayoutResult

        val matrix = Matrix()
        textFieldToRootTransform(matrix)
        val textFieldFrame = matrix.map(decorationBoxBounds)
        val contentFrame = matrix.map(
            Rect(
                offset = Offset.Zero,
                size = textLayoutResult.size.toSize()
            )
        )
        val frame = textFieldFrame.toDpRect(rootView.density)
        val bounds = Rect(
            offset = textFieldFrame.topLeft - contentFrame.topLeft,
            size = contentFrame.size
        ).toDpRect(rootView.density)

        notifyGeometryChange(frame, bounds)
    }

    fun updateTextLayoutResult(textLayoutResult: TextLayoutResult) {
        this.textLayoutResult = textLayoutResult
    }

    fun notifyGeometryChange(frame: DpRect, bounds: DpRect) {
        textUIView?.notifyGeometryChanged(frame.asCGRect(), bounds.asCGRect())
    }

    override fun notifyFocusedRect(rect: Rect) {
        currentInput?.let { input ->
            input.focusedRect = rect
        }
    }

    private fun handleEnterKey(event: KeyEvent): Boolean {
        _tempImeActionIsCalledWithHardwareReturnKey = false
        return when (event.type) {
            KeyEventType.KeyUp -> {
                _tempHardwareReturnKeyPressed = false
                false
            }

            KeyEventType.KeyDown -> {
                _tempHardwareReturnKeyPressed = true
                // This prevents two new line characters from being added for one hardware return key press.
                true
            }

            else -> false
        }
    }

    private fun handleBackspace(event: KeyEvent): Boolean {
        // This prevents two characters from being removed for one hardware backspace key press.
        return event.type == KeyEventType.KeyDown
    }

    private fun handleEscape(event: KeyEvent): Boolean {
        return if (currentInput != null && event.type == KeyEventType.KeyUp) {
            focusManager().releaseFocus()
            true
        } else {
            false
        }
    }

    private val editCommandsBatch = mutableListOf<EditCommand>()
    private var editBatchDepth: Int = 0
        set(value) {
            field = value
            flushEditCommandsIfNeeded()
        }

    private fun sendEditCommand(vararg commands: EditCommand) {
        _tempCurrentInputSession?.apply(commands.toList())

        editCommandsBatch.addAll(commands)
        flushEditCommandsIfNeeded()
    }

    fun flushEditCommandsIfNeeded(force: Boolean = false) {
        if ((force || editBatchDepth == 0) && editCommandsBatch.isNotEmpty()) {
            val commandList = editCommandsBatch.toList()
            editCommandsBatch.clear()

            currentInput?.onEditCommand?.invoke(commandList)
        }
    }

    private fun imeActionRequired(): Boolean =
        currentImeOptions?.run {
            singleLine || (
                imeAction != ImeAction.None
                    && imeAction != ImeAction.Default
                    && !(imeAction == ImeAction.Search && _tempHardwareReturnKeyPressed)
                )
        } ?: false

    private fun runImeActionIfRequired(): Boolean {
        val imeAction = currentImeOptions?.imeAction ?: return false
        val imeActionHandler = currentImeActionHandler ?: return false
        if (!imeActionRequired()) {
            return false
        }
        if (!_tempImeActionIsCalledWithHardwareReturnKey) {
            if (imeAction == ImeAction.Default) {
                imeActionHandler(ImeAction.Done)
            } else {
                imeActionHandler(imeAction)
            }
        }
        if (_tempHardwareReturnKeyPressed) {
            _tempImeActionIsCalledWithHardwareReturnKey = true
        }
        return true
    }

    private fun getState(): TextFieldValue? = currentInput?.value
    private fun getFocusedRect(): Rect? = currentInput?.focusedRect

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        if (textUIView == null) {
            // If showMenu() is called and textUIView is not created,
            // then it means that showMenu() called in SelectionContainer without any textfields,
            // and IntermediateTextInputView must be created to show an editing menu
            attachIntermediateTextInputView()
            updateView()
        }
        textUIView?.showTextMenu(
            targetRect = rect.toDpRect(rootView.density).asCGRect(),
            textActions = object : TextActions {
                override val copy: (() -> Unit)? = onCopyRequested
                override val cut: (() -> Unit)? = onCutRequested
                override val paste: (() -> Unit)? = onPasteRequested
                override val selectAll: (() -> Unit)? = onSelectAllRequested
            }
        )
    }

    /**
     * TODO on UIKit native behaviour is hide text menu, when touch outside
     */
    override fun hide() {
        textUIView?.hideTextMenu()
        if ((textUIView != null) && (currentInput == null)) { // means that editing context menu shown in selection container
            textUIView?.resignFirstResponder()
            detachIntermediateTextInputView()
        }
    }

    override val status: TextToolbarStatus
        get() = if (textUIView?.isFirstResponder() == true)
            TextToolbarStatus.Shown
        else
            TextToolbarStatus.Hidden

    private fun attachIfNeeded() {
        if  (textUIView == null) {
            attachIntermediateTextInputView()

            showSoftwareKeyboard()
            onInputStarted()

            textUIView?.setNeedsLayout()
            textUIView?.setNeedsDisplay()

            mainScope.launch {
                textUIView?.setNeedsLayout()
                textUIView?.setNeedsDisplay()
                textUIView?.layoutIfNeeded()
            }
        }
    }

    private fun attachIntermediateTextInputView() {
        detachIntermediateTextInputView()

        textUIView = IntermediateTextInputUIView(
            viewConfiguration = viewConfiguration
        ).also {
            rootView.addSubview(it)
            rootView.setFrame(it.bounds)

            it.setBackgroundColor(UIColor.redColor.colorWithAlphaComponent(0.3))
            it.setTintColor(UIColor.yellowColor) // forward colors here
            it.onKeyboardPresses = onKeyboardPresses
            it.clipsToBounds = true
            it.input = createSkikoInput()
            it.inputTraits = getUITextInputTraits(currentImeOptions)


            // Resizing should be done later
            // TODO: Check selection container
            it.resignFirstResponder()
            val success = it.becomeFirstResponder()
        }
    }

    private fun detachIntermediateTextInputView() {
        textUIView?.let { view ->
            view.input = null
            view.inputTraits = EmptyInputTraits
            view.resetOnKeyboardPressesCallback()
            mainScope.launch {
                view.removeFromSuperview()
            }
        }
        textUIView = null
    }

    private fun createSkikoInput() = object : IOSSkikoInput {

        private var floatingCursorTranslation: Offset? = null

        override fun beginFloatingCursor(offset: DpOffset) {
            val cursorPos = getState()?.selection?.start ?: return
            val cursorRect = textLayoutResult?.getCursorRect(cursorPos) ?: return
            floatingCursorTranslation = cursorRect.center - offset.toOffset(rootView.density)
        }

        override fun updateFloatingCursor(offset: DpOffset) {
            val translation = floatingCursorTranslation ?: return
            val offsetPx = offset.toOffset(rootView.density)
            val pos = textLayoutResult
                ?.getOffsetForPosition(offsetPx + translation) ?: return

            sendEditCommand(SetSelectionCommand(pos, pos))
        }

        override fun endFloatingCursor() {
            floatingCursorTranslation = null
        }

        override fun beginEditBatch() {
            editBatchDepth++
        }

        override fun endEditBatch() {
            editBatchDepth--
        }

        /**
         * A Boolean value that indicates whether the text-entry object has any text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
         */
        override fun hasText(): Boolean = getState()?.text?.isNotEmpty() ?: false

        /**
         * Inserts a character into the displayed text.
         * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
         * @param text A string object representing the character typed on the system keyboard.
         */
        override fun insertText(text: String) {
            if (text == "\n") {
                if (runImeActionIfRequired()) {
                    return
                }
            }
            sendEditCommand(CommitTextCommand(text, 1))
        }

        /**
         * Deletes a character from the displayed text.
         * Remove the character just before the cursor from your class’s backing store and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
         */
        override fun deleteBackward() {
            // Before this function calls, iOS changes selection in setSelectedTextRange.
            // All needed characters should be already selected, and we can just remove them.
            sendEditCommand(
                CommitTextCommand("", 0)
            )
        }

        /**
         * The text position for the end of a document.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
         */
        override fun endOfDocument(): Int = getState()?.text?.length ?: 0

        /**
         * The range of selected text in a document.
         * If the text range has a length, it indicates the currently selected text.
         * If it has zero length, it indicates the caret (insertion point).
         * If the text-range object is nil, it indicates that there is no current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
         */
        override fun getSelectedTextRange(): TextRange? {
            return getState()?.selection
        }

        override fun setSelectedTextRange(range: TextRange?) {
            if (range != null) {
                sendEditCommand(
                    SetSelectionCommand(range.start, range.end)
                )
            } else {
                sendEditCommand(
                    SetSelectionCommand(endOfDocument(), endOfDocument())
                )
            }
        }

        override fun selectAll() {
            sendEditCommand(
                SetSelectionCommand(0, endOfDocument())
            )
        }

        /**
         * Returns the text in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
         * @param range A range of text in a document.
         * @return A substring of a document that falls within the specified range.
         */
        override fun textInRange(range: TextRange): String {
            if (isIncorrect(range)) {
                return ""
            }
            val text = getState()?.text ?: return ""
            return text.substring(range.start, range.end)
        }

        /**
         * Replaces the text in a document that is in the specified range.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
         * @param range A range of text in a document.
         * @param text A string to replace the text in range.
         */
        override fun replaceRange(range: TextRange, text: String) {
            sendEditCommand(
                SetComposingRegionCommand(range.start, range.end),
                SetComposingTextCommand(text, 1),
                FinishComposingTextCommand(),
            )
        }

        /**
         * Inserts the provided text and marks it to indicate that it is part of an active input session.
         * Setting marked text either replaces the existing marked text or,
         * if none is present, inserts it in place of the current selection.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614465-setmarkedtext
         * @param markedText The text to be marked.
         * @param selectedRange A range within markedText that indicates the current selection.
         * This range is always relative to markedText.
         */
        override fun setMarkedText(markedText: String?, selectedRange: TextRange) {
            if (markedText != null) {
                sendEditCommand(
                    SetComposingTextCommand(markedText, 1)
                )
            }
        }

        /**
         * The range of currently marked text in a document.
         * If there is no marked text, the value of the property is nil.
         * Marked text is provisionally inserted text that requires user confirmation;
         * it occurs in multistage text input.
         * The current selection, which can be a caret or an extended range, always occurs within the marked text.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
         */
        override fun markedTextRange(): TextRange? {
            return getState()?.composition
        }

        /**
         * Unmarks the currently marked text.
         * After this method is called, the value of markedTextRange is nil.
         * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
         */
        override fun unmarkText() {
            sendEditCommand(FinishComposingTextCommand())
        }

        /**
         * Returns the text position at a specified offset from another text position.
         * Returned value must be in range between 0 and length of text (inclusive).
         */
        override fun positionFromPosition(position: Int, offset: Int): Int {
            val text = getState()?.text ?: return 0

            if (position + offset >= text.length) {
                return text.length
            }
            if (position + offset <= 0) {
                return 0
            }
            var resultPosition = position
            val iterator = BreakIterator.makeCharacterInstance()
            iterator.setText(text)

            repeat(offset.absoluteValue) {
                val iteratorResult = if (offset > 0) {
                    iterator.following(resultPosition)
                } else {
                    iterator.preceding(resultPosition)
                }

                if (iteratorResult == BreakIterator.DONE) {
                    return resultPosition
                } else {
                    resultPosition = iteratorResult
                }
            }

            return resultPosition
        }

        /**
         * Returns the text position at a specified offset from another text position.
         * Returned value must be in range between 0 and length of text (inclusive).
         */
        override fun verticalPositionFromPosition(position: Int, verticalOffset: Int): Int {
            val text = getState()?.text ?: return 0
            val layoutResult = textLayoutResult ?: return 0

            val line = layoutResult.getLineForOffset(position)
            val lineStartOffset = layoutResult.getLineStart(line)
            val offsetInLine = position - lineStartOffset
            val targetLine = line + verticalOffset
            return when {
                targetLine < 0 -> 0
                targetLine >= layoutResult.lineCount -> text.length
                else -> {
                    val targetLineEnd = layoutResult.getLineEnd(targetLine)
                    val lineStart = layoutResult.getLineStart(targetLine)
                    positionFromPosition(
                        lineStart, min(offsetInLine, targetLineEnd - lineStart)
                    )
                }
            }
        }

        override fun currentFocusedDpRect(): DpRect? = getFocusedRect()?.toDpRect(rootView.density)

        override fun caretDpRectForPosition(position: Int): DpRect? {
            val text = getState()?.text ?: return null
            if (position < 0 || position > text.length) {
                return null
            }
            val currentTextLayoutResult = textLayoutResult ?: return null
            if (position > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) {
                return null
            }
            val rect = currentTextLayoutResult.getCursorRect(position)
            return rect.toDpRect(rootView.density)
        }

        override fun selectionRectsForRange(range: TextRange): List<TextSelectionRect> {
            val emptyList = emptyList<TextSelectionRect>()
            if (isIncorrect(range)) { return emptyList }
            val currentTextLayoutResult = textLayoutResult ?: return emptyList
            if (range.end > currentTextLayoutResult.multiParagraph.intrinsics.annotatedString.length) {
                return emptyList()
            }

            val startHandleRect = currentTextLayoutResult.getCursorRect(range.start)
            val endHandleRect = currentTextLayoutResult.getCursorRect(range.end)

            val oneLineSelection = startHandleRect.bottom == endHandleRect.bottom

            if (oneLineSelection) {
                val resultRect = TextSelectionRect(
                    dpRect = Rect(
                        startHandleRect.left,
                        startHandleRect.top,
                        endHandleRect.right,
                        endHandleRect.bottom
                    )
                        .toDpRect(rootView.density),
                    writingDirection = TextDirection.Content,
                    containsStart = true,
                    containsEnd = true,
                    isVertical = false
                )
                return listOf(resultRect)
            } else {
                val startLineNumber = currentTextLayoutResult.getLineForOffset(range.start)
                val startLineRight = currentTextLayoutResult.getLineRight(startLineNumber)
                val firstLineRect = TextSelectionRect(
                    dpRect = Rect(
                        startHandleRect.left,
                        startHandleRect.top,
                        startLineRight,
                        startHandleRect.bottom
                    )
                        .toDpRect(rootView.density),
                    writingDirection = TextDirection.Content,
                    containsStart = true,
                    containsEnd = false,
                    isVertical = false
                )

                val endLineNumber = currentTextLayoutResult.getLineForOffset(range.end)
                val endLineLeft = currentTextLayoutResult.getLineLeft(endLineNumber)
                val endLineRect = TextSelectionRect(
                    dpRect = Rect(endLineLeft, endHandleRect.top, endHandleRect.right, endHandleRect.bottom)
                        .toDpRect(rootView.density),
                    writingDirection = TextDirection.Content,
                    containsStart = false,
                    containsEnd = true,
                    isVertical = false
                )

                val lineStart = currentTextLayoutResult.getLineLeft(startLineNumber)
                val lineEnd = currentTextLayoutResult.getLineRight(endLineNumber)
                val middleRect = TextSelectionRect(
                    dpRect = Rect(lineStart, startHandleRect.bottom, lineEnd, endHandleRect.top)
                        .toDpRect(rootView.density),
                    writingDirection = TextDirection.Content,
                    containsStart = false,
                    containsEnd = false,
                    isVertical = false
                )

                return listOf(
                    firstLineRect,
                    middleRect,
                    endLineRect
                )
            }
        }

        override fun closestPositionToPoint(point: DpOffset): Int? {
            return textLayoutResult?.getOffsetForPosition(point.toOffset(rootView.density))
        }

        override fun closestPositionToPoint(point: DpOffset, withinRange: TextRange): Int? {
            val pointOffset =
                textLayoutResult?.getOffsetForPosition(point.toOffset(rootView.density))
                    ?: return null
            return pointOffset.coerceIn(withinRange.start, withinRange.end)
        }

        override fun characterRangeAtPoint(point: DpOffset): TextRange? {
            val pointOffset =
                textLayoutResult?.getOffsetForPosition(point.toOffset(rootView.density))
                    ?: return null
            return textLayoutResult?.getWordBoundary(pointOffset)
        }

        override fun positionWithinRange(range: TextRange, atCharacterOffset: Int): Int? {
            TODO("Not yet implemented")
        }

        override fun positionWithinRange(range: TextRange, farthestIndirection: String): Int? {
            TODO("Not yet implemented")
        }

        override fun characterRangeByExtendingPosition(
            position: Int,
            direction: String
        ): TextRange? {
            TODO("Not yet implemented")
        }

        override fun baseWritingDirectionForPosition(position: Int, inDirection: String): String? {
            TODO("Not yet implemented")
        }

        override fun offset(fromPosition: Int, toPosition: Int): Int {
            TODO("Not yet implemented")
        }

        private fun isIncorrect(range: TextRange): Boolean = range.start < 0 || range.end > endOfDocument() || range.start > range.end
    }
}

private data class CurrentInput(
    var value: TextFieldValue,
    val onEditCommand: (List<EditCommand>) -> Unit,
    var focusedRect: Rect? = null
)

internal data class TextSelectionRect(
    val dpRect: DpRect,
    val writingDirection: TextDirection,
    val containsStart: Boolean,
    val containsEnd: Boolean,
    val isVertical: Boolean
)