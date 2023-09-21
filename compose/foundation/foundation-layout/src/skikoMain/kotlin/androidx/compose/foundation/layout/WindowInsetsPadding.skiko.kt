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

package androidx.compose.foundation.layout

import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.debugInspectorInfo

actual fun Modifier.safeDrawingPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "safeDrawingPadding" }) { safeDrawing }

actual fun Modifier.safeGesturesPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "safeGesturesPadding" }) { safeGestures }

actual fun Modifier.safeContentPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "safeContentPadding" }) { safeContent }

actual fun Modifier.systemBarsPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "systemBarsPadding" }) { systemBars }

actual fun Modifier.displayCutoutPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "displayCutoutPadding" }) { displayCutout }

actual fun Modifier.statusBarsPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "statusBarsPadding" }) { statusBars }

actual fun Modifier.imePadding() =
    windowInsetsPadding(debugInspectorInfo { name = "imePadding" }) { ime }

actual fun Modifier.navigationBarsPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "navigationBarsPadding" }) { navigationBars }

actual fun Modifier.captionBarPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "captionBarPadding" }) { captionBar }

actual fun Modifier.waterfallPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "waterfallPadding" }) { waterfall }

actual fun Modifier.systemGesturesPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "systemGesturesPadding" }) { systemGestures }

actual fun Modifier.mandatorySystemGesturesPadding() =
    windowInsetsPadding(debugInspectorInfo { name = "mandatorySystemGesturesPadding" }) {
        mandatorySystemGestures
    }

@Suppress("NOTHING_TO_INLINE")
@Stable
private inline fun Modifier.windowInsetsPadding(
    noinline inspectorInfo: InspectorInfo.() -> Unit,
    crossinline insetsCalculation: WindowInsets.Companion.() -> WindowInsets
): Modifier = composed(inspectorInfo) {
    val insets = WindowInsets.Companion.insetsCalculation()
    InsetsPaddingModifier(insets)
}
