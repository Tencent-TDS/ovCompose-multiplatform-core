/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.toDpRect
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSDictionary
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.Foundation.dictionary
import platform.UIKit.NSWritingDirection
import platform.UIKit.NSWritingDirectionLeftToRight
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIResponder
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
import platform.UIKit.UITextInputStringTokenizer
import platform.UIKit.UITextInputTokenizerProtocol
import platform.UIKit.UITextInteraction
import platform.UIKit.UITextInteractionMode
import platform.UIKit.UITextLayoutDirection
import platform.UIKit.UITextLayoutDirectionDown
import platform.UIKit.UITextLayoutDirectionLeft
import platform.UIKit.UITextLayoutDirectionRight
import platform.UIKit.UITextLayoutDirectionUp
import platform.UIKit.UITextPosition
import platform.UIKit.UITextRange
import platform.UIKit.UITextSelectionRect
import platform.UIKit.UITextStorageDirection
import platform.UIKit.UITextWritingDirection
import platform.UIKit.UITouch
import platform.UIKit.UIView
import platform.UIKit.addInteraction
import platform.UIKit.removeInteraction
import platform.darwin.NSInteger

private val NoOpOnKeyboardPresses: (Set<*>) -> Unit = {}

/**
 * Hidden UIView to interact with iOS Keyboard and TextInput system.
 * TODO maybe need to call reloadInputViews() to update UIKit text features?
 */
