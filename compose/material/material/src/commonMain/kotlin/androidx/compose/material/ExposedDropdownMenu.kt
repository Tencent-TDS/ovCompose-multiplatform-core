/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.compose.material

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.internal.JvmDefaultWithCompatibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * [Material Design Exposed Dropdown Menu](https://material.io/components/menus#exposed-dropdown-menu).
 *
 * Box for Exposed Dropdown Menu. Expected to contain [TextField] and
 * [ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as a content.
 *
 * An example of read-only Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material.samples.ExposedDropdownMenuSample
 *
 * An example of editable Exposed Dropdown Menu:
 *
 * @sample androidx.compose.material.samples.EditableExposedDropdownMenuSample
 *
 * @param expanded Whether Dropdown Menu should be expanded or not.
 * @param onExpandedChange Executes when the user clicks on the ExposedDropdownMenuBox.
 * @param modifier The modifier to apply to this layout
 * @param content The content to be displayed inside ExposedDropdownMenuBox.
 */
@ExperimentalMaterialApi
@Composable
expect fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
)

@JvmDefaultWithCompatibility
/**
 * Scope for [ExposedDropdownMenuBox].
 */
@ExperimentalMaterialApi
interface ExposedDropdownMenuBoxScope {
    /**
     * Modifier which should be applied to an [ExposedDropdownMenu]
     * placed inside the scope. It's responsible for
     * setting the width of the [ExposedDropdownMenu], which
     * will match the width of the [TextField]
     * (if [matchTextFieldWidth] is set to true).
     * Also it'll change the height of [ExposedDropdownMenu], so
     * it'll take the largest possible height to not overlap
     * the [TextField] and the software keyboard.
     *
     * @param matchTextFieldWidth Whether menu should match
     * the width of the text field to which it's attached.
     * If set to true the width will match the width
     * of the text field.
     */
    fun Modifier.exposedDropdownSize(
        matchTextFieldWidth: Boolean = true
    ): Modifier
}

/**
 * Popup which contains content for Exposed Dropdown Menu.
 * Should be used inside the content of [ExposedDropdownMenuBox].
 *
 * @param expanded Whether the menu is currently open and visible to the user
 * @param onDismissRequest Called when the user requests to dismiss the menu, such as by
 * tapping outside the menu's bounds
 * @param modifier The modifier to apply to this layout
 * @param scrollState a [ScrollState] to used by the menu's content for items vertical scrolling
 * @param content The content of the [ExposedDropdownMenu]
 */
@Composable
expect fun ExposedDropdownMenuBoxScope.ExposedDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState = rememberScrollState(),
    content: @Composable ColumnScope.() -> Unit
)

/**
 * Contains default values used by Exposed Dropdown Menu.
 */
@ExperimentalMaterialApi
expect object ExposedDropdownMenuDefaults {
    /**
     * Default trailing icon for Exposed Dropdown Menu.
     *
     * @param expanded Whether [ExposedDropdownMenuBoxScope.ExposedDropdownMenu]
     * is expanded or not. Affects the appearance of the icon.
     * @param onIconClick Called when the icon was clicked.
     */
    @ExperimentalMaterialApi
    @Composable
    fun TrailingIcon(
        expanded: Boolean,
        onIconClick: () -> Unit = {}
    )

    /**
     * Creates a [TextFieldColors] that represents the default input text, background and content
     * (including label, placeholder, leading and trailing icons) colors used in a [TextField].
     *
     * @param textColor Represents the color used for the input text of this text field.
     * @param disabledTextColor Represents the color used for the input text of this text field
     * when it's disabled.
     * @param backgroundColor Represents the background color for this text field.
     * @param cursorColor Represents the cursor color for this text field.
     * @param errorCursorColor Represents the cursor color for this text field
     * when it's in error state.
     * @param focusedIndicatorColor Represents the indicator color for this text field
     * when it's focused.
     * @param unfocusedIndicatorColor Represents the indicator color for this text field
     * when it's not focused.
     * @param disabledIndicatorColor Represents the indicator color for this text field
     * when it's disabled.
     * @param errorIndicatorColor Represents the indicator color for this text field
     * when it's in error state.
     * @param leadingIconColor Represents the leading icon color for this text field.
     * @param disabledLeadingIconColor Represents the leading icon color for this text field
     * when it's disabled.
     * @param errorLeadingIconColor Represents the leading icon color for this text field
     * when it's in error state.
     * @param trailingIconColor Represents the trailing icon color for this text field.
     * @param focusedTrailingIconColor Represents the trailing icon color for this text field
     * when it's focused.
     * @param disabledTrailingIconColor Represents the trailing icon color for this text field
     * when it's disabled.
     * @param errorTrailingIconColor Represents the trailing icon color for this text field
     * when it's in error state.
     * @param focusedLabelColor Represents the label color for this text field
     * when it's focused.
     * @param unfocusedLabelColor Represents the label color for this text field
     * when it's not focused.
     * @param disabledLabelColor Represents the label color for this text field
     * when it's disabled.
     * @param errorLabelColor Represents the label color for this text field
     * when it's in error state.
     * @param placeholderColor Represents the placeholder color for this text field.
     * @param disabledPlaceholderColor Represents the placeholder color for this text field
     * when it's disabled.
     */
    @Composable
    fun textFieldColors(
        textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
        backgroundColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.BackgroundOpacity),
        cursorColor: Color = MaterialTheme.colors.primary,
        errorCursorColor: Color = MaterialTheme.colors.error,
        focusedIndicatorColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedIndicatorColor: Color =
            MaterialTheme.colors.onSurface.copy(
                alpha = TextFieldDefaults.UnfocusedIndicatorLineOpacity
            ),
        disabledIndicatorColor: Color = unfocusedIndicatorColor.copy(alpha = ContentAlpha.disabled),
        errorIndicatorColor: Color = MaterialTheme.colors.error,
        leadingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
        disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled),
        errorLeadingIconColor: Color = leadingIconColor,
        trailingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
        focusedTrailingIconColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        disabledTrailingIconColor: Color = trailingIconColor.copy(alpha = ContentAlpha.disabled),
        errorTrailingIconColor: Color = MaterialTheme.colors.error,
        focusedLabelColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedLabelColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledLabelColor: Color = unfocusedLabelColor.copy(ContentAlpha.disabled),
        errorLabelColor: Color = MaterialTheme.colors.error,
        placeholderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledPlaceholderColor: Color = placeholderColor.copy(ContentAlpha.disabled)
    ): TextFieldColors

    /**
     * Creates a [TextFieldColors] that represents the default input text, background and content
     * (including label, placeholder, leading and trailing icons) colors used in an
     * [OutlinedTextField].
     *
     * @param textColor Represents the color used for the input text of this text field.
     * @param disabledTextColor Represents the color used for the input text of this text field
     * when it's disabled.
     * @param backgroundColor Represents the background color for this text field.
     * @param cursorColor Represents the cursor color for this text field.
     * @param errorCursorColor Represents the cursor color for this text field
     * when it's in error state.
     * @param focusedBorderColor Represents the border color for this text field
     * when it's focused.
     * @param unfocusedBorderColor Represents the border color for this text field
     * when it's not focused.
     * @param disabledBorderColor Represents the border color for this text field
     * when it's disabled.
     * @param errorBorderColor Represents the border color for this text field
     * when it's in error state.
     * @param leadingIconColor Represents the leading icon color for this text field.
     * @param disabledLeadingIconColor Represents the leading icon color for this text field
     * when it's disabled.
     * @param errorLeadingIconColor Represents the leading icon color for this text field
     * when it's in error state.
     * @param trailingIconColor Represents the trailing icon color for this text field.
     * @param focusedTrailingIconColor Represents the trailing icon color for this text field
     * when it's focused.
     * @param disabledTrailingIconColor Represents the trailing icon color for this text field
     * when it's disabled.
     * @param errorTrailingIconColor Represents the trailing icon color for this text field
     * when it's in error state.
     * @param focusedLabelColor Represents the label color for this text field
     * when it's focused.
     * @param unfocusedLabelColor Represents the label color for this text field
     * when it's not focused.
     * @param disabledLabelColor Represents the label color for this text field
     * when it's disabled.
     * @param errorLabelColor Represents the label color for this text field
     * when it's in error state.
     * @param placeholderColor Represents the placeholder color for this text field.
     * @param disabledPlaceholderColor Represents the placeholder color for this text field
     * when it's disabled.
     */
    @Composable
    fun outlinedTextFieldColors(
        textColor: Color = LocalContentColor.current.copy(LocalContentAlpha.current),
        disabledTextColor: Color = textColor.copy(ContentAlpha.disabled),
        backgroundColor: Color = Color.Transparent,
        cursorColor: Color = MaterialTheme.colors.primary,
        errorCursorColor: Color = MaterialTheme.colors.error,
        focusedBorderColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedBorderColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
        disabledBorderColor: Color = unfocusedBorderColor.copy(alpha = ContentAlpha.disabled),
        errorBorderColor: Color = MaterialTheme.colors.error,
        leadingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
        disabledLeadingIconColor: Color = leadingIconColor.copy(alpha = ContentAlpha.disabled),
        errorLeadingIconColor: Color = leadingIconColor,
        trailingIconColor: Color =
            MaterialTheme.colors.onSurface.copy(alpha = TextFieldDefaults.IconOpacity),
        focusedTrailingIconColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        disabledTrailingIconColor: Color = trailingIconColor.copy(alpha = ContentAlpha.disabled),
        errorTrailingIconColor: Color = MaterialTheme.colors.error,
        focusedLabelColor: Color =
            MaterialTheme.colors.primary.copy(alpha = ContentAlpha.high),
        unfocusedLabelColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledLabelColor: Color = unfocusedLabelColor.copy(ContentAlpha.disabled),
        errorLabelColor: Color = MaterialTheme.colors.error,
        placeholderColor: Color = MaterialTheme.colors.onSurface.copy(ContentAlpha.medium),
        disabledPlaceholderColor: Color = placeholderColor.copy(ContentAlpha.disabled)
    ): TextFieldColors
}
