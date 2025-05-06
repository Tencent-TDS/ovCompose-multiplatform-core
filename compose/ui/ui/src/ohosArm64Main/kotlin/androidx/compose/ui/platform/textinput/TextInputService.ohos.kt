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

package androidx.compose.ui.platform.textinput

import androidx.compose.runtime.collection.mutableVectorOf
import androidx.compose.ui.graphics.kLog
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.textinput.TextInputService.TextInputCommand.HideKeyboard
import androidx.compose.ui.platform.textinput.TextInputService.TextInputCommand.ShowKeyboard
import androidx.compose.ui.platform.textinput.TextInputService.TextInputCommand.StartInput
import androidx.compose.ui.platform.textinput.TextInputService.TextInputCommand.StopInput
import androidx.compose.ui.text.input.BackspaceCommand
import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.EditProcessor
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.ImeOptions
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.text.input.TextFieldValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val DEBUG = false
private const val TAG = "OhosTextInputService"
private const val DEBUG_CLASS = "OhosTextInputService"

internal class TextInputService : PlatformTextInputService {

    /**
     * Commands that can be sent into [textInputCommandQueue] to be processed by
     * [processInputCommands].
     */
    private enum class TextInputCommand {
        StartInput,
        StopInput,
        ShowKeyboard,
        HideKeyboard;
    }

    private val inputCommandProcessorExecutor = DelayExecutor(100.milliseconds)

    private var currentInput: CurrentInput? = null
    private var currentImeOptions: ImeOptions? = null
    private var currentImeActionHandler: ((ImeAction) -> Unit)? = null

    /**
     * A channel that is used to debounce rapid operations such as showing/hiding the keyboard and
     * starting/stopping input, so we can make the minimal number of calls on the
     * [inputMethodManager]. The [TextInputCommand]s sent to this channel are processed by
     * [processInputCommands].
     */
    private val textInputCommandQueue = mutableVectorOf<TextInputCommand>()
    private var frameCallback: Runnable? = null

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

    /**
     * Workaround to fix voice dictation.
     * UIKit call insertText(text) and replaceRange(range,text) immediately,
     * but Compose recomposition happen on next draw frame.
     * So the value of getSelectedTextRange is in the old state when the replaceRange function is called.
     * @see _tempCursorPos helps to fix this behaviour. Permanently update _tempCursorPos in function insertText.
     * And after clear in updateState function.
     */
    private var _tempCursorPos: Int? = null

    init {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.create")
        }
    }

    override fun startInput(
        value: TextFieldValue,
        imeOptions: ImeOptions,
        onEditCommand: (List<EditCommand>) -> Unit,
        onImeActionPerformed: (ImeAction) -> Unit
    ) {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.startInput")
        }
        currentInput = CurrentInput(value, OhosTextConfig(imeOptions), onEditCommand, onImeActionPerformed)
        _tempCurrentInputSession = EditProcessor().apply {
            reset(value, null)
        }
        currentImeOptions = imeOptions
        currentImeActionHandler = onImeActionPerformed

        // Don't actually send the command to the IME yet, it may be overruled by a subsequent call
        // to stopInput.
        sendInputCommand(StartInput)
    }

    override fun startInput() {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.startInput")
        }

        // Don't set editorHasFocus or any of the other properties used to support the legacy text
        // input system.

        // Don't actually send the command to the IME yet, it may be overruled by a subsequent call
        // to stopInput.
        sendInputCommand(StartInput)
    }

    override fun stopInput() {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.stopInput")
        }
        currentInput = null
        _tempCurrentInputSession = null
        currentImeOptions = null
        currentImeActionHandler = null

        // Don't actually send the command to the IME yet, it may be overruled by a subsequent call
        // to startInput.
        sendInputCommand(StopInput)
    }

    override fun showSoftwareKeyboard() {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.showSoftwareKeyboard")
        }
        sendInputCommand(ShowKeyboard)
    }

    override fun hideSoftwareKeyboard() {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.hideSoftwareKeyboard")
        }
        sendInputCommand(HideKeyboard)
    }

    private fun sendInputCommand(command: TextInputCommand) {
        textInputCommandQueue += command
        if (frameCallback == null) {
            frameCallback = Runnable {
                frameCallback = null
                processInputCommands()
            }.also(inputCommandProcessorExecutor::execute)
        }
    }

    private fun processInputCommands() {
        // Multiple commands may have been queued up in the channel while this function was
        // waiting to be resumed. We don't execute the commands as they come in because making a
        // bunch of calls to change the actual IME quickly can result in flickers. Instead, we
        // manually coalesce the commands to figure out the minimum number of IME operations we
        // need to get to the desired final state.
        // The queued commands effectively operate on a simple state machine consisting of two
        // flags:
        //   1. Whether to start a new input connection (true), tear down the input connection
        //      (false), or leave the current connection as-is (null).
        var startInput: Boolean? = null
        //   2. Whether to show the keyboard (true), hide the keyboard (false), or leave the
        //      keyboard visibility as-is (null).
        var showKeyboard: Boolean? = null

        // And a function that performs the appropriate state transition given a command.
        fun TextInputCommand.applyToState() {
            when (this) {
                StartInput -> {
                    // Any commands before restarting the input are meaningless since they would
                    // apply to the connection we're going to tear down and recreate.
                    // Starting a new connection implicitly stops the previous connection.
                    startInput = true
                    // It doesn't make sense to start a new connection without the keyboard
                    // showing.
                    showKeyboard = true
                }

                StopInput -> {
                    startInput = false
                    // It also doesn't make sense to keep the keyboard visible if it's not
                    // connected to anything. Note that this is different than the Android
                    // default behavior for Views, which is to keep the keyboard showing even
                    // after the view that the IME was shown for loses focus.
                    // See this doc for some notes and discussion on whether we should auto-hide
                    // or match Android:
                    // https://docs.google.com/document/d/1o-y3NkfFPCBhfDekdVEEl41tqtjjqs8jOss6txNgqaw/edit?resourcekey=0-o728aLn51uXXnA4Pkpe88Q#heading=h.ieacosb5rizm
                    showKeyboard = false
                }

                ShowKeyboard,
                HideKeyboard -> {
                    // Any keyboard visibility commands sent after input is stopped but before
                    // input is started should be ignored.
                    // Otherwise, the last visibility command sent either before the last stop
                    // command, or after the last start command, is the one that should take
                    // effect.
                    if (startInput != false) {
                        showKeyboard = this == ShowKeyboard
                    }
                }
            }
        }

        // Feed all the queued commands into the state machine.
        textInputCommandQueue.forEach { command ->
            command.applyToState()
            if (DEBUG) {
                log(
                    TAG, "$DEBUG_CLASS.textInputCommandEventLoop.$command " +
                        "(startInput=$startInput, showKeyboard=$showKeyboard)"
                )
            }
        }
        textInputCommandQueue.clear()

        // Now that we've calculated what operations we need to perform on the actual input
        // manager, perform them.
        // If the keyboard visibility was changed after starting a new connection, we need to
        // perform that operation change after starting it.
        // If the keyboard visibility was changed before closing the connection, we need to
        // perform that operation before closing the connection so it doesn't no-op.
        if (startInput == true) {
            restartInputImmediately()
        }
        showKeyboard?.also(::setKeyboardVisibleImmediately)
        if (startInput == false) {
            restartInputImmediately()
        }

        if (DEBUG) log(TAG, "$DEBUG_CLASS.textInputCommandEventLoop.finished")
    }

    /** Immediately restart the IME connection, bypassing the [textInputCommandQueue]. */
    private fun restartInputImmediately() {
        if (DEBUG) log(TAG, "$DEBUG_CLASS.restartInputImmediately")
//        inputMethodManager.restartInput()
    }

    /** Immediately show or hide the keyboard, bypassing the [textInputCommandQueue]. */
    private fun setKeyboardVisibleImmediately(visible: Boolean) {
        if (DEBUG) log(TAG, "$DEBUG_CLASS.setKeyboardVisibleImmediately(visible=$visible)")
        if (visible) {
            val textConfig = currentInput?.textConfig
            val inputConnection = createInputConnection()
            InputMethodManager.showSoftKeyboard(textConfig, inputConnection)
        } else {
            InputMethodManager.hideSoftKeyboard()
        }
    }

    override fun updateState(oldValue: TextFieldValue?, newValue: TextFieldValue) {
        if (DEBUG) {
            log(TAG, "$DEBUG_CLASS.updateState called: $oldValue -> $newValue")
        }
        val internalOldValue = _tempCurrentInputSession?.toTextFieldValue()
        val textChanged = internalOldValue == null || internalOldValue.text != newValue.text
        val selectionChanged =
            textChanged || internalOldValue == null || internalOldValue.selection != newValue.selection
        _tempCurrentInputSession?.reset(newValue, null)
        currentInput?.let { input ->
            input.value = newValue
            _tempCursorPos = null
        }
    }

    fun onPreviewKeyEvent(event: KeyEvent): Boolean {
        return when (event.key) {
            Key.Enter -> handleEnterKey(event)
            Key.Backspace -> handleBackspace(event)
            else -> false
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

    private fun handleBackspace(event: KeyEvent): Boolean =
        // This prevents two characters from being removed for one hardware backspace key press.
        event.type == KeyEventType.KeyDown

    private fun sendEditCommand(vararg commands: EditCommand) {
        val commandList = commands.toList()
        _tempCurrentInputSession?.apply(commandList)
        currentInput?.let { input ->
            input.onEditCommand(commandList)
        }
    }

    private fun getCursorPos(): Int? {
        if (_tempCursorPos != null) {
            return _tempCursorPos
        }
        val selection = getState()?.selection
        if (selection != null && selection.start == selection.end) {
            return selection.start
        }
        return null
    }

    private fun imeActionRequired(): Boolean =
        currentImeOptions?.run {
            singleLine || (
                imeAction != ImeAction.None &&
                    imeAction != ImeAction.Default &&
                    !(imeAction == ImeAction.Search && _tempHardwareReturnKeyPressed)
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

    private fun createInputConnection() = object : InputConnection {

        /**
         * Inserts a character into the displayed text.
         * Add the character text to your class’s backing store at the index corresponding to the cursor and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614543-inserttext
         * @param text A string object representing the character typed on the system keyboard.
         */
        override fun insertText(text: String) {
            if (DEBUG) {
                log(TAG, "$DEBUG_CLASS.insertText text: ${text}")
            }
            if (text == "\n") {
                if (runImeActionIfRequired()) {
                    return
                }
            }
            getCursorPos()?.let {
                _tempCursorPos = it + text.length
            }
            sendEditCommand(CommitTextCommand(text, 1))
        }

        /**
         * Deletes a character from the displayed text.
         * Remove the character just before the cursor from your class’s backing store and redisplay the text.
         * https://developer.apple.com/documentation/uikit/uikeyinput/1614572-deletebackward
         */
        override fun deleteBackward() {
            if (DEBUG) {
                log(TAG, "$DEBUG_CLASS.deleteBackward")
            }
            // Before this function calls, iOS changes selection in setSelectedTextRange.
            // All needed characters should be allready selected, and we can just remove them.
            sendEditCommand(
                BackspaceCommand()
            )
        }

        override fun performEditorAction(editorAction: Int): Boolean {
            if (DEBUG) {
                log(TAG, "$DEBUG_CLASS.performEditorAction($editorAction)")
            }
            val imeAction = when (editorAction) {
                EnterKeyType.UNSPECIFIED.value -> ImeAction.Default
                EnterKeyType.DONE.value -> ImeAction.Done
                EnterKeyType.SEND.value -> ImeAction.Send
                EnterKeyType.SEARCH.value -> ImeAction.Search
                EnterKeyType.PREVIOUS.value -> ImeAction.Previous
                EnterKeyType.NEXT.value -> ImeAction.Next
                EnterKeyType.GO.value -> ImeAction.Go
                else -> {
                    log(TAG, "$DEBUG_CLASS.performEditorAction - IME sends unsupported Editor Action: $editorAction")
                    ImeAction.Default
                }
            }
            currentInput?.let { input ->
                input.onImeActionPerformed(imeAction)
            }
            return true
        }
    }
}

private class DelayExecutor(
    private val delayTime: Duration
) {

    fun execute(task: Runnable) {
        CoroutineScope(Dispatchers.Default).launch {
            // 延迟执行
            delay(delayTime.inWholeMilliseconds)
            withContext(Dispatchers.Main) {
                task.run()
            }
        }
    }
}

private data class CurrentInput(
    var value: TextFieldValue,
    val textConfig: OhosTextConfig,
    val onEditCommand: (List<EditCommand>) -> Unit,
    val onImeActionPerformed: (ImeAction) -> Unit
)

internal fun log(tag: String, message: String) {
    kLog("$tag $message")
}