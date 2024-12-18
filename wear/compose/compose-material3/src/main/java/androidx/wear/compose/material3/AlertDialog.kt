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

package androidx.wear.compose.material3

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListScope
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material3.AlertDialogDefaults.edgeButtonExtraTopPadding
import androidx.wear.compose.material3.PaddingDefaults.horizontalContentPadding
import androidx.wear.compose.material3.PaddingDefaults.verticalContentPadding
import androidx.wear.compose.material3.internal.Strings
import androidx.wear.compose.material3.internal.getString
import androidx.wear.compose.materialcore.screenHeightDp
import androidx.wear.compose.materialcore.screenWidthDp

/**
 * AlertDialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The AlertDialog is scrollable by default if the
 * content exceeds the viewport height.
 *
 * This overload has 2 [IconButton]s for confirmation and cancellation, placed horizontally at the
 * bottom of the dialog. It should be used when the user will be presented with a binary decision,
 * to either confirm or dismiss an action.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * Example of an [AlertDialog] with an icon, title and two buttons to confirm and dismiss:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithConfirmAndDismissSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping
 *   right (typically also called by the [dismissButton]).
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    dismissButton: @Composable RowScope.() -> Unit = {
        AlertDialogDefaults.DismissButton(onDismissRequest)
    },
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.confirmDismissContentPadding(),
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    Dialog(
        show = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        AlertDialogContent(
            confirmButton = confirmButton,
            title = title,
            dismissButton = dismissButton,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content
        )
    }
}

/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload doesn't have any dedicated slots for buttons. It has a content slot so that the
 * caller has flexibility in how to seek user input. In most cases, we recommend using other
 * AlertDialog variations with 2 confirm/dismiss buttons or a single confirmation button.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.contentPadding(),
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    Dialog(
        show = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        AlertDialogContent(
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content
        )
    }
}

/**
 * Dialogs provide important prompts in a user flow. They can require an action, communicate
 * information, or help users accomplish a task. The dialog is scrollable by default if the content
 * exceeds the viewport height.
 *
 * This overload has a single slot for a confirm [EdgeButton] at the bottom of the dialog. It should
 * be used when the user will be presented with a single acknowledgement.
 *
 * Where user input is not required, such as displaying a transient success or failure message, use
 * [ConfirmationDialog], [SuccessConfirmationDialog] or [FailureConfirmationDialog] instead.
 *
 * Example of an [AlertDialog] with an icon, title, text and bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithEdgeButtonSample
 *
 * Example of an [AlertDialog] with content groups and a bottom [EdgeButton]:
 *
 * @sample androidx.wear.compose.material3.samples.AlertDialogWithContentGroupsSample
 * @param visible A boolean indicating whether the dialog should be displayed.
 * @param onDismissRequest A lambda function to be called when the dialog is dismissed by swiping to
 *   the right or by other dismiss action.
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy e.g. by setting [visible] to false. It's
 *   recommended to use [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note
 *   that when using an [EdgeButton] which is not Medium size, the contentPadding parameters should
 *   be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text.By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Ensure there is
 *   enough space for the [EdgeButton], for example, using
 *   [AlertDialogDefaults.contentPaddingWithEdgeButton]
 * @param properties An optional [DialogProperties] object for configuring the dialog's behavior.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialog(
    visible: Boolean,
    onDismissRequest: () -> Unit,
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.contentPaddingWithEdgeButton(),
    properties: DialogProperties = DialogProperties(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    Dialog(
        show = visible,
        onDismissRequest = onDismissRequest,
        properties = properties,
    ) {
        AlertDialogContent(
            edgeButton = edgeButton,
            title = title,
            modifier = modifier,
            icon = icon,
            text = text,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content
        )
    }
}

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] with 2 buttons to
 * confirm or dismiss an action. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * @param confirmButton A slot for a [Button] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.ConfirmButton] in this slot with onClick callback.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param dismissButton A slot for a [Button] indicating negative sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.DismissButton] in this slot with onClick callback.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialogContent(
    confirmButton: @Composable RowScope.() -> Unit,
    title: @Composable () -> Unit,
    dismissButton: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.confirmDismissContentPadding(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    AlertDialogImpl(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        title = title,
        icon = icon,
        text = text,
        alertButtonsParams = AlertButtonsParams.ConfirmDismissButtons(confirmButton, dismissButton),
        content = content
    )
}

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] without any
 * dedicated slots for buttons. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents.
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialogContent(
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.contentPadding(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    AlertDialogImpl(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        title = title,
        icon = icon,
        text = text,
        alertButtonsParams = AlertButtonsParams.NoButtons,
        content = content
    )
}

/**
 * This [AlertDialogContent] overload provides the content for an [AlertDialog] with a single
 * [EdgeButton] to confirm an action. Prefer using [AlertDialog] directly, which provides built-in
 * animations and a streamlined API. This composable may be used to provide the content for an alert
 * dialog if custom animations are required.
 *
 * @param edgeButton Slot for an [EdgeButton] indicating positive sentiment. Clicking the button
 *   must remove the dialog from the composition hierarchy. It's recommended to use
 *   [AlertDialogDefaults.EdgeButton] in this slot with onClick callback. Note that when using an
 *   [EdgeButton] which is not Medium size, the contentPadding parameters should be specified.
 * @param title A slot for displaying the title of the dialog. Title should contain a summary of the
 *   dialog's purpose or content and should not exceed 3 lines of text. By default,
 *   [TextOverflow.Ellipsis] will be applied when text exceeds 3 lines.
 * @param modifier Modifier to be applied to the dialog content.
 * @param icon Optional slot for an icon to be shown at the top of the dialog.
 * @param text Optional slot for displaying the message of the dialog below the title. Should
 *   contain additional text that presents further details about the dialog's purpose if the title
 *   is insufficient.
 * @param verticalArrangement The vertical arrangement of the dialog's children. There is a default
 *   padding between icon, title, and text, which will be added to the spacing specified in this
 *   [verticalArrangement] parameter.
 * @param contentPadding The padding to apply around the entire dialog's contents. Ensure there is
 *   enough space for the [EdgeButton], for example, using
 *   [AlertDialogDefaults.contentPaddingWithEdgeButton]
 * @param content A slot for additional content, displayed within a scrollable [ScalingLazyColumn].
 */
