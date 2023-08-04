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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.uikit.LocalSafeAreaState
import androidx.compose.ui.unit.IntOffset
import kotlin.math.max

@OptIn(InternalComposeApi::class)
internal actual val systemOffset: IntOffset
    @Composable
    get() {
        val density = LocalDensity.current
        val safeArea = LocalSafeAreaState.current.value
        with(density) {
            return IntOffset(
                x = max(safeArea.left.roundToPx(), safeArea.right.roundToPx()),
                y = max(safeArea.top.roundToPx(), safeArea.bottom.roundToPx())
            )
        }
    }
