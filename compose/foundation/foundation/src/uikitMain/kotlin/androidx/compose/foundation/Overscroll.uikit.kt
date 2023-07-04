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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

private var isOptOut = false

/**
 * Opt out of the Cupertino overscroll behavior (rubber banding and spring effect).
 *
 * This method sets the `isOptOut` flag to true, indicating that the Cupertino overscroll behavior should be disabled.
 * By default, the Cupertino overscroll behavior is enabled on iOS.
 *
 * This method should be called before any @Composable function using this effect is executed
 * (sometime during app start up).
 */
@OptIn(ExperimentalFoundationApi::class)
fun optOutOfCupertinoOverscroll() {
    isOptOut = true
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal actual fun rememberOverscrollEffect(): OverscrollEffect =
    if (isOptOut) {
        NoOpOverscrollEffect
    } else {
        val density = LocalDensity.current.density
        val layoutDirection = LocalLayoutDirection.current

        remember(density, layoutDirection) {
            CupertinoOverscrollEffect(density, layoutDirection)
        }
    }