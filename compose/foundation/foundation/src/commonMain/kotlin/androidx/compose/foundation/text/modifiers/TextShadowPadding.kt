/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import kotlin.math.ceil

/**
 * Whether text shadow padding is enabled for this node.
 *
 * On iOS (uikitMain), this reads [LocalTextShadowPaddingEnabled] CompositionLocal,
 * allowing per-component control via CompositionLocalProvider.
 *
 * On all other platforms, this always returns false — shadow padding is not needed
 * because they don't go through the bitmap → CALayer rendering pipeline.
 */
internal expect fun CompositionLocalConsumerModifierNode.isTextShadowPaddingEnabled(): Boolean

/**
 * Shadow padding info for expanding bitmap to avoid shadow clipping.
 * When shadow is None or blurRadius is 0, all paddings are 0 — no impact on existing behavior.
 */
internal data class ShadowPadding(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    val horizontalTotal get() = left + right
    val verticalTotal get() = top + bottom

    companion object {
        val None = ShadowPadding(0, 0, 0, 0)
    }
}

/**
 * Calculate shadow padding based on shadow blur radius and offset.
 *
 * When [isEnabled] is false, always returns [ShadowPadding.None] — zero impact on existing behavior.
 *
 * The blur sigma in Skia is approximately blurRadius * 0.57735 + 0.5,
 * and visible blur extent is roughly 3 * sigma ≈ 2 * blurRadius.
 * We use ceil(blurRadius * 2) as a safe expansion to fully contain the shadow.
 */
internal fun calculateShadowPadding(shadow: Shadow, isEnabled: Boolean): ShadowPadding {
    if (!isEnabled) return ShadowPadding.None
    if (shadow == Shadow.None || shadow.blurRadius <= 0f) return ShadowPadding.None

    val blurExpand = ceil(shadow.blurRadius * 2f).toInt()
    val offsetX = shadow.offset.x
    val offsetY = shadow.offset.y
    return ShadowPadding(
        left = (blurExpand + maxOf(0f, -offsetX)).toInt(),
        top = (blurExpand + maxOf(0f, -offsetY)).toInt(),
        right = (blurExpand + maxOf(0f, offsetX)).toInt(),
        bottom = (blurExpand + maxOf(0f, offsetY)).toInt()
    )
}