@Composable
fun AlertDialogContent(
    edgeButton: (@Composable BoxScope.() -> Unit),
    title: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (() -> Unit)? = null,
    text: @Composable (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = AlertDialogDefaults.VerticalArrangement,
    contentPadding: PaddingValues = AlertDialogDefaults.contentPaddingWithEdgeButton(),
    content: (ScalingLazyListScope.() -> Unit)? = null
) {
    AlertDialogImpl(
        modifier = modifier,
        verticalArrangement = verticalArrangement,
        contentPadding = contentPadding,
        title = title,
        icon = icon,
        text = text,
        alertButtonsParams = AlertButtonsParams.EdgeButton(edgeButton),
        content = content
    )
}

/** Contains the default values used by [AlertDialog] */
object AlertDialogDefaults {
    /**
     * Default composable for the edge button in an [AlertDialog]. This is a medium sized
     * [EdgeButton]. Should be used with [AlertDialog] overload which contains a single edgeButton
     * slot.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [ButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    fun EdgeButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: ButtonColors = ButtonDefaults.buttonColors(),
        content: @Composable RowScope.() -> Unit = ConfirmIcon
    ) {
        EdgeButton(
            modifier = modifier,
            onClick = onClick,
            colors = colors,
            buttonSize = EdgeButtonSize.Medium,
            content = content
        )
    }

    /**
     * Default composable for the confirm button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [IconButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [ConfirmIcon].
     */
    @Composable
    fun ConfirmButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: IconButtonColors = IconButtonDefaults.filledIconButtonColors(),
        content: @Composable RowScope.() -> Unit = ConfirmIcon
    ) {
        val confirmWidth = 63.dp
        val confirmHeight = 54.dp

        val confirmShape = CircleShape

        FilledIconButton(
            onClick = onClick,
            modifier = modifier.rotate(-45f).size(confirmWidth, confirmHeight),
            colors = colors,
            shapes = IconButtonDefaults.shapes(confirmShape)
        ) {
            Row(
                modifier = Modifier.align(Alignment.Center).graphicsLayer { rotationZ = 45f },
                content = content
            )
        }
    }

    /**
     * Default composable for the dismiss button in an [AlertDialog]. Should be used with
     * [AlertDialog] overload which has 2 button slots to confirm or dismiss the action.
     *
     * @param onClick The callback to be invoked when the button is clicked.
     * @param modifier The [Modifier] to be applied to the button.
     * @param colors The [IconButtonColors] to be used for the button.
     * @param content The composable content of the button. Defaults to [DismissIcon].
     */
    @Composable
    fun DismissButton(
        onClick: () -> Unit,
        modifier: Modifier = Modifier,
        colors: IconButtonColors = IconButtonDefaults.filledTonalIconButtonColors(),
        content: @Composable RowScope.() -> Unit = DismissIcon
    ) {
        val dismissSize = 60.dp
        val dismissShape = MaterialTheme.shapes.medium

        Box(modifier = Modifier.size(dismissSize + cancelButtonPadding)) {
            FilledTonalIconButton(
                onClick = onClick,
                modifier = modifier.size(dismissSize).align(Alignment.BottomEnd),
                colors = colors,
                shapes = IconButtonDefaults.shapes(dismissShape)
            ) {
                Row(content = content)
            }
        }
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with confirm dismiss
     * buttons.
     */
    @Composable
    fun confirmDismissContentPadding(): PaddingValues {
        val verticalPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(horizontal = horizontalPadding, vertical = verticalPadding)
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with a bottom button.
     * If you need to configure custom paddings, consider using
     * [ScreenScaffoldDefaults.contentPaddingWithEdgeButton]
     */
    @Composable
    fun contentPaddingWithEdgeButton(
        edgeButtonSize: EdgeButtonSize = EdgeButtonSize.Medium
    ): PaddingValues {
        val topPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return ScreenScaffoldDefaults.contentPaddingWithEdgeButton(
            edgeButtonSize,
            top = topPadding,
            start = horizontalPadding,
            end = horizontalPadding,
            extraBottom = edgeButtonExtraTopPadding
        )
    }

    /**
     * The padding to apply around the content for the [AlertDialog] variation with a stack of
     * options and no buttons at the end.
     */
    @Composable
    fun contentPadding(): PaddingValues {
        val topPadding = verticalContentPadding()
        val horizontalPadding = horizontalContentPadding()
        return PaddingValues(
            top = topPadding,
            bottom = screenHeightDp().dp * noEdgeButtonBottomPaddingFraction,
            start = horizontalPadding,
            end = horizontalPadding,
        )
    }

    /**
     * Separator for the [AlertDialog]. Should be used inside [AlertDialog] content for splitting
     * groups of elements.
     */
    @Composable
    fun GroupSeparator() {
        Spacer(Modifier.height(8.dp))
    }

    /** Default vertical arrangement for an [AlertDialog]. */
    val VerticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(space = 4.dp, alignment = Alignment.CenterVertically)

    /** Default icon for the confirm button. */
    val ConfirmIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = getString(Strings.AlertDialogContentDescriptionConfirmButton),
            modifier = Modifier.size(36.dp).align(Alignment.CenterVertically)
        )
    }

    /** Default icon for the dismiss button. */
    val DismissIcon: @Composable RowScope.() -> Unit = {
        Icon(
            imageVector = Icons.Outlined.Close,
            contentDescription = getString(Strings.AlertDialogContentDescriptionDismissButton),
            modifier = Modifier.size(36.dp).align(Alignment.CenterVertically)
        )
    }

    /** The extra top padding to apply to the edge button. */
    internal val edgeButtonExtraTopPadding = 1.dp
    internal val noEdgeButtonBottomPaddingFraction = 0.3646f
    internal val cancelButtonPadding = 1.dp
}

@Composable
private fun AlertDialogImpl(
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical,
    contentPadding: PaddingValues,
    title: @Composable () -> Unit,
    icon: @Composable (() -> Unit)?,
    text: @Composable (() -> Unit)? = null,
    alertButtonsParams: AlertButtonsParams,
    content: (ScalingLazyListScope.() -> Unit)?
) {
    val state = rememberScalingLazyListState(initialCenterItemIndex = 0)
    ScreenScaffold(
        scrollState = state,
        modifier = modifier,
        edgeButton =
            if (alertButtonsParams is AlertButtonsParams.EdgeButton) {
                {
                    Box(
                        Modifier.padding(top = edgeButtonExtraTopPadding),
                        content = alertButtonsParams.edgeButton
                    )
                }
            } else null
    ) {
        ScalingLazyColumn(
            state = state,
            contentPadding = contentPadding,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = verticalArrangement,
            autoCentering = null,
            modifier = Modifier.fillMaxSize(),
        ) {
            if (icon != null) {
                item { IconAlert(icon) }
            }
            item { Title(title) }
            if (text != null) {
                item { TextMessage(text) }
            }
            if (content != null) {
                item { Spacer(Modifier.height(ContentTopSpacing)) }
                content()
            }

            when (alertButtonsParams) {
                is AlertButtonsParams.ConfirmDismissButtons ->
                    item { ConfirmDismissButtons(alertButtonsParams) }
                is AlertButtonsParams.EdgeButton ->
                    if (content == null) {
                        item { Spacer(Modifier.height(BottomButtonSpacing)) }
                    }
                is AlertButtonsParams.NoButtons -> Unit
            }
        }
    }
}

