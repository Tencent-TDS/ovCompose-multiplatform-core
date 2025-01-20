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
import androidx.compose.ui.text.AnnotatedString
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

internal actual fun ClipEntry.readText(): String? {
    return try {
        transferable.getTransferData(DataFlavor.stringFlavor) as String?
    } catch (_: UnsupportedFlavorException) {
        null
    } catch (_: IllegalStateException) {
        null
    } catch (_: IOException) {
        null
    }
}

internal actual fun ClipEntry.readAnnotatedString(): AnnotatedString? {
    return readText()?.let { return AnnotatedString(it) }
}

internal actual fun AnnotatedString?.toClipEntry(): ClipEntry? {
    if (this == null) return null
    val transferable = StringSelection(this.text)
    return ClipEntry(transferable)
}

internal actual fun ClipEntry?.hasText(): Boolean {
    return try {
        (this?.transferable?.getTransferData(DataFlavor.stringFlavor) as? String)?.isNotEmpty() == true
    } catch (_: UnsupportedFlavorException) {
        false
    } catch (_: IllegalStateException) {
        false
    } catch (_: IOException) {
        false
    }
}