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

import Foundation
import UIKit
import SwiftUI
import shared

class SceneDelegate: UIResponder, UIWindowSceneDelegate {
    var window: UIWindow?

    func scene(_ scene: UIScene, willConnectTo session: UISceneSession, options connectionOptions: UIScene.ConnectionOptions) {
        if let windowScene = scene as? UIWindowScene {
            let window = UIWindow(windowScene: windowScene)
            window.rootViewController = SwiftHelper().getViewController()
            self.window = window
            window.makeKeyAndVisible()
        }
    }

}

extension UIView {
    func takeTextureSnapshot() -> MTLTexture? {
        createMTLTexture(self, MTLCreateSystemDefaultDevice()!)
    }
}

func createMTLTexture(_ uiView: UIView, _ device: MTLDevice) -> MTLTexture? {//todo remove
    let width = Int(uiView.bounds.width)
    let height = Int(uiView.bounds.height)

    if let context = CGContext(data: nil,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: 0,
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue),
       let data = context.data {
        uiView.layer.render(in: context)

        let desc = MTLTextureDescriptor.texture2DDescriptor(pixelFormat: .rgba8Unorm,
                width: width,
                height: height,
                mipmapped: false)
        if let texture = device.makeTexture(descriptor: desc) {
            texture.replace(region: MTLRegionMake2D(0, 0, width, height),
                    mipmapLevel: 0,
                    withBytes: data,
                    bytesPerRow: context.bytesPerRow)
            return texture
        }
    }
    return nil
}
