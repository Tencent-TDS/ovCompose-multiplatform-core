package androidx.compose.ui.platform

import androidx.compose.ui.text.input.CommitTextCommand
import androidx.compose.ui.text.input.DeleteSurroundingTextInCodePointsCommand
import androidx.compose.ui.text.input.EditCommand
import androidx.compose.ui.text.input.SetComposingTextCommand
import androidx.compose.ui.text.input.TextFieldValue
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.CompositionEvent
import org.w3c.dom.events.KeyboardEvent

internal interface DomInputStrategy {
    val htmlInput: HTMLTextAreaElement
    fun updateState(textFieldValue: TextFieldValue)
}

internal class CommonDomInputStrategy(
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

        htmlInput.addEventListener("blur", {evt ->
            // both accent dialogue and composition dialogue are lost when we switch windows
            // but can be restored later on if we are back
            editState = EditState.Default
        })

        htmlInput.addEventListener("keydown", {evt ->
            evt as KeyboardEvent
            console.log(evt.type, evt.timeStamp, evt.isComposing, evt)
            lastKeyboardEventIsDown = evt.key != "Dead"

            evt.preventDefault()

            if (editState is EditState.AccentDialogue) {
                return@addEventListener
            }

            if (evt.repeat) {
                editState = EditState.AccentDialogue
                return@addEventListener
            }

            if (evt.isComposing) {
                editState = EditState.CompositeDialogue
            }

            if (editState is EditState.CompositeDialogue) {
                return@addEventListener
            }

            if (evt.repeat) {
                editState = EditState.AccentDialogue
            }

            if (editState is EditState.AccentDialogue) {
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
                editState = EditState.CompositeDialogue
            }

        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent
            console.log("[binput] %c%s %c %s", "font-weight: bold", evt.inputType, "font-weight: normal", evt.data)

            if (editState is EditState.WaitingComposeActivity) return@addEventListener

            if (evt.inputType == "insertCompositionText") {
                editState = EditState.Default
                composeSender.sendEditCommand(SetComposingTextCommand(evt.data!!, 1))
            } else if (evt.inputType == "insertText") {
                evt.preventDefault()

                val editCommands = mutableListOf<EditCommand>()
                if (editState is EditState.AccentDialogue) {
                    editCommands.add(DeleteSurroundingTextInCodePointsCommand(1, 0))
                }
                editCommands.add(CommitTextCommand(evt.data!!, 1))

                editState = EditState.WaitingComposeActivity
                composeSender.sendEditCommand(editCommands)
            }

            evt.preventDefault()
        })

        htmlInput.addEventListener("compositionstart", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)
            editState = EditState.CompositeDialogue

            if (lastKeyboardEventIsDown) {
                composeSender.sendEditCommand(DeleteSurroundingTextInCodePointsCommand(1, 0))
            }
        })

        htmlInput.addEventListener("compositionend", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)
            lastActualCompositionTimestamp = evt.timeStamp.toInt()

            // in Safari we can rely on "insertFromComposition" input event but unfortunately it's not present in other browsers
            editState = EditState.WaitingComposeActivity
            composeSender.sendEditCommand(CommitTextCommand(evt.data, 1))
        })
    }
}


sealed interface EditState {
    data object Default : EditState
    data object WaitingComposeActivity : EditState
    data object CompositeDialogue: EditState
    data object AccentDialogue: EditState
}