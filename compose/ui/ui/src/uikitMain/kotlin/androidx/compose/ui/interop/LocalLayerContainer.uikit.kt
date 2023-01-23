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

package androidx.compose.ui.interop

import androidx.compose.runtime.compositionLocalOf
import org.jetbrains.skia.GrBackendTexture
import org.jetbrains.skia.Image
import org.jetbrains.skiko.SkikoTouchEvent
import platform.UIKit.UIView

internal val LocalLayerContainer = compositionLocalOf<UIView> {
    error("CompositionLocal LayerContainer not provided")
}

internal val SkikoTouchEventHandler = compositionLocalOf<(Array<SkikoTouchEvent>) -> Unit> {
    error("SkikoTouchEventProvider not provided")
}

internal val SkikoBackendTextureToImage = compositionLocalOf<((GrBackendTexture)) -> Image?> {
    error("SkikoTouchEventProvider not provided")
}
