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

@file:OptIn(ExperimentalFoundationApi::class)

package androidx.compose.foundation.text

import androidx.compose.foundation.ContextMenuArea
import androidx.compose.foundation.ContextMenuItem
import androidx.compose.foundation.ContextMenuState
import androidx.compose.foundation.DesktopPlatform
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.JPopupContextMenuRepresentation
import androidx.compose.foundation.LocalContextMenuRepresentation
import androidx.compose.foundation.text.TextContextMenu.Actions
import androidx.compose.foundation.text.selection.SelectionManager
import androidx.compose.foundation.text.selection.TextFieldSelectionManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.PasswordVisualTransformation
import java.awt.Component
import javax.swing.JMenuItem
import javax.swing.JPopupMenu
import kotlinx.coroutines.flow.collect

@Composable
internal actual fun ContextMenuArea(
    manager: TextFieldSelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    LocalTextContextMenu.current.Area(manager.actions, state, content)
}

@Composable
internal actual fun ContextMenuArea(
    manager: SelectionManager,
    content: @Composable () -> Unit
) {
    val state = remember { ContextMenuState() }
    if (DesktopPlatform.Current == DesktopPlatform.MacOS) {
        OpenMenuAdjuster(state) { manager.contextMenuOpenAdjustment(it) }
    }
    LocalTextContextMenu.current.Area(manager.actions, state, content)
}

@Composable
internal fun OpenMenuAdjuster(state: ContextMenuState, adjustAction: (Offset) -> Unit) {
    LaunchedEffect(state) {
        snapshotFlow { state.status }.collect { status ->
            if (status is ContextMenuState.Status.Open) {
                adjustAction(status.rect.center)
            }
        }
    }
}

private val TextFieldSelectionManager.actions get() = object : Actions {
    val isPassword get() = visualTransformation is PasswordVisualTransformation

    override val cut: (() -> Unit)? get() =
        if (!value.selection.collapsed && editable && !isPassword) {
            {
                cut()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val copy: (() -> Unit)? get() =
        if (!value.selection.collapsed && !isPassword) {
            {
                copy(false)
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val paste: (() -> Unit)? get() =
        if (editable && clipboardManager?.getText() != null) {
            {
                paste()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }

    override val selectAll: (() -> Unit)? get() =
        if (value.selection.length != value.text.length) {
            {
                selectAll()
                focusRequester?.requestFocus()
            }
        } else {
            null
        }
}

private val SelectionManager.actions get() = object : Actions {
    override var cut: (() -> Unit)? = null
    override var copy: (() -> Unit)? = { copy() }
    override var paste: (() -> Unit)? = null
    override var selectAll: (() -> Unit)? = null
}

/**
 * Composition local that keeps [TextContextMenu].
 */
@ExperimentalFoundationApi
val LocalTextContextMenu:
    ProvidableCompositionLocal<TextContextMenu> = staticCompositionLocalOf { TextContextMenu.Default }

/**
 * Describes how to show the text context menu for selectable texts and text fields.
 */
@ExperimentalFoundationApi
interface TextContextMenu {
    /**
     * Defines an area, that describes how to open and show text context menus.
     * Usually it uses [ContextMenuArea] as the implementation.
     *
     * @param actions Available actions that can be performed on the text for which we show the text context menu.
     * @param state [ContextMenuState] of menu controlled by this area.
     * @param content The content of the [ContextMenuArea].
     */
    @Composable
    fun Area(actions: Actions, state: ContextMenuState, content: @Composable () -> Unit)

    /**
     * Available actions that can be performed with text for which we show the text context menu.
     */
    @ExperimentalFoundationApi
    interface Actions {
        /**
         * Action for cutting the selected text to the clipboard. Null if there is no text to cut.
         */
        val cut: (() -> Unit)?

        /**
         * Action for copy the selected text to the clipboard. Null if there is no text to copy.
         */
        val copy: (() -> Unit)?

        /**
         * Action for pasting text from the clipboard. Null if there is no text in the clipboard.
         */
        val paste: (() -> Unit)?

        /**
         * Action for selecting the whole text. Null if the text is already selected.
         */
        val selectAll: (() -> Unit)?
    }

    companion object {
        /**
         * [TextContextMenu] that is used by default in Compose.
         */
        @ExperimentalFoundationApi
        val Default = object : TextContextMenu {
            @Composable
            override fun Area(actions: Actions, state: ContextMenuState, content: @Composable () -> Unit) {
                val localization = LocalLocalization.current
                val items = {
                    listOfNotNull(
                        actions.cut?.let {
                            ContextMenuItem(localization.cut, it)
                        },
                        actions.copy?.let {
                            ContextMenuItem(localization.copy, it)
                        },
                        actions.paste?.let {
                            ContextMenuItem(localization.paste, it)
                        },
                        actions.selectAll?.let {
                            ContextMenuItem(localization.selectAll, it)
                        },
                    )
                }

                ContextMenuArea(items, state, content = content)
            }
        }
    }
}

/**
 * [TextContextMenu] that uses [JPopupMenu] to show the text context menu.
 *
 * You can use it by overriding [TextContextMenu] on the top level of your application.
 *
 * @param owner The root component that owns a context menu. Usually it is [ComposeWindow] or [ComposePanel].
 * @param createMenu Describes how to create [JPopupMenu]. Use it if you want customization of the menu.
 * @param createItem Describes how to create a generic context menu item. Use it if you want customization.
 * of the menu items. It is called for items provided by [ContextMenuArea] in user code.
 * @param createCut Describes hot to create the menu item for Cut action.
 * @param createCopy Describes hot to create the menu item for Copy action.
 * @param createPaste Describes hot to create the menu item for Paste action.
 * @param createSelectAll Describes hot to create the menu item for Select All action.
 */
@ExperimentalFoundationApi
class JPopupTextMenu(
    private val owner: Component,
    private val createMenu: () -> JPopupMenu = { JPopupMenu() },
    private val createItem: (ContextMenuItem) -> Component = { item ->
        JMenuItem(item.label).apply {
            addActionListener { item.onClick() }
        }
    },
    private val createCut: (ContextMenuItem) -> Component = createItem,
    private val createCopy: (ContextMenuItem) -> Component = createItem,
    private val createPaste: (ContextMenuItem) -> Component = createItem,
    private val createSelectAll: (ContextMenuItem) -> Component = createItem
) : TextContextMenu {
    @Composable
    override fun Area(actions: Actions, state: ContextMenuState, content: @Composable () -> Unit) {
        val localization = LocalLocalization.current
        val items = {
            listOfNotNull(
                actions.cut?.let {
                    JPopupContextMenuItem(localization.cut, it, createCut)
                },
                actions.copy?.let {
                    JPopupContextMenuItem(localization.copy, it, createCopy)
                },
                actions.paste?.let {
                    JPopupContextMenuItem(localization.paste, it, createPaste)
                },
                actions.selectAll?.let {
                    JPopupContextMenuItem(localization.selectAll, it, createSelectAll)
                },
            )
        }

        CompositionLocalProvider(
            LocalContextMenuRepresentation provides JPopupContextMenuRepresentation(
                owner,
                createMenu
            ) { item ->
                when (item) {
                    is JPopupContextMenuItem -> item.createJMenuItem(item)
                    else -> createItem(item)
                }
            }
        ) {
            ContextMenuArea(items, state, content = content)
        }
    }
}

private class JPopupContextMenuItem(
    label: String,
    onClick: () -> Unit,
    val createJMenuItem: (ContextMenuItem) -> Component
) : ContextMenuItem(label, onClick)
