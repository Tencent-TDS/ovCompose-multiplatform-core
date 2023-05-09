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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/**
 * Indicates whether access to [WindowInsets] within the [content][ComposeView.setContent]
 * should consume the Android  [android.view.WindowInsets]. The default value is `true`, meaning
 * that access to [WindowInsets.Companion] will consume the Android WindowInsets.
 *
 * This property should be set prior to first composition.
 */
var ComposeView.consumeWindowInsets: Boolean
    get() = getTag(R.id.consume_window_insets_tag) as? Boolean ?: true
    set(value) {
        setTag(R.id.consume_window_insets_tag, value)
    }

/**
 * For the [WindowInsetsCompat.Type.displayCutout]. This insets represents the area that the
 * display cutout (e.g. for camera) is and important content should be excluded from.
 */
val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().displayCutout

/**
 * For the [WindowInsetsCompat.Type.ime]. On API level 23 (M) and above, the soft keyboard can be
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
    get() = WindowInsetsHolder.current().ime

/**
 * For the [WindowInsetsCompat.Type.mandatorySystemGestures]. These insets represents the
 * space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().mandatorySystemGestures

/**
 * For the [WindowInsetsCompat.Type.navigationBars]. These insets represent where
 * system UI places navigation bars. Interactive UI should avoid the navigation bars
 * area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().navigationBars

/**
 * For the [WindowInsetsCompat.Type.statusBars].
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().statusBars

/**
 * For the [WindowInsetsCompat.Type.systemBars].
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemBars

/**
 * For the [WindowInsetsCompat.Type.systemGestures].
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemGestures

/**
 * For the [WindowInsetsCompat.Type.tappableElement].
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().tappableElement

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [system bars][systemBars], [display cutout][displayCutout], and
 * [soft keyboard][ime].
 */
val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeDrawing

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [system gestures][systemGestures],
 * [mandatory system gestures][mandatorySystemGestures],
 * [rounded display areas][waterfall], and [tappable areas][tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeGestures

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().safeContent

/**
 * The insets that the [WindowInsetsCompat.Type.captionBar] will consume if shown.
 * If it cannot be shown then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.captionBarIgnoringVisibility: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().captionBarIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.navigationBars] will consume if shown.
 * These insets represent where system UI places navigation bars. Interactive UI should
 * avoid the navigation bars area. If navigation bars cannot be shown, then this will be
 * empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.navigationBarsIgnoringVisibility: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().navigationBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.statusBars] will consume if shown.
 * If the status bar can never be shown, then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.statusBarsIgnoringVisibility: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().statusBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.systemBars] will consume if shown.
 *
 * If system bars can never be shown, then this will be empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.systemBarsIgnoringVisibility: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemBarsIgnoringVisibility

/**
 * The insets that [WindowInsetsCompat.Type.tappableElement] will consume if active.
 *
 * If there are never tappable elements then this is empty.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.tappableElementIgnoringVisibility: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().tappableElementIgnoringVisibility

/**
 * `true` when the [caption bar][captionBar] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isCaptionBarVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().captionBar.isVisible

/**
 * `true` when the [soft keyboard][ime] is being displayed, irrespective of
 * whether it intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isImeVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().ime.isVisible

/**
 * `true` when the [statusBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areStatusBarsVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().statusBars.isVisible

/**
 * `true` when the [navigationBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areNavigationBarsVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().navigationBars.isVisible

/**
 * `true` when the [systemBars] are being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.areSystemBarsVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().systemBars.isVisible
/**
 * `true` when the [tappableElement] is being displayed, irrespective of
 * whether they intersects with the Window.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.isTappableElementVisible: Boolean
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().tappableElement.isVisible

/**
 * The [WindowInsets] for the IME before the IME started animating in. The current
 * animated value is [WindowInsets.Companion.ime].
 *
 * This will be the same as [imeAnimationTarget] when there is no IME animation
 * in progress.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.imeAnimationSource: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().imeAnimationSource

/**
 * The [WindowInsets] for the IME when the animation completes, if it is allowed
 * to complete successfully. The current animated value is [WindowInsets.Companion.ime].
 *
 * This will be the same as [imeAnimationSource] when there is no IME animation
 * in progress.
 */
@ExperimentalLayoutApi
val WindowInsets.Companion.imeAnimationTarget: WindowInsets
    @Suppress("OPT_IN_MARKER_ON_WRONG_TARGET")
    @ExperimentalLayoutApi
    @Composable
    @NonRestartableComposable
    get() = WindowInsetsHolder.current().imeAnimationTarget
