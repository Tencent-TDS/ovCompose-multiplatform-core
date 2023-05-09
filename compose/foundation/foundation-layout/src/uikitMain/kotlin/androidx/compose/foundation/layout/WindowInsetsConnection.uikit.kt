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

package androidx.compose.foundation.layout

import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Controls the soft keyboard as a nested scrolling on Android [R][Build.VERSION_CODES.R]
 * and later. This allows the user to drag the soft keyboard up and down.
 *
 * After scrolling, the IME will animate either to the fully shown or fully hidden position,
 * depending on the position and fling.
 *
 * @sample androidx.compose.foundation.layout.samples.windowInsetsNestedScrollDemo
 */
@ExperimentalLayoutApi
fun Modifier.imeNestedScroll(): Modifier {
    TODO()
//    return composed(
//        debugInspectorInfo {
//            name = "imeNestedScroll"
//        }
//    ) {
//        val nestedScrollConnection = rememberWindowInsetsConnection(
//            WindowInsetsHolder.current().ime,
//            WindowInsetsSides.Bottom
//        )
//        nestedScroll(nestedScrollConnection)
//    }
}

