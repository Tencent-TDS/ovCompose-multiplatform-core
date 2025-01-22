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

package androidx.compose.ui.platform

import platform.UIKit.UIPasteboard

actual typealias NativeClipboard = platform.UIKit.UIPasteboard

internal class UiKitPlatformClipboard internal constructor() : Clipboard {
    override suspend fun getClipEntry(): ClipEntry? {
        if (nativeClipboard.items.isEmpty()) return null
        return ClipEntry().apply {
            plainText = nativeClipboard.string
        }
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        if (clipEntry == null) {
            nativeClipboard.items = emptyList<Map<String, Any>>()
        } else {
            nativeClipboard.string = clipEntry.plainText
        }
    }

    override val nativeClipboard: NativeClipboard
        get() = UIPasteboard.generalPasteboard
}

internal actual fun createPlatformClipboard(): Clipboard {
    return UiKitPlatformClipboard()
}


actual class ClipEntry internal constructor() {

    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")

    internal var plainText: String? = null

    fun getPlainText(): String? = plainText

    companion object {
        fun withPlainText(text: String): ClipEntry = ClipEntry().apply {
            plainText = text
        }
    }
}