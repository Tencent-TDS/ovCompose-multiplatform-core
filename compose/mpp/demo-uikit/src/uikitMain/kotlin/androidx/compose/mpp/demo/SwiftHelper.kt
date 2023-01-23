/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.mpp.demo

import cnames.structs.CGContext
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGBitmapContextCreate
import platform.CoreGraphics.CGBitmapContextGetBytesPerRow
import platform.CoreGraphics.CGBitmapContextGetData
import platform.CoreGraphics.CGColorSpaceCreateDeviceRGB
import platform.CoreGraphics.CGImageAlphaInfo
import platform.Metal.MTLCreateSystemDefaultDevice
import platform.Metal.MTLDeviceProtocol
import platform.Metal.MTLPixelFormatA1BGR5Unorm
import platform.Metal.MTLPixelFormatRGBA8Unorm
import platform.Metal.MTLRegionMake2D
import platform.Metal.MTLTextureDescriptor
import platform.Metal.MTLTextureProtocol
import platform.UIKit.UISwitch
import platform.UIKit.UIView
import platform.UIKit.UIViewController

class SwiftHelper {
    fun getViewController(mtlTexture: MTLTextureProtocol): UIViewController =
        getViewControllerWithCompose(UISwitch().toMtlTexture())
}

fun UIView.toMtlTexture(): MTLTextureProtocol {
    val device = MTLCreateSystemDefaultDevice() ?: error("Failed to create MTLCreateSystemDefaultDevice")
    return createMetalTexture(this, device) ?: error("fail to createMetalTexture, uiView: $this")
}

fun createMetalTexture(uiView: UIView, device: MTLDeviceProtocol): MTLTextureProtocol? {
    val (width, height) = uiView.bounds().useContents { size.width to size.height }
    val context: CPointer<CGContext>? = CGBitmapContextCreate(
        null,
        width.toULong(),
        height.toULong(),
        8,
        0,
        CGColorSpaceCreateDeviceRGB(),
        CGImageAlphaInfo.kCGImageAlphaPremultipliedLast.value
    );
    val data = CGBitmapContextGetData(context)
    if (data != null) {
        uiView.layer.renderInContext(context)
        val desc = MTLTextureDescriptor.texture2DDescriptorWithPixelFormat(
            pixelFormat = MTLPixelFormatRGBA8Unorm,
            width = width.toULong(),
            height = height.toULong(),
            mipmapped = false
        )
        val texture = device.newTextureWithDescriptor(desc)
        if (texture != null) {
            texture.replaceRegion(
                region = MTLRegionMake2D(0, 0, width.toULong(), height.toULong()),
                mipmapLevel = 0,
                withBytes = data,
                bytesPerRow = CGBitmapContextGetBytesPerRow(context)
            )
            return texture
        }
    }
    return null
}
