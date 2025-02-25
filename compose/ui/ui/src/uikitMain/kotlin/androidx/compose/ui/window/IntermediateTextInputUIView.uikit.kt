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

import androidx.compose.ui.platform.EmptyInputTraits
import androidx.compose.ui.platform.IOSSkikoInput
import androidx.compose.ui.platform.SkikoUITextInputTraits
import androidx.compose.ui.platform.TextActions
import androidx.compose.ui.platform.TextSelectionRect
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.uikit.utils.CMPEditMenuView
import androidx.compose.ui.uikit.utils.CMPTextInputStringTokenizer
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import kotlin.math.max
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.DurationUnit
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.jetbrains.skia.BreakIterator
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectContainsPoint
import platform.CoreGraphics.CGRectEqualToRect
import platform.CoreGraphics.CGRectGetHeight
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectGetWidth
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGRectNull
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSComparisonResult
import platform.Foundation.NSOrderedAscending
import platform.Foundation.NSOrderedDescending
import platform.Foundation.NSOrderedSame
import platform.Foundation.NSRange
import platform.UIKit.NSWritingDirection
import platform.UIKit.NSWritingDirectionLeftToRight
import platform.UIKit.NSWritingDirectionNatural
import platform.UIKit.UIEdgeInsetsMake
import platform.UIKit.UIEvent
import platform.UIKit.UIKeyInputProtocol
import platform.UIKit.UIKeyboardAppearance
import platform.UIKit.UIKeyboardType
import platform.UIKit.UIMenuAutoFill
import platform.UIKit.UIMenuBuilderProtocol
import platform.UIKit.UIPress
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIResponder
import platform.UIKit.UIReturnKeyType
import platform.UIKit.UIScrollView
import platform.UIKit.UITextAutocapitalizationType
import platform.UIKit.UITextAutocorrectionType
import platform.UIKit.UITextContentType
import platform.UIKit.UITextDirection
import platform.UIKit.UITextGranularity
import platform.UIKit.UITextInputDelegateProtocol
import platform.UIKit.UITextInputProtocol
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
import platform.UIKit.UITextStorageDirectionForward
import platform.UIKit.UITextWritingDirection
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
                resignFirstResponder()
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

    override fun layoutSubviews() {
        super.layoutSubviews()

        hideCursorView()
    }

    override fun becomeFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.becomeFirstResponder()

        if (!isFirstResponder && this.isFirstResponder()) {
            this.addInteraction(selectionInteraction)
        }

        return result
    }

    override fun resignFirstResponder(): Boolean {
        val isFirstResponder = this.isFirstResponder()
        val result = super.resignFirstResponder()

        if (isFirstResponder && !this.isFirstResponder()) {
            this.removeInteraction(selectionInteraction)
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

    /**
     * A Boolean value that indicates whether the text-entry object has any text.
     * https://developer.apple.com/documentation/uikit/uikeyinput/1614457-hastext
     */
    override fun hasText(): Boolean {
        val hasText = input?.hasText() ?: false
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
        return input?.textInRange(range.toTextRange())
    }

    /**
     * Replaces the text in a document that is in the specified range.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614558-replace
     * @param range A range of text in a document.
     * @param withText A string to replace the text in range.
     */
    override fun replaceRange(range: UITextRange, withText: String) {
        input?.replaceRange(range.toTextRange(), withText)
    }

    override fun setSelectedTextRange(selectedTextRange: UITextRange?) {
        input?.setSelectedTextRange(selectedTextRange?.toTextRange())
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
        return markedTextRange
    }

    override fun setMarkedTextStyle(markedTextStyle: Map<Any?, *>?) {
        // do nothing
    }

    override fun markedTextStyle(): Map<Any?, *>? {
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
        val (locationRelative, lengthRelative) = selectedRange.useContents {
            location.toInt() to length.toInt()
        }
        val relativeTextRange = TextRange(locationRelative, locationRelative + lengthRelative)

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
        input?.unmarkText()
    }

    override fun beginningOfDocument(): UITextPosition {
        return IntermediateTextPosition(0)
    }

    /**
     * The text position for the end of a document.
     * https://developer.apple.com/documentation/uikit/uitextinput/1614555-endofdocument
     */
    override fun endOfDocument(): UITextPosition {
        return IntermediateTextPosition(input?.endOfDocument() ?: 0)
    }

    /**
     * Attention! fromPosition and toPosition may be null
     */
    override fun textRangeFromPosition(
        fromPosition: UITextPosition,
        toPosition: UITextPosition
    ): UITextRange? {
        val from = (fromPosition as? IntermediateTextPosition)?.position ?: return null
        val to = (toPosition as? IntermediateTextPosition)?.position ?: return null
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
        val input = input ?: return null
        return input.positionFromPosition(position = p, offset = offset.toInt())?.let {
            IntermediateTextPosition(it)
        }
    }

    private fun positionFromPositionVertical(
        position: UITextPosition,
        offset: NSInteger
    ): UITextPosition? {
        val p = (position as? IntermediateTextPosition)?.position ?: return null
        val input = input ?: return null
        return input.verticalPositionFromPosition(position = p, verticalOffset = offset.toInt())
            ?.let { IntermediateTextPosition(it) }
    }

    override fun positionFromPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection,
        offset: NSInteger
    ): UITextPosition? {
        return when (inDirection) {
            UITextLayoutDirectionLeft -> positionFromPosition(position, -offset)
            UITextLayoutDirectionRight -> positionFromPosition(position, offset)
            UITextLayoutDirectionDown -> positionFromPositionVertical(position, offset)
            UITextLayoutDirectionUp -> positionFromPositionVertical(position, -offset)
            else -> null
        }
    }

    /**
     * Attention! position and toPosition may be null
     */
    override fun comparePosition(
        position: UITextPosition,
        toPosition: UITextPosition
    ): NSComparisonResult {
        val from = (position as? IntermediateTextPosition)?.position ?: return NSOrderedSame
        val to = (toPosition as? IntermediateTextPosition)?.position ?: return NSOrderedSame
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
        return result
    }

    override fun offsetFromPosition(from: UITextPosition, toPosition: UITextPosition): NSInteger {
        if (from !is IntermediateTextPosition) {
            error("from !is IntermediateTextPosition: $from")
        }
        if (toPosition !is IntermediateTextPosition) {
            error("toPosition !is IntermediateTextPosition: $toPosition")
        }
        return (toPosition.position - from.position).toLong()
    }

    override fun positionWithinRangeAtCharacterOffset(
        range: UITextRange,
        atCharacterOffset: NSInteger
    ): UITextPosition {
        return IntermediateTextPosition(0)
    }

    override fun positionWithinRangeFarthestInDirection(
        range: UITextRange,
        farthestInDirection: UITextLayoutDirection
    ): UITextPosition {
        return IntermediateTextPosition(0)
    }

    override fun characterRangeByExtendingPosition(
        position: UITextPosition,
        inDirection: UITextLayoutDirection
    ): UITextRange? {
        val oldPosition = position as? IntermediateTextPosition ?: return null
        val newPosition = positionFromPosition(oldPosition, inDirection = inDirection, offset = 1)
            as? IntermediateTextPosition ?: return null
        return if (newPosition.position < oldPosition.position) {
            IntermediateTextRange(newPosition, oldPosition)
        } else {
            IntermediateTextRange(oldPosition, newPosition)
        }
    }

    override fun baseWritingDirectionForPosition(
        position: UITextPosition,
        inDirection: UITextStorageDirection
    ): NSWritingDirection {
        return NSWritingDirectionLeftToRight // TODO support RTL text direction
    }

    override fun setBaseWritingDirection(
        writingDirection: NSWritingDirection,
        forRange: UITextRange
    ) {
        // TODO support RTL text direction
    }

    // Working with Geometry and Hit-Testing. Some methods return stubs for now.
    override fun firstRectForRange(range: UITextRange): CValue<CGRect> {
        return input?.currentFocusedDpRect()?.asCGRect() ?: return CGRectNull.readValue()
    }

    override fun caretRectForPosition(position: UITextPosition): CValue<CGRect> {
        // Cursor is drawing on Compose canvas, hence no need to display it in UIKit.
        // Returning zero-width rect that will hide cursor on iOS 13 - iOS 16.
        // On iOS 17+ cursor is removed manually after it is placed.

        mainScope.launch {
            hideCursorView()
        }

        val fallbackRect = CGRectMake(x = 1.0, y = 1.0, width = 0.0, height = 1.0)
        val longPosition = (position as? IntermediateTextPosition)?.position ?: return fallbackRect
        val caretDpRect = input?.caretDpRectForPosition(longPosition)?.let {
            it.copy(right = it.left)
        }
        return caretDpRect?.asCGRect() ?: fallbackRect
    }

    override fun selectionRectsForRange(range: UITextRange): List<*> {
        val fallbackList = listOf<UITextSelectionRect>() // can't be empty?
        val textRange = TextRange(
            start = (range.start as? IntermediateTextPosition)?.position ?: return fallbackList,
            end = (range.end as? IntermediateTextPosition)?.position ?: return fallbackList
        )
        val rects = input?.selectionRectsForRange(textRange) ?: return fallbackList

        // HACK: On iOS 17+, selection changes are not submitted during selection interaction.
        //
        if (available(OS.Ios to OSVersion(major = 17))) {
            shouldPerformSelectionHotifications = false
            if (input?.getSelectedTextRange() != textRange) {
                input?.setSelectedTextRange(textRange)
            }
            shouldPerformSelectionHotifications = true
        }

        return rects.fastMap { IntermediateTextSelectionRect(it) }
    }

    override fun closestPositionToPoint(point: CValue<CGPoint>): UITextPosition? {
        val closestPosition =
            input?.closestPositionToPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return IntermediateTextPosition(closestPosition)
    }

    override fun closestPositionToPoint(
        point: CValue<CGPoint>,
        withinRange: UITextRange
    ): UITextPosition? {
        val textRange = (withinRange as? IntermediateTextRange)?.toTextRange() ?: return null

        val closestPosition = input?.closestPositionToPoint(
            point.useContents { DpOffset(x.dp, y.dp) },
            textRange
        ) ?: return null

        return IntermediateTextPosition(closestPosition)
    }

    override fun characterRangeAtPoint(point: CValue<CGPoint>): UITextRange? {
        val characterRange =
            input?.characterRangeAtPoint(point.useContents { DpOffset(x.dp, y.dp) }) ?: return null
        return IntermediateTextRange(characterRange.start, characterRange.end)
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

    override fun buildMenuWithBuilder(builder: UIMenuBuilderProtocol) {
        if (available(OS.Ios to OSVersion(major = 17))) {
            builder.removeMenuForIdentifier(UIMenuAutoFill)
        }
        super.buildMenuWithBuilder(builder)
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
    var shouldPerformSelectionHotifications: Boolean = true
    fun selectionWillChange() {
        if (shouldPerformSelectionHotifications) {
            _inputDelegate?.selectionWillChange(this)
        }
    }

    /**
     * Call when something changes in text data
     */
    fun selectionDidChange() {
        if (shouldPerformSelectionHotifications) {
            _inputDelegate?.selectionDidChange(this)
        }
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

    private val _tokenizer = IntermediateTextTokenizer(textInput = this) {
        input?.let { it.textInRange(TextRange(0, it.endOfDocument())) }
    }
    override fun tokenizer(): UITextInputTokenizerProtocol = _tokenizer

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

    private fun hideCursorView() {
        val cursorViewClass = when {
            available(OS.Ios to OSVersion(major = 17, minor = 4)) -> "UIStandardTextCursorView"
            available(OS.Ios to OSVersion(major = 17)) -> "_UITextCursorView"
            else -> return
        }

        subviews.forEach { subview ->
            subview as UIView
            if (subview::class.simpleName == cursorViewClass) {
                subview.setHidden(true)
            }
        }
    }
}

private class IntermediateTextPosition(val position: Int = 0) : UITextPosition() {
    override fun description(): String {
        return "IntermediateTextPosition($position)"
    }

    init {
        assert(position >= 0) { "position should be >= 0" }
    }
}

private class IntermediateTextSelectionRect(
    private var _rect: CValue<CGRect>,
    private val _writingDirection: UITextWritingDirection,
    private val _containsStart: Boolean,
    private val _containsEnd: Boolean,
    private val _isVertical: Boolean

) : UITextSelectionRect() {
    constructor(textSelectionRect: TextSelectionRect) : this(
        textSelectionRect.dpRect.asCGRect(),
        NSWritingDirectionNatural,
        textSelectionRect.containsStart,
        textSelectionRect.containsEnd,
        textSelectionRect.isVertical
    )

    override fun rect(): CValue<CGRect> = _rect
    override fun writingDirection(): NSWritingDirection = _writingDirection
    override fun containsStart(): Boolean = _containsStart
    override fun containsEnd(): Boolean = _containsEnd
    override fun isVertical(): Boolean = _isVertical
}

private fun IntermediateTextRange(start: Int, end: Int) =
    IntermediateTextRange(
        _start = IntermediateTextPosition(start),
        _end = IntermediateTextPosition(end)
    )

private class IntermediateTextRange(
    val _start: IntermediateTextPosition,
    val _end: IntermediateTextPosition
) : UITextRange() {
    override fun isEmpty() = (_end.position - _start.position) <= 0
    override fun start(): UITextPosition = _start
    override fun end(): UITextPosition = _end

    override fun description(): String {
        return "IntermediateTextRange(start=$_start, end=$_end)"
    }
}

private fun UITextRange.toTextRange(): TextRange {
    val start = (start() as IntermediateTextPosition).position
    val end = (end() as IntermediateTextPosition).position
    return TextRange(start, end)
}

private fun TextRange.toUITextRange(): UITextRange =
    IntermediateTextRange(start = start, end = end)

private fun UITextLayoutDirection.directionToStr() =
    when (this) {
        UITextLayoutDirectionLeft -> "Left"
        UITextLayoutDirectionRight -> "Right"
        UITextLayoutDirectionUp -> "Up"
        UITextLayoutDirectionDown -> "Down"
        else -> "Unknown"
    }

internal class IntermediateTextTokenizer(
    textInput: UIResponder,
    val getString: () -> String?
): CMPTextInputStringTokenizer(textInput) {
    override fun positionFromPosition(
        position: UITextPosition,
        toBoundary: UITextGranularity,
        inDirection: UITextDirection
    ): UITextPosition? {
        val textPosition = position as? IntermediateTextPosition ?: return null
        val isForward = inDirection == UITextStorageDirectionForward ||
            inDirection == UITextLayoutDirectionRight ||
            inDirection == UITextLayoutDirectionDown

        val iterator = when (toBoundary) {
            UITextGranularity.UITextGranularityCharacter -> BreakIterator.makeCharacterInstance()
            UITextGranularity.UITextGranularityWord -> BreakIterator.makeWordInstance()
            UITextGranularity.UITextGranularitySentence -> BreakIterator.makeSentenceInstance()
            UITextGranularity.UITextGranularityLine -> BreakIterator.makeLineInstance()
            UITextGranularity.UITextGranularityParagraph ->
                return positionFromPositionToParagraphBoundary(position, isForward)

            else -> return super.positionFromPosition(position, toBoundary, inDirection)
        }

        val string = getString() ?: ""
        iterator.setText(string)

        val iteratorResult = if (isForward) {
            if (textPosition.position >= string.length - 1) {
                string.length
            } else {
                iterator.following(textPosition.position)
            }
        } else {
            if (textPosition.position <= 0) {
                0
            } else {
                iterator.preceding(textPosition.position)
            }
        }

        return IntermediateTextPosition(iteratorResult)
    }

    override fun isPositionAtBoundary(
        position: UITextPosition,
        atBoundary: UITextGranularity,
        inDirection: UITextDirection
    ): Boolean {
        val textPosition = position as? IntermediateTextPosition ?: return false

        val iterator = when (atBoundary) {
            UITextGranularity.UITextGranularityCharacter -> BreakIterator.makeCharacterInstance()
            UITextGranularity.UITextGranularityWord -> BreakIterator.makeWordInstance()
            UITextGranularity.UITextGranularitySentence -> BreakIterator.makeSentenceInstance()
            UITextGranularity.UITextGranularityLine -> BreakIterator.makeLineInstance()
            UITextGranularity.UITextGranularityParagraph ->
                // TODO: Properly implement Paragraph boundary check, or write comment why false value is optimal one here.
                return false

            else -> return super.isPositionAtBoundary(position, atBoundary, inDirection)
        }

        iterator.setText(getString() ?: "")
        return iterator.isBoundary(textPosition.position)
    }

    private fun positionFromPositionToParagraphBoundary(
        position: UITextPosition,
        isForward: Boolean
    ): UITextPosition? {
        val textPosition = position as? IntermediateTextPosition ?: return null
        val newlineCharacters: Set<Char> = setOf('\n', '\r', '\u2029')

        val string = getString() ?: ""
        var location = textPosition.position
        while (isForward && location < string.length || !isForward && location > 0) {
            if (isForward) {
                if (string[location] in newlineCharacters) {
                    break
                }
                location++
            } else {
                if (string[location] in newlineCharacters) {
                    location++
                    break
                }
                location--
            }
        }
        return IntermediateTextPosition(location)
    }
}

internal class IntermediateTextScrollView(): UIScrollView(frame = CGRectZero.readValue()) {
    init {
        setScrollEnabled(false)
        setShowsVerticalScrollIndicator(false)
        setShowsHorizontalScrollIndicator(false)
        setCanCancelContentTouches(false)
        setDelaysContentTouches(false)
        setClipsToBounds(false)
    }

    var textUIView: IntermediateTextInputUIView? = null
        set(value) {
            if (field != value) {
                field?.removeFromSuperview()
                field = value
                value?.let {
                    addSubview(value)
                }
            }
        }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        return if (textUIView != null) {
            super.hitTest(point, withEvent) ?: textUIView?.outOfTheBoundsHitTest(point, withEvent)
        } else {
            null
        }
    }

    fun setFrame(frame: CValue<CGRect>, bounds: CValue<CGRect>) {
        textUIView?.setFrame(
            CGRectMake(
                x = 0.0,
                y = 0.0,
                width = CGRectGetWidth(bounds),
                height = CGRectGetHeight(bounds)
            )
        )

        val inset = UIEdgeInsetsMake(
            top = max(0.0, -CGRectGetMinY(bounds)),
            left = max(0.0, -CGRectGetMinX(bounds)),
            bottom = max(0.0, CGRectGetHeight(frame) - CGRectGetHeight(bounds) + CGRectGetMinY(bounds)),
            right = max(0.0, CGRectGetWidth(frame) - CGRectGetWidth(bounds) + CGRectGetMinX(bounds))
        )
        setFrame(frame)
        setContentInset(inset)
        setContentSize(bounds.useContents { size.readValue() })
        setContentOffset(bounds.useContents { origin.readValue() })
    }

    fun interactionModeAt(point: CValue<CGPoint>): UIKitInteropInteractionMode? {
        val textView = textUIView ?: return null
        val hitTested = outOfTheBoundsHitTest(point, withEvent = null)
        hitTested ?: return null

        return if (CGRectEqualToRect(hitTested.bounds, textView.bounds)) {
            UIKitInteropInteractionMode.Cooperative()
        } else {
            UIKitInteropInteractionMode.NonCooperative
        }
    }
}

private fun UIView.outOfTheBoundsHitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
    subviews.reversed().forEach { subview ->
        subview as UIView
        val subviewPoint = this.convertPoint(point, toView = subview)
        subview.outOfTheBoundsHitTest(subviewPoint, withEvent)?.let {
            return it
        }
    }
    return this.takeIf { isUserInteractionEnabled() && CGRectContainsPoint(bounds, point) }
}