@Composable
private fun IconAlert(content: @Composable () -> Unit) {
    Column {
        content()
        Spacer(Modifier.height(AlertIconBottomSpacing))
    }
}

@Composable
private fun Title(content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthDp().dp * TitlePaddingFraction
    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.titleMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    maxLines = AlertTitleMaxLines,
                    overflow = TextOverflow.Ellipsis
                ),
            content = content
        )
    }
}

@Composable
private fun ConfirmDismissButtons(alertButtonsParams: AlertButtonsParams.ConfirmDismissButtons) {
    Column {
        Spacer(modifier = Modifier.height(ConfirmDismissButtonsTopSpacing))
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(6.dp))
            alertButtonsParams.dismissButton(this)
            Spacer(
                modifier =
                    Modifier.width(screenWidthDp().dp * ConfirmDismissBetweenButtonsPaddingFraction)
            )
            alertButtonsParams.confirmButton(this)
            Spacer(modifier = Modifier.width(2.dp))
        }
        Spacer(
            modifier =
                Modifier.height(screenHeightDp().dp * ConfirmDismissButtonsBottomSpacingFraction)
        )
    }
}

@Composable
private fun TextMessage(content: @Composable () -> Unit) {
    val horizontalPadding = screenWidthDp().dp * TextPaddingFraction
    Column(modifier = Modifier.padding(horizontal = horizontalPadding)) {
        Spacer(Modifier.height(AlertTextMessageTopSpacing))
        CompositionLocalProvider(
            LocalContentColor provides MaterialTheme.colorScheme.onBackground,
            LocalTextStyle provides MaterialTheme.typography.bodyMedium,
            LocalTextConfiguration provides
                TextConfiguration(
                    textAlign = TextAlign.Center,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = TextConfigurationDefaults.MaxLines
                ),
            content = content
        )
    }
}

private sealed interface AlertButtonsParams {
    object NoButtons : AlertButtonsParams

    class EdgeButton(
        val edgeButton: @Composable BoxScope.() -> Unit,
    ) : AlertButtonsParams

    class ConfirmDismissButtons(
        val confirmButton: @Composable RowScope.() -> Unit,
        val dismissButton: @Composable RowScope.() -> Unit
    ) : AlertButtonsParams
}

internal val AlertIconBottomSpacing = 4.dp
internal val AlertTextMessageTopSpacing = 8.dp
internal val ConfirmDismissButtonsTopSpacing = 12.dp
internal const val ConfirmDismissButtonsBottomSpacingFraction = 0.045f
internal const val AlertTitleMaxLines = 3

private val ContentTopSpacing = 8.dp
private val BottomButtonSpacing = 8.dp
private const val TextPaddingFraction = 0.0416f
private const val TitlePaddingFraction = 0.12f
private const val ConfirmDismissBetweenButtonsPaddingFraction = 0.03f
