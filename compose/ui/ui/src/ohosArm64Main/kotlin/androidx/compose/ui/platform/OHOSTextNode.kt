/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 Tencent. All rights reserved.
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

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.nativefoundation.OHOSNativeCanvas

/** OHOS平台的文本节点 */
class OHOSTextNode : PlatformTextNode {

    override fun needRedrawText(
        nativeCanvas: Canvas,
        paragraphHashKey: Int,
        width: Int,
        height: Int,
    ): Boolean {
        if (nativeCanvas is OHOSNativeCanvas) {
            return nativeCanvas.needRedrawImageWithHashCode(
                paragraphHashCode = paragraphHashKey,
                width = width,
                height = height
            )
        }
        return false
    }

    // width, height暂时保留
    override fun renderTextImage(
        imageBitmap: ImageBitmap?,
        width: Int,
        height: Int,
        paragraphHashCode: Int,
        nativeCanvas: Canvas
    ) {
        imageBitmap?.let {
            if (nativeCanvas is OHOSNativeCanvas) {
                nativeCanvas.drawParagraphImage(
                    image = it,
                    paragraphHashCode = paragraphHashCode,
                    width = width,
                    height = height
                )
            }
        }
    }

    override fun imageFromImageBitmap(
        nativeCanvas: Canvas,
        paragraphHashCode: Int,
        imageBitmap: ImageBitmap
    ): Long {
        if (nativeCanvas is OHOSNativeCanvas) {
            return nativeCanvas.imageFromImageBitmap(paragraphHashCode, imageBitmap)
        }
        throw RuntimeException("nativeCanvas is not OHOSNativeCanvas")
    }

    override fun asyncDrawIntoCanvas(
        nativeCanvas: Canvas,
        asyncTask: () -> Long,
        paragraphHashCode: Int,
        width: Int,
        height: Int
    ) {
        if (nativeCanvas is OHOSNativeCanvas) {
            nativeCanvas.asyncDrawIntoCanvas(asyncTask, paragraphHashCode, width, height)
        }
    }
}

