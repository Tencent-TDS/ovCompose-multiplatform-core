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

package androidx.compose.ui.platform

import androidx.compose.ui.arkui.messenger.Messenger
import androidx.compose.ui.arkui.messenger.onReceive
import androidx.compose.ui.arkui.messenger.send
import androidx.compose.ui.geometry.Rect
import org.jetbrains.skiko.IClipboardProxy
import org.jetbrains.skiko.PlatformProxy

/**
 * PlatformTextToolbar
 *
 * @author gavinbaoliu
 * @since 2025/3/12
 */
internal class PlatformTextToolbar(
    private val messenger: Messenger,
    private val clipboardProxy: IClipboardProxy
) : TextToolbar {

    private var onCopyRequested: (() -> Unit)? = null
    private var onPasteRequested: (() -> Unit)? = null
    private var onCutRequested: (() -> Unit)? = null
    private var onSelectAllRequested: (() -> Unit)? = null

    override var status: TextToolbarStatus = TextToolbarStatus.Hidden

    init {
        messenger.onReceive(RECEIVE_ON_CUT, ::onCut)
        messenger.onReceive(RECEIVE_ON_COPY, ::onCopy)
        messenger.onReceive(RECEIVE_ON_PASTE, ::onPaste)
        messenger.onReceive(RECEIVE_ON_SELECT_ALL, ::onSelectAll)
        messenger.onReceive(RECEIVE_ON_SHOWN, ::onShown)
        messenger.onReceive(RECEIVE_ON_HIDDEN, ::onHidden)
    }

    override fun showMenu(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ) {
        this.onCopyRequested = onCopyRequested
        this.onPasteRequested = onPasteRequested
        this.onCutRequested = onCutRequested
        this.onSelectAllRequested = onSelectAllRequested
        if (shouldShowMenu()) {
            PlatformProxy.clipboardProxy = clipboardProxy
            messenger.send(
                type = SEND_SHOW_MENU,
                message = makeMessage(rect, onCopyRequested, onPasteRequested, onCutRequested, onSelectAllRequested)
            )
        }
    }

    override fun hide() {
        messenger.send(type = SEND_HIDE)
    }

    private fun onCut() {
        onCutRequested?.invoke()
    }

    private fun onCopy() {
        onCopyRequested?.invoke()
    }

    private fun onPaste() {
        onPasteRequested?.invoke()
    }

    private fun onSelectAll() {
        onSelectAllRequested?.invoke()
    }

    private fun onShown() {
        status = TextToolbarStatus.Shown
    }

    private fun onHidden() {
        status = TextToolbarStatus.Hidden
        onCopyRequested = null
        onPasteRequested = null
        onCutRequested = null
        onSelectAllRequested = null
    }

    private fun makeMessage(
        rect: Rect,
        onCopyRequested: (() -> Unit)?,
        onPasteRequested: (() -> Unit)?,
        onCutRequested: (() -> Unit)?,
        onSelectAllRequested: (() -> Unit)?
    ): String = buildString {
        append(rect.left).append(",").append(rect.top).append(",").append(rect.right).append(",").append(rect.bottom)
        onCopyRequested?.let { append(",").append(SHOW_MENU_COPY) }
        onPasteRequested?.let { append(",").append(SHOW_MENU_PASTE) }
        onCutRequested?.let { append(",").append(SHOW_MENU_CUT) }
        onSelectAllRequested?.let { append(",").append(SHOW_MENU_SELECT_ALL) }
    }

    private fun shouldShowMenu(): Boolean =
        onCopyRequested != null || onPasteRequested != null || onCutRequested != null || onSelectAllRequested != null

    companion object {
        const val SHOW_MENU_COPY = "Copy"
        const val SHOW_MENU_PASTE = "Paste"
        const val SHOW_MENU_CUT = "Cut"
        const val SHOW_MENU_SELECT_ALL = "SelectAll"
        const val SEND_SHOW_MENU = "compose.ui.TextToolbar:showMenu"
        const val SEND_HIDE = "compose.ui.TextToolbar:hide"
        const val RECEIVE_ON_CUT = "compose.ui.TextToolbar:showMenu.onCut"
        const val RECEIVE_ON_COPY = "compose.ui.TextToolbar:showMenu.onCopy"
        const val RECEIVE_ON_PASTE = "compose.ui.TextToolbar:showMenu.onPaste"
        const val RECEIVE_ON_SELECT_ALL = "compose.ui.TextToolbar:showMenu.onSelectAll"
        const val RECEIVE_ON_SHOWN = "compose.ui.TextToolbar:onShown"
        const val RECEIVE_ON_HIDDEN = "compose.ui.TextToolbar:onHidden"
    }
}