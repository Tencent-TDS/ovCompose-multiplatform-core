/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.gestures.extention

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollableState

// region Tencent Code
/**
 * Default scrolling state listener
 *  help all scrollState to get current info about scroll
 * @constructor Create empty Default scrolling state listener
 */
interface IDefaultScrollingStateListener {
    fun startScrolling(scrollPriority: MutatePriority, scrollableState: ScrollableState)
    fun stopScrolling(scrollPriority: MutatePriority, scrollableState: ScrollableState)
}

internal var DefaultScrollingStateListener: IDefaultScrollingStateListener? = null
fun setDefaultScrollingStateListener(listener: IDefaultScrollingStateListener) {
    DefaultScrollingStateListener = listener
}
// endregion