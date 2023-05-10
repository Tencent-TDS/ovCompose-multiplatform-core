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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.LocalSafeArea
import androidx.compose.ui._stateKeyboardHeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

/**
 * Indicates whether access to [WindowInsets] within the [content][ComposeView.setContent]
 * should consume the Android  [android.view.WindowInsets]. The default value is `true`, meaning
 * that access to [WindowInsets.Companion] will consume the Android WindowInsets.
 *
 * This property should be set prior to first composition.
 */
//var ComposeView.consumeWindowInsets: Boolean
//    get() = getTag(R.id.consume_window_insets_tag) as? Boolean ?: true
//    set(value) {
//        setTag(R.id.consume_window_insets_tag, value)
//    }

val WindowInsets.Companion.iosSafeAreaTop: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

val WindowInsets.Companion.iosSafeAreaBottom: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

val WindowInsets.Companion.iosSafeAreaLeft: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

val WindowInsets.Companion.iosSafeAreaRight: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

/**
 * This insets represents the iOS SafeArea
 */
val WindowInsets.Companion.iosSafeArea: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets(
        top = LocalSafeArea.current.top.dp,
        bottom = LocalSafeArea.current.bottom.dp,
        left = LocalSafeArea.current.left.dp,
        right = LocalSafeArea.current.right.dp,
    )

/**
 * On API level 23 (M) and above, the soft keyboard can be
 * detected and [ime] will update when it shows. On API 30 (R) and above, the [ime] insets will
 * animate synchronously with the actual IME animation.
 *
 * Developers should set `android:windowSoftInputMode="adjustResize"` in their
 * `AndroidManifest.xml` file and call `WindowCompat.setDecorFitsSystemWindows(window, false)`
 * in their [android.app.Activity.onCreate].
 */
val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets(bottom = _stateKeyboardHeight.value.dp)

@OptIn(ExperimentalLayoutApi::class)
val _imeMutableWindowInset = MutableWindowInsets(WindowInsets(0,0,0,0))

///**
// * The insets that include areas where gestures may be confused with other input,
// * including [system gestures][systemGestures],
// * [mandatory system gestures][mandatorySystemGestures],
// * [rounded display areas][waterfall], and [tappable areas][tappableElement].
// */
//val WindowInsets.Companion.safeGestures: WindowInsets
//    @Composable
//    @NonRestartableComposable
//    get() = WindowInsetsHolder.current().safeGestures
//
///**
// * The insets that include all areas that may be drawn over or have gesture confusion,
// * including everything in [safeDrawing] and [safeGestures].
// */
//val WindowInsets.Companion.safeContent: WindowInsets
//    @Composable
//    @NonRestartableComposable
//    get() = WindowInsetsHolder.current().safeContent
