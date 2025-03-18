package androidx.compose.ui.platform

import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.KeyboardEvent

internal interface DomInputStrategy {
    val htmlInput: HTMLTextAreaElement
    fun updateState(textFieldValue: TextFieldValue)
}

internal class DefaultDomInputStrategy(
    override val htmlInput: HTMLTextAreaElement,
    private val composeSender: ComposeCommandCommunicator,
) : DomInputStrategy {

    private var lastActualCompositionTimestamp: Int = 0
    private var editState: EditState = EditState.Default


    init {
        initEvents()
    }

    override fun updateState(textFieldValue: TextFieldValue) {
        if (editState != EditState.WaitingComposeActivity) return

        htmlInput.value = textFieldValue.text
        htmlInput.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)

        editState = EditState.Default
    }

    private fun initEvents() {
        var lastKeyboardEventIsDown = false

        htmlInput.addEventListener("keydown", {evt ->
            evt as KeyboardEvent
            lastKeyboardEventIsDown = true

            evt.preventDefault()

            if (evt.isComposing) {
                editState = EditState.CompositeDialogueMode
            }

            if (editState is EditState.CompositeDialogueMode) {
                return@addEventListener
            }

            if (evt.repeat) {
                editState = EditState.AccentDialogueMode
            }

            if (editState is EditState.AccentDialogueMode) {
                return@addEventListener
            }

            if ((evt.timeStamp.toInt() - lastActualCompositionTimestamp) <= 0) {
                return@addEventListener
            }

            editState = EditState.WaitingComposeActivity

            val processed = composeSender.sendKeyboardEvent(evt)
            if (!processed) {
                editState = EditState.Default
            }

            lastActualCompositionTimestamp = 0
        })

        htmlInput.addEventListener("keyup", { evt ->
            lastKeyboardEventIsDown = false
            evt as KeyboardEvent
            if (evt.isComposing) {
                editState = EditState.CompositeDialogueMode
            }

        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent

            if (editState is EditState.WaitingComposeActivity) return@addEventListener

            if (evt.inputType == "insertCompositionText") {
                editState = EditState.Default
                composeSender.sendEditCommand(SetComposingTextCommand(evt.data!!, 1))
            } else if (evt.inputType == "insertText") {
                evt.preventDefault()
                editState = EditState.WaitingComposeActivity
                composeSender.sendEditCommand(CommitTextCommand(evt.data!!, 1))
            }

            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionstart", {evt ->
            evt as CompositionEvent
            editState = EditState.CompositeDialogueMode

            if (lastKeyboardEventIsDown) {
                composeSender.sendEditCommand(DeleteSurroundingTextInCodePointsCommand(1, 0))
            }
        })

        htmlInput.addEventListener("compositionupdate", {evt ->
            evt as CompositionEvent
        })

        htmlInput.addEventListener("compositionend", {evt ->
            evt as CompositionEvent
            lastActualCompositionTimestamp = evt.timeStamp.toInt()

            // in Safari we can rely on "insertFromComposition" input event but unfortunately it's not present in other browsers
            editState = EditState.WaitingComposeActivity
            composeSender.sendEditCommand(CommitTextCommand(evt.data!!, 1))
        })
    }
}


sealed interface EditState {
    data object Default : EditState
    data object WaitingComposeActivity : EditState
    data object CompositeDialogueMode: EditState
    data object AccentDialogueMode: EditState
}