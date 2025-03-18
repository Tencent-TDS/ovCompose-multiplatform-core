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

internal abstract class StatefulDomInputStrategy : DomInputStrategy {
    protected var editState: EditState = EditState.Default

    override fun updateState(textFieldValue: TextFieldValue) {
        if (editState != EditState.WaitingComposeActivity) return

        println("updateState $editState ${textFieldValue.text}")

        htmlInput.value = textFieldValue.text
        htmlInput.setSelectionRange(textFieldValue.selection.start, textFieldValue.selection.end)

        editState = EditState.Default
    }
}

internal class DefaultDomInputStrategy(
    override val htmlInput: HTMLTextAreaElement,
    private val composeSender: ComposeCommandCommunicator,
) : StatefulDomInputStrategy() {

    init {
        initEvents()
    }

    private fun initEvents() {
        var lastKeydownWasProcessed = false

        htmlInput.addEventListener("keydown", {evt ->
            evt as KeyboardEvent
            console.log(evt.type, evt.timeStamp, evt.isComposing, evt)

            if (evt.repeat) {
                editState = EditState.AccentDialogueMode
                return@addEventListener
            }

            // this won't prevent other input-related events, in particular "compositionupdate"
            // even if it was triggered via key press
            evt.preventDefault()

            if (editState is EditState.AccentDialogueMode) {
                return@addEventListener
            }

            if (editState is EditState.CompositeDialogueMode) {
                return@addEventListener
            }

            editState = EditState.WaitingComposeActivity

            lastKeydownWasProcessed = composeSender.sendKeyboardEvent(evt)
            println("PROCESSED $lastKeydownWasProcessed")
            if (!lastKeydownWasProcessed) {
                editState = EditState.Default
            }
        })

        htmlInput.addEventListener("beforeinput", { evt ->
            evt as InputEvent
            console.log("[binput] %c%s %c %s", "font-weight: bold", evt.inputType, "font-weight: normal", evt.data)

            evt.preventDefault()
        })

        htmlInput.addEventListener("input", { evt ->
            evt as InputEvent
            console.log("[input] %c%s %c %s", "font-weight: bold", evt.inputType, "font-weight: normal", evt.data)
        })

        htmlInput.addEventListener("compositionstart", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)
            if (lastKeydownWasProcessed) {
                composeSender.sendEditCommand(DeleteSurroundingTextInCodePointsCommand(1, 0))
            }

            editState  = EditState.CompositeDialogueMode
        })

        htmlInput.addEventListener("compositionupdate", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)

            composeSender.sendEditCommand(SetComposingTextCommand(evt.data, 1))
        })

        htmlInput.addEventListener("compositionend", {evt ->
            evt as CompositionEvent
            console.log(evt.type, evt.timeStamp, evt.data)

            editState = EditState.WaitingComposeActivity
            composeSender.sendEditCommand(CommitTextCommand(evt.data, 1))
        })
    }
}


sealed interface EditState {
    data object Default : EditState
    data object WaitingComposeActivity : EditState
    data object CompositeDialogueMode: EditState
    data object AccentDialogueMode: EditState
}