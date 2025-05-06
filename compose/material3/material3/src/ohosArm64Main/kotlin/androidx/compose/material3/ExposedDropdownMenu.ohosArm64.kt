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

package androidx.compose.material3

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * <a href="https://m3.material.io/components/menus/overview" class="external" target="_blank">Material Design Exposed Dropdown Menu</a>.
 *
 * Menus display a list of choices on a temporary surface. They appear when users interact with a
 * button, action, or other control.
 *
 * Exposed dropdown menus, sometimes also called "spinners" or "combo boxes", display the currently
 * selected item in a text field to which the menu is anchored. In some cases, it can accept and
 * display user input (whether or not it’s listed as a menu choice), in which case it may be used to
 * implement autocomplete.
 *
 * ![Exposed dropdown menu image](https://developer.android.com/images/reference/androidx/compose/material3/exposed-dropdown-menu.png)
 *
 * The [ExposedDropdownMenuBox] is expected to contain a [TextField] (or [OutlinedTextField]) and
 * [ExposedDropdownMenu][ExposedDropdownMenuBoxScope.ExposedDropdownMenu] as content. The
 * [menuAnchor][ExposedDropdownMenuBoxScope.menuAnchor] modifier should be passed to the text field.
 *
 * An example of a read-only Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.ExposedDropdownMenuSample
 *
 * An example of an editable Exposed Dropdown Menu:
 * @sample androidx.compose.material3.samples.EditableExposedDropdownMenuSample
 *
 * @param expanded whether the menu is expanded or not
 * @param onExpandedChange called when the exposed dropdown menu is clicked and the expansion state
 * changes.
 * @param modifier the [Modifier] to be applied to this ExposedDropdownMenuBox
 * @param content the content of this ExposedDropdownMenuBox, typically a [TextField] and an
 * [ExposedDropdownMenu][ExposedDropdownMenuBoxScope.ExposedDropdownMenu]. The
 * [menuAnchor][ExposedDropdownMenuBoxScope.menuAnchor] modifier should be passed to the text field
 * for proper menu behavior.
 */
@Composable
@ExperimentalMaterial3Api
actual fun ExposedDropdownMenuBox(
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier,
    content: @Composable ExposedDropdownMenuBoxScope.() -> Unit
) {
}

@Composable
internal actual fun ExposedDropdownMenuBoxScope.ExposedDropdownMenuDefaultImpl(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
    scrollState: ScrollState,
    content: @Composable ColumnScope.() -> Unit
) {
}