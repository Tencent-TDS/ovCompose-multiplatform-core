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

package androidx.compose.ui.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf

// region Tencent Code
/**
 * Composition local for SafeArea of ComposeUIViewController
 */
@InternalComposeApi
val LocalSafeArea = compositionLocalOf { PlatformInsets.Zero }

/**
 * Composition local for layoutMargins of ComposeUIViewController
 */
@InternalComposeApi
val LocalLayoutMargins = compositionLocalOf { PlatformInsets.Zero }
// endregion
@OptIn(InternalComposeApi::class)
private object SafeAreaInsetsConfig : InsetsConfig {
    override val safeInsets: PlatformInsets
        @Composable get() = LocalSafeArea.current

    @Composable
    override fun excludeSafeInsets(content: @Composable () -> Unit) {
        val safeArea = LocalSafeArea.current
        val layoutMargins = LocalLayoutMargins.current
        CompositionLocalProvider(
            LocalSafeArea provides PlatformInsets(),
            LocalLayoutMargins provides layoutMargins.exclude(safeArea),
            content = content
        )
    }
}

internal actual var PlatformInsetsConfig: InsetsConfig = SafeAreaInsetsConfig