@Suppress("CONFLICTING_OVERLOADS")
internal class IntermediateTextInputUIView(
    private val viewConfiguration: ViewConfiguration
) : CMPEditMenuView(frame = CGRectZero.readValue()),
    UIKeyInputProtocol, UITextInputProtocol {
    private var _inputDelegate: UITextInputDelegateProtocol? = null
    var input: IOSSkikoInput? = null
        set(value) {
            field = value
            if (value == null) {
                hideEditMenu()
            }
        }

    private val mainScope = MainScope()

    /**
     * Callback to handle keyboard presses. The parameter is a [Set] of [UIPress] objects.
     * Erasure happens due to K/N not supporting Obj-C lightweight generics.
     */
    var onKeyboardPresses: (Set<*>) -> Unit = NoOpOnKeyboardPresses

    var inputTraits: SkikoUITextInputTraits = EmptyInputTraits

    override fun canBecomeFirstResponder() = true

    private val selectionInteraction =
        UITextInteraction.textInteractionForMode(UITextInteractionMode.UITextInteractionModeEditable)
            .also {
                it.setTextInput(this)
            }

    override fun becomeFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.becomeFirstResponder()

        if (!isFirstResponder && this.isFirstResponder()) {
            this.addInteraction(selectionInteraction)
            println("Added selection interaction")
        }

        return result
    }

    override fun resignFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.resignFirstResponder()

        if (isFirstResponder && !this.isFirstResponder()) {
            this.removeInteraction(selectionInteraction)
            println("removeInteraction selection interaction")
        }

        return result
    }

    override fun beginFloatingCursorAtPoint(point: CValue<CGPoint>) {
        input?.beginFloatingCursor(point.useContents { DpOffset(x.dp, y.dp) })
    }

    override fun updateFloatingCursorAtPoint(point: CValue<CGPoint>) {
        input?.updateFloatingCursor(point.useContents { DpOffset(x.dp, y.dp) })
    }

    override fun endFloatingCursor() {
        input?.endFloatingCursor()
    }

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return if (input == null) {
            null
        } else {
            super.hitTest(point, withEvent)
        }
    }

    /**
     * A Boolean value that indicates whether the text-entry object has any text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
     */
    override fun hasText(): Boolean {
        val hasText = input?.hasText() ?: false
        println("=== hasText = $hasText")
        return hasText
    }

    /**
     * Inserts a character into the displayed text.
     * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
     * @param text A string object representing the character typed on the system keyboard.
     */
    override fun insertText(text: String) {
        input?.insertText(text)
    }

    /**
     * Deletes a character from the displayed text.
     * Remove the character just before the cursor from your class’s backing store and redisplay the text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
     */
    override fun deleteBackward() {
        input?.deleteBackward()
    }

    override fun inputDelegate(): UITextInputDelegateProtocol? {
        return _inputDelegate
    }

    override fun setInputDelegate(inputDelegate: UITextInputDelegateProtocol?) {
        _inputDelegate = inputDelegate
    }

    /**
     * Returns the text in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614527-text
     * @param range A range of text in a document.
     * @return A substring of a document that falls within the specified range.
     */
    override fun textInRange(range: UITextRange): String? {
        val text = input?.textInRange(range.toIntRange())
//        println("=== textInRange, TEXT: $text")
        return text
    }

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param withText A string to replace the text in range.
     */
    override fun replaceRange(range: UITextRange, withText: String) {
        println("=== replaceRange")
        input?.replaceRange(range.toIntRange(), withText)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        println("=== setSelectedTextRange")
        input?.setSelectedTextRange(selectedTextRange?.toIntRange())
    }

    /**
     * The range of selected text in a document.
     * If the text range has a length, it indicates the currently selected text.
     * If it has zero length, it indicates the caret (insertion point).
     * If the text-range object is nil, it indicates that there is no current selection.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614541-selectedtextrange
     */
    override fun selectedTextRange(): UITextRange? {
        val range = input?.getSelectedTextRange()
        println("=== selectedTextRange -> $range")
        return range?.toUITextRange()
    }

    /**
     * The range of currently marked text in a document.
     * If there is no marked text, the value of the property is nil.
     * Marked text is provisionally inserted text that requires user confirmation;
     * it occurs in multistage text input.
     * The current selection, which can be a caret or an extended range, always occurs within the marked text.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614489-markedtextrange
     */
    override fun markedTextRange(): UITextRange? {
        val markedTextRange = input?.markedTextRange()?.toUITextRange()
        println("=== markedTextRange -> $markedTextRange")
        return markedTextRange
    }

    override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) {
        println("=== setMarkedTextStyle")
        // do nothing
    }

    override fun markedTextStyle(): Map<Any?, *>? {
        println("=== markedTextStyle")
        return null
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
    override fun setMarkedText(markedText: String?, selectedRange: CValue<NSRange>) {
        println("=== setMarkedText")
        val (locationRelative, lengthRelative) = selectedRange.useContents {
            location.toInt() to length.toInt()
        }
        val relativeTextRange = locationRelative until locationRelative + lengthRelative

        // Due to iOS specifics, [setMarkedText] can be called several times in a row. Batching
        // helps to avoid text input problems, when Composables use parameters set during
        // recomposition instead of the current ones. Example:
        // 1. State "1" -> TextField(text = "1")
        // 2. setMarkedText "12" -> Not equal to TextField(text = "1") -> State "12"
        // 3. setMarkedText "1" -> Equal to TextField(text = "1") -> State remains "12"
        // scene.render() - Recomposes TextField
        // 4. State "12" -> TextField(text = "12") - Invalid state. Should be TextField(text = "1")
        input?.withBatch {
            input?.setMarkedText(markedText, relativeTextRange)
        }
    }

    /**
     * Unmarks the currently marked text.
     * After this method is called, the value of markedTextRange is nil.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614512-unmarktext
     */
    override fun unmarkText() {
        println("=== unmarkText")
        input?.unmarkText()
    }

    override fun beginningOfDocument(): UITextPosition {
        println("=== beginningOfDocument")
        return IntermediateTextPosition(0)
    }

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    override fun endOfDocument(): UITextPosition {
        println("=== endOfDocument")
        return IntermediateTextPosition(input?.endOfDocument() ?: 0)
    }

    /**
     * Attention! fromPosition and toPosition may be null
     */
    override fun textRangeFromPosition(
        fromPosition: UITextPosition,
        toPosition: UITextPosition
    ): UITextRange? {
        val from = (fromPosition as? IntermediateTextPosition)?.position ?: 0
        val to = (toPosition as? IntermediateTextPosition)?.position ?: 0
        println("=== textRangeFromPosition from = $from to $to")
        return IntermediateTextRange(
            IntermediateTextPosition(minOf(from, to)),
            IntermediateTextPosition(maxOf(from, to))
        )
    }

    /**
     * Attention! position may be null
     * @param position a custom UITextPosition object that represents a location in a document.
     * @param offset a character offset from position. It can be a positive or negative value.
     * Offset should be considered as a number of Unicode characters. One Unicode character can contain several bytes.
     */
    override fun positionFromPosition(
        position: UITextPosition,
        offset: NSInteger
    ): UITextPosition? {
        val p = (position as? IntermediateTextPosition)?.position ?: return null
        val endOfDocument = input?.endOfDocument()
        return if (endOfDocument != null) {
            val result = input?.positionFromPosition(position = p, offset = offset)
            println("=== positionFromPosition, resultInput = $result, return = ${result ?: (p + offset).coerceIn(0, endOfDocument)}")
            IntermediateTextPosition(result ?: (p + offset).coerceIn(0, endOfDocument))
        } else {
            null
        }
    }

    override fun positionFromPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
        offset: NSInteger
    ): UITextPosition? {
        println("=== positionFromPosition in Direction")
        // TODO: Handle directions
        return when (inDirection) {
            UITextLayoutDirectionLeft, UITextLayoutDirectionUp -> {
                positionFromPosition(position, -offset)
            }

            else -> positionFromPosition(position, offset)
        }
    }

    /**
     * Attention! position and toPosition may be null
     */
    override fun comparePosition(
        position: UITextPosition,
        toPosition: UITextPosition
    ): NSComparisonResult {
        val from = (position as? IntermediateTextPosition)?.position ?: 0
        val to = (toPosition as? IntermediateTextPosition)?.position ?: 0
        val result = if (from < to) {
            NSOrderedAscending
        } else if (from > to) {
            NSOrderedDescending
        } else {
            NSOrderedSame
        }
        val resultText = when (result) {
            NSOrderedAscending -> "NSOrderedAscending"
            NSOrderedDescending -> "NSOrderedDescending"
            NSOrderedSame -> "NSOrderedSame"
            else -> "Unknown"
        }
        println("=== comparePosition, from = ${from}, to = ${to} result = $resultText")
        return result
    }

    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): NSInteger {
        if (from !is IntermediateTextPosition) {
            error("from !is IntermediateTextPosition")
        }
        if (toPosition !is IntermediateTextPosition) {
            error("toPosition !is IntermediateTextPosition")
        }
        println("=== offsetFromPosition, toPosition.position - from.position -> ${toPosition.position} - ${from.position} = ${toPosition.position - from.position}")
        return toPosition.position - from.position
    }

    override fun positionWithinRange(
        range: UITextRange,
        atCharacterOffset: NSInteger
    ): UITextPosition? {
        println("=== positionWithinRange at char")
        return IntermediateTextPosition(0)
    }

    override fun positionWithinRange(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition? {
        println("=== positionWithinRange in direction")
        return IntermediateTextPosition(0)
    }

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        println("=== characterRangeByExtendingPosition")
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
        return null // TODO characterRangeByExtendingPosition
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): NSWritingDirection {
        println("=== baseWritingDirectionForPosition")
        return NSWritingDirectionLeftToRight // TODO support RTL text direction
    }

    override fun setBaseWritingDirection(
        writingDirection: NSWritingDirection,
        forRange: UITextRange
    ) {
        println("=== setBaseWritingDirection")
        // TODO support RTL text direction
    }

    //Working with Geometry and Hit-Testing. Some methods return stubs for now.
    override fun firstRectForRange(range: UITextRange): CValue<CGRect> {
        println("=== !!! firstRectForRange iOS side")
        return input?.currentFocusedDpRect()?.asCGRect() ?: return CGRectNull.readValue()
    }

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> {
        val fallbackRect = CGRectMake(x = 1.0, y = 1.0, width = 1.0, height = 1.0)
        val longPosition = (position as? IntermediateTextPosition)?.position ?: return fallbackRect
        val caretDpRect = input?.caretDpRectForPosition(longPosition)
        println("=== !!! caretRectForPosition iOS side, dpRect = ${caretDpRect}")
        val debugRect = DpRect(left = 27.dp, top = 166.dp, right = 54.dp, bottom = 183.dp)
//        return caretDpRect?.asCGRect() ?: fallbackRect
        return debugRect.asCGRect()
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> {
        println("=== !!! selectionRectsForRange iOS side: range(${(range.start as? IntermediateTextPosition)?.position}, ${(range.end as? IntermediateTextPosition)?.position})")
        val fallbackList = listOf<UITextSelectionRect>() // can't be empty LOL
        val start = (range.start as? IntermediateTextPosition)?.position ?: return fallbackList
        val end = (range.end as? IntermediateTextPosition)?.position ?: return fallbackList

//        val dpRect = input?.selectionDpRectsForRange(IntRange(start.toInt(), end.toInt()))?.first() ?: return fallbackList
        val debugRect = DpRect(left = 27.dp, top = 166.dp, right = 54.dp, bottom = 183.dp)
//        val resultRect = IntermediateTextSelectionRect(_rect = dpRect.asCGRect(), _writingDirection = NSWritingDirectionLeftToRight, _containsStart = true, _containsEnd = true, _isVertical = false)
        val resultRect = IntermediateTextSelectionRect(_rect = debugRect.asCGRect(), _writingDirection = NSWritingDirectionLeftToRight, _containsStart = true, _containsEnd = true, _isVertical = false)
        println("resultRect = $resultRect, ${resultRect.rect().asDpRect()}")
        return listOf(resultRect)
    }

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? {
        println("=== !!! closestPositionToPoint iOS side")
        val closestPosition =
            input?.closestPositionToPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return IntermediateTextPosition(closestPosition)
    }

    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? {
        println("=== !!! closestPositionToPoint withinRange iOS side")
        val intWithinRange = IntermediateTextRange(
            withinRange.start as IntermediateTextPosition,
            withinRange.end as IntermediateTextPosition
        ).toIntRange() // TODO: ensure to use it in new methods
        val closestPosition = input?.closestPositionToPoint(
            point.useContents { DpOffset(x.dp, y.dp) },
            intWithinRange
        ) ?: return null
        return IntermediateTextPosition(closestPosition)
    }

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? {
        println("=== !!! characterRangeAtPoint iOS side")
        val characterRange =
            input?.characterRangeAtPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return IntermediateTextRange(characterRange.start, characterRange.endInclusive)
    }

//    override fun textStylingAtPosition(
//        position: UITextPosition,
//        inDirection: UITextStorageDirection
//    ): Map<Any?, *>? {
//        println("=== !!! textStylingAtPosition")
//        return NSDictionary.dictionary()
//        //TODO: Need to implement
//    }

    override fun characterOffsetOfPosition(
        position: UITextPosition,
        withinRange: UITextRange
    ): NSInteger {
        println("=== !!! characterOffsetOfPosition withinRange")
        if (position !is IntermediateTextPosition) {
            error("position !is IntermediateTextPosition")
        }
        return 0 // TODO: characterOffsetOfPosition
    }

    override fun shouldChangeTextInRange(range: UITextRange, replacementText: String): Boolean {
        // Here we should decide to replace text in range or not.
        // By default, this method returns true.
        return true
    }

    override fun textInputView(): UIView {
        return this
    }

    override fun keyboardType(): UIKeyboardType = inputTraits.keyboardType()
    override fun keyboardAppearance(): UIKeyboardAppearance = inputTraits.keyboardAppearance()
    override fun returnKeyType(): UIReturnKeyType = inputTraits.returnKeyType()
    override fun textContentType(): UITextContentType = inputTraits.textContentType()
    override fun isSecureTextEntry(): Boolean = inputTraits.isSecureTextEntry()
    override fun enablesReturnKeyAutomatically(): Boolean =
        inputTraits.enablesReturnKeyAutomatically()

    override fun autocapitalizationType(): UITextAutocapitalizationType =
        inputTraits.autocapitalizationType()

    override fun autocorrectionType(): UITextAutocorrectionType = inputTraits.autocorrectionType()

    override fun dictationRecognitionFailed() {
        //todo may be useful
    }

    override fun dictationRecordingDidEnd() {
        //todo may be useful
    }

    /**
     * Call when something changes in text data
     */
    fun textWillChange() {
        _inputDelegate?.textWillChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun textDidChange() {
        _inputDelegate?.textDidChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun selectionWillChange() {
        _inputDelegate?.selectionWillChange(this)
    }

    /**
     * Call when something changes in text data
     */
    fun selectionDidChange() {
        _inputDelegate?.selectionDidChange(this)
    }

    override fun isUserInteractionEnabled(): Boolean = true

    override fun editMenuDelay(): Double =
        viewConfiguration.doubleTapTimeoutMillis.milliseconds.toDouble(DurationUnit.SECONDS)

    /**
     * Show copy/paste text menu
     * @param targetRect - rectangle of selected text area
     * @param textActions - available (not null) actions in text menu
     */
    fun showTextMenu(targetRect: CValue<CGRect>, textActions: TextActions) {
        this.showEditMenuAtRect(
            targetRect = targetRect,
            copy = textActions.copy,
            cut = textActions.cut,
            paste = textActions.paste,
            selectAll = textActions.selectAll
        )
    }

    fun hideTextMenu() = this.hideEditMenu()

    fun isTextMenuShown() = isEditMenuShown

    override fun tokenizer(): UITextInputTokenizerProtocol {
        println("=== tokenizer iOS side")
        return UITextInputStringTokenizer(textInput = this)
    }

    fun resetOnKeyboardPressesCallback() {
        onKeyboardPresses = NoOpOnKeyboardPresses
    }

    private fun IOSSkikoInput.withBatch(update: () -> Unit) {
        beginEditBatch()
        update()
        mainScope.launch {
            endEditBatch()
        }
    }
}

private class IntermediateTextPosition(val position: Long = 0) : UITextPosition()

internal class IntermediateTextSelectionRect(
    private var _rect: CValue<CGRect>,
    private val _writingDirection: UITextWritingDirection,
    private val _containsStart: Boolean,
    private val _containsEnd: Boolean,
    private val _isVertical: Boolean

): UITextSelectionRect() {
    override fun rect(): CValue<CGRect> = _rect
    override fun writingDirection(): NSWritingDirection = _writingDirection
    override fun containsStart(): Boolean = _containsStart
    override fun containsEnd(): Boolean = _containsEnd
    override fun isVertical(): Boolean = _isVertical
}

private fun IntermediateTextRange(start: Int, end: Int) =
    IntermediateTextRange(
        _start = IntermediateTextPosition(start.toLong()),
        _end = IntermediateTextPosition(end.toLong())
    )

private class IntermediateTextRange(
    private val _start: IntermediateTextPosition,
    private val _end: IntermediateTextPosition
) : UITextRange() {
    override fun isEmpty() = (_end.position - _start.position) <= 0
    override fun start(): UITextPosition = _start
    override fun end(): UITextPosition = _end
}

private fun UITextRange.toIntRange(): IntRange {
    val start = (start() as IntermediateTextPosition).position.toInt()
    val end = (end() as IntermediateTextPosition).position.toInt()
    return start until end
}

private fun IntRange.toUITextRange(): UITextRange =
    IntermediateTextRange(start = start, end = endInclusive + 1)

private fun NSWritingDirection.directionToStr() =
    when (this) {
        UITextLayoutDirectionLeft -> "Left"
        UITextLayoutDirectionRight -> "Right"
        UITextLayoutDirectionUp -> "Up"
        UITextLayoutDirectionDown -> "Down"
        else -> "Unknown"
    }
