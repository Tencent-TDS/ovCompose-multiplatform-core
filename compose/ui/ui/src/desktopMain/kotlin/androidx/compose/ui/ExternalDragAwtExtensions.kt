/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toPainter
import java.awt.Image
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.DataFlavor.selectBestTextFlavor
import java.awt.datatransfer.Transferable
import java.awt.image.BufferedImage
import java.io.File

@OptIn(ExperimentalComposeUiApi::class)
internal fun Transferable.dropData(): DropData? {
    val mimeTypes = transferDataFlavors.map { it.mimeType }
    val bestTextFlavor = selectBestTextFlavor(transferDataFlavors)

    return when {
        isDataFlavorSupported(DataFlavor.javaFileListFlavor) ->
            DropDataFilesListImpl(mimeTypes, this)

        isDataFlavorSupported(DataFlavor.imageFlavor) -> DropDataImageImpl(mimeTypes, this)

        bestTextFlavor != null -> DropDataTextImpl(mimeTypes, bestTextFlavor, this)

        else -> null
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DropDataFilesListImpl(
    override val mimeTypes: List<String>,
    private val transferable: Transferable
) : DropData.FilesList {
    override fun readFiles(): List<String> {
        val files = transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<*>
        return files.filterIsInstance<File>().map { it.toURI().toString() }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DropDataImageImpl(
    override val mimeTypes: List<String>,
    private val transferable: Transferable
) : DropData.Image {
    override fun readImage(): Painter {
        return (transferable.getTransferData(DataFlavor.imageFlavor) as Image).painter()
    }

    private fun Image.painter(): Painter {
        if (this is BufferedImage) {
            return this.toPainter()
        }
        val bufferedImage =
            BufferedImage(getWidth(null), getHeight(null), BufferedImage.TYPE_INT_ARGB)

        val g2 = bufferedImage.createGraphics()
        try {
            g2.drawImage(this, 0, 0, null)
        } finally {
            g2.dispose()
        }

        return bufferedImage.toPainter()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private class DropDataTextImpl(
    override val mimeTypes: List<String>,
    private val bestTextFlavor: DataFlavor,
    private val transferable: Transferable
) : DropData.Text {
    override val bestMimeType: String = bestTextFlavor.mimeType

    override fun readText(): String {
        val reader = bestTextFlavor.getReaderForText(transferable)
        return reader.readText()
    }
}