/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.internal

import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.NativeClipboard
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal actual suspend fun ClipEntry.readText(): String? {
    if (!hasText()) return null

    return withContext(Dispatchers.IO) {
        val transferable = nativeClipEntry as? java.awt.datatransfer.Transferable
        try {
            transferable?.getTransferData(DataFlavor.stringFlavor) as? String
        } catch (_: IOException) {
            // the data is no longer available in the requested flavor
            null
        }
    }
}

// TODO: https://youtrack.jetbrains.com/issue/CMP-7543/Support-styled-text-in-PlatformClipboard-implementations
internal actual suspend fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    return readText()?.let { AnnotatedString(it) }
}

// TODO: https://youtrack.jetbrains.com/issue/CMP-7543/Support-styled-text-in-PlatformClipboard-implementations
internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    val transferable = StringSelection(this.text)
    return ClipEntry(transferable)
}

internal actual fun ClipEntry?.hasText(): Boolean {
    if (this == null) return false
    val transferable = nativeClipEntry as? java.awt.datatransfer.Transferable ?: return false
    return transferable.isDataFlavorSupported(DataFlavor.stringFlavor)
}

// Here we rely on the NativeClipboard directly instead of using ClipEntry,
// because getClipEntry is a suspend function, but in ContextMenu.desktop.kt we have older code
// expecting a synchronous execution.
// Note: the name can't be just `hasText` because NativeClipboard is a typealias to Any,
// so it would conflict with ClipEntry?.hasText declaration. Therefore, we need a unique name.
internal fun NativeClipboard.nativeClipboardHasText(): Boolean {
    val awtClipboard = this as? java.awt.datatransfer.Clipboard ?: return false
    return awtClipboard.isDataFlavorAvailable(DataFlavor.stringFlavor)
}