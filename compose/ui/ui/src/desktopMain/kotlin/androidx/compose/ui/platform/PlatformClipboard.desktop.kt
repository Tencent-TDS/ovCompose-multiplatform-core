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

import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException

actual typealias NativeClipboard = java.awt.datatransfer.Clipboard

internal class AwtPlatformClipboard internal constructor() : Clipboard {

    private val systemClipboard by lazy {
        try {
            Toolkit.getDefaultToolkit().systemClipboard
        } catch (e: java.awt.HeadlessException) {
            null
        }
    }

    override suspend fun getClipEntry(): ClipEntry? {
        val transferable = systemClipboard?.getContents(null) ?: return null
        val flavors = transferable.transferDataFlavors
        if (flavors?.size == 0) return null
        return ClipEntry(transferable)
    }

    override suspend fun setClipEntry(clipEntry: ClipEntry?) {
        systemClipboard?.setContents(
            clipEntry?.transferable ?: EmptyTransferable,
            null
        )
    }

    /**
     * Provides the [java.awt.datatransfer.Clipboard] instance.
     */
    override val nativeClipboard: NativeClipboard
        get() = systemClipboard ?: error("systemClipboard is not available in headless mode")
}

/**
 * A wrapper for [Transferable] instance which can be used to access or set the Clipboard content.
 */
actual class ClipEntry(val transferable: Transferable) {
    // TODO https://youtrack.jetbrains.com/issue/CMP-1260/ClipboardManager.-Implement-getClip-getClipMetadata-setClip
    actual val clipMetadata: ClipMetadata
        get() = TODO("ClipMetadata is not implemented. Consider using nativeClipboard")
}

private object EmptyTransferable : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> {
        return emptyArray()
    }

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean = false

    override fun getTransferData(flavor: DataFlavor?): Any {
        throw UnsupportedFlavorException(flavor)
    }
}

internal actual fun createPlatformClipboard(): Clipboard {
    return AwtPlatformClipboard()
}