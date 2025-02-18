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

package androidx.compose.foundation.text

import android.os.Build
import androidx.compose.foundation.R
import androidx.compose.foundation.contextmenu.ContextMenuScope
import androidx.compose.foundation.contextmenu.ContextMenuState
import androidx.compose.foundation.contextmenu.close
import androidx.compose.foundation.internal.hasText
import androidx.compose.foundation.text.input.internal.selection.TextFieldSelectionState
import androidx.compose.foundation.text.input.internal.selection.contextMenuBuilder
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.foundation.text.selection.contextMenuBuilder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch

@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    val coroutineScope = rememberCoroutineScope()
    val menuItemsAvailability = remember { mutableStateOf(MenuItemsAvailability.None) }

    androidx.compose.foundation.contextmenu.ContextMenuArea(
        state = state,
        onDismiss = { state.close() },
        contextMenuBuilderBlock = manager.contextMenuBuilder(state, menuItemsAvailability),
        enabled = manager.enabled,
        onOpenGesture = {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                menuItemsAvailability.value = manager.getContextMenuItemsAvailability()
            }
        },
        content = content,
    )
}

@Composable
internal actual fun ContextMenuArea(
    selectionState: TextFieldSelectionState,
    enabled: Boolean,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    val coroutineScope = rememberCoroutineScope()
    val menuItemsAvailability = remember { mutableStateOf(MenuItemsAvailability.None) }
    val menuBuilder =
        selectionState.contextMenuBuilder(
            state = state,
            itemsAvailability = menuItemsAvailability,
            onMenuItemClicked = { item ->
                coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                    when (item) {
                        TextContextMenuItems.Cut -> cut()
                        TextContextMenuItems.Copy -> copy(false)
                        TextContextMenuItems.Paste -> paste()
                        TextContextMenuItems.SelectAll -> selectAll()
                        TextContextMenuItems.Autofill -> autofill()
                    }
                }
            }
        )

    androidx.compose.foundation.contextmenu.ContextMenuArea(
        state = state,
        onDismiss = { state.close() },
        contextMenuBuilderBlock = menuBuilder,
        enabled = enabled,
        onOpenGesture = {
            coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
                menuItemsAvailability.value = selectionState.getContextMenuItemsAvailability()
            }
        },
        content = content,
    )
}

@Composable
internal actual fun ContextMenuArea(manager: SelectionManager, content: @Composable () -> Unit) {
    val state = remember { ContextMenuState() }
    androidx.compose.foundation.contextmenu.ContextMenuArea(
        state = state,
        onDismiss = { state.close() },
        contextMenuBuilderBlock = manager.contextMenuBuilder(state),
        content = content,
    )
}

/**
 * The default text context menu items.
 *
 * @param stringId The android [android.R.string] id for the label of this item
 */
internal enum class TextContextMenuItems(private val stringId: Int) {
    Cut(android.R.string.cut),
    Copy(android.R.string.copy),
    Paste(android.R.string.paste),
    SelectAll(android.R.string.selectAll),
    Autofill(
        if (Build.VERSION.SDK_INT <= 26) {
            R.string.autofill
        } else {
            android.R.string.autofill
        }
    );

    @ReadOnlyComposable @Composable fun resolvedString(): String = stringResource(stringId)
}

internal inline fun ContextMenuScope.TextItem(
    state: ContextMenuState,
    label: TextContextMenuItems,
    enabled: Boolean,
    crossinline operation: () -> Unit
) {
    // b/365619447 - instead of setting `enabled = enabled` in `item`,
    //  just remove the item from the menu.
    if (enabled) {
        item(label = { label.resolvedString() }) {
            operation()
            state.close()
        }
    }
}

internal suspend fun TextFieldSelectionState.getContextMenuItemsAvailability() =
    MenuItemsAvailability(
        canCopy = canCopy(),
        canPaste = canPaste(),
        canCut = canCut(),
        canSelectAll = canSelectAll(),
        canAutofill = canAutofill()
    )

internal suspend fun TextFieldSelectionManager.getContextMenuItemsAvailability():
    MenuItemsAvailability {
    val isPassword = visualTransformation is PasswordVisualTransformation
    val hasSelection = !value.selection.collapsed

    return MenuItemsAvailability(
        canCopy = hasSelection && !isPassword,
        canPaste = editable && clipboard?.getClipEntry()?.hasText() == true,
        canCut = hasSelection && editable && !isPassword,
        canSelectAll = value.selection.length != value.text.length,
        canAutofill = editable && value.selection.collapsed
    )
}

@JvmInline
internal value class MenuItemsAvailability private constructor(val value: Int) {
    constructor(
        canCopy: Boolean,
        canPaste: Boolean,
        canCut: Boolean,
        canSelectAll: Boolean,
        canAutofill: Boolean
    ) : this(
        (if (canCopy) COPY else 0) or
            (if (canPaste) PASTE else 0) or
            (if (canCut) CUT else 0) or
            (if (canSelectAll) SELECT_ALL else 0) or
            (if (canAutofill) AUTO_FILL else 0)
    )

    companion object {
        private const val COPY = 0b0001
        private const val PASTE = 0b0010
        private const val CUT = 0b0100
        private const val SELECT_ALL = 0b1000
        private const val AUTO_FILL = 0b10000
        private const val NONE = 0

        val None = MenuItemsAvailability(NONE)
    }

    val canCopy
        get() = value and COPY == COPY

    val canPaste
        get() = value and PASTE == PASTE

    val canCut
        get() = value and CUT == CUT

    val canSelectAll
        get() = value and SELECT_ALL == SELECT_ALL

    val canAutofill
        get() = value and AUTO_FILL == AUTO_FILL
}
