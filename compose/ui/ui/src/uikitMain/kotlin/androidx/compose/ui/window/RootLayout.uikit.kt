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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.PlatformInsets
import androidx.compose.ui.platform.LocalLayoutMargins
import androidx.compose.ui.platform.LocalSafeArea
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

@OptIn(InternalComposeApi::class)
@Composable
internal actual fun platformPadding(): RootLayoutPadding {
    val density = LocalDensity.current
    val safeArea = LocalSafeArea.current
    return safeArea.toRootLayoutPadding(density)
}


@OptIn(InternalComposeApi::class)
@Composable
internal actual fun platformOwnerContent(content: @Composable () -> Unit) {
    val safeArea = LocalSafeArea.current
    val layoutMargins = LocalLayoutMargins.current
    CompositionLocalProvider(
        LocalSafeArea provides PlatformInsets(),
        LocalLayoutMargins provides layoutMargins.exclude(safeArea),
        content = content
    )
}

@OptIn(InternalComposeApi::class)
private fun PlatformInsets.toRootLayoutPadding(density: Density) = with(density) {
    RootLayoutPadding(
        left = left.roundToPx(),
        top = top.roundToPx(),
        right = right.roundToPx(),
        bottom = bottom.roundToPx()
    )
}

@OptIn(InternalComposeApi::class)
private fun PlatformInsets.exclude(insets: PlatformInsets) = PlatformInsets(
    left = (left - insets.left).coerceAtLeast(0.dp),
    top = (top - insets.top).coerceAtLeast(0.dp),
    right = (right - insets.right).coerceAtLeast(0.dp),
    bottom = (bottom - insets.bottom).coerceAtLeast(0.dp)
)
