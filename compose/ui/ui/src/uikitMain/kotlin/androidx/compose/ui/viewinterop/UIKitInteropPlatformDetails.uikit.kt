/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color

internal class UIKitInteropPlatformDetails<T: InteropView>(
    val properties: UIKitInteropProperties,
    val listener: UIKitInteropListener<T>?
): InteropPlatformDetails {
    override fun platformModifier(holder: InteropViewHolder): Modifier =
        Modifier
            .pointerInteropFilter(
                isInteractive = properties.isInteractive,
                holder
            )
            .drawBehind {
                drawRect(
                    color = Color.Transparent,
                    blendMode = BlendMode.Clear
                )
            }
            .nativeAccessibility(
                isEnabled = properties.isNativeAccessibilityEnabled,
                interopWrappingView = holder.group as InteropWrappingView
            )
}