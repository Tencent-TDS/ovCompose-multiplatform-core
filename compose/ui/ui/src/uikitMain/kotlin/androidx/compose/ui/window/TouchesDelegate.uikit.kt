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

package androidx.compose.ui.window

import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGPoint
import platform.UIKit.UIEvent
import platform.UIKit.UIView

internal enum class UITouchesEventPhase {
    BEGAN, MOVED, ENDED, CANCELLED
}

internal interface TouchesDelegate {
    fun pointInside(point: CValue<CGPoint>, event: UIEvent?): Boolean
    fun onTouchesEvent(view: UIView, event: UIEvent, phase: UITouchesEventPhase)
}
