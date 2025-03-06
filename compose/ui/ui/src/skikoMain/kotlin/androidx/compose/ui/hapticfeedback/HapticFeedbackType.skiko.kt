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

package androidx.compose.ui.hapticfeedback

// TODO
internal actual object PlatformHapticFeedbackType {
    actual val Confirm = HapticFeedbackType(0)
    actual val ContextClick = HapticFeedbackType(1)
    actual val GestureEnd = HapticFeedbackType(2)
    actual val GestureThresholdActivate = HapticFeedbackType(3)
    actual val LongPress = HapticFeedbackType(4)
    actual val Reject = HapticFeedbackType(5)
    actual val SegmentFrequentTick = HapticFeedbackType(6)
    actual val SegmentTick = HapticFeedbackType(7)
    actual val TextHandleMove = HapticFeedbackType(8)
    actual val ToggleOn = HapticFeedbackType(9)
    actual val ToggleOff = HapticFeedbackType(10)
    actual val VirtualKey = HapticFeedbackType(11)
}
