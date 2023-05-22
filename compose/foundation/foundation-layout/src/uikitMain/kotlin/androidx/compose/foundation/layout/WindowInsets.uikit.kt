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
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.*
import androidx.compose.ui.unit.dp
import platform.UIKit.UIDeviceOrientation
import platform.UIKit.interfaceOrientation

private val EmptyInset = WindowInsets(0, 0, 0, 0)

/**
 * This insets represents iosSafeAreas.
 */
private val WindowInsets.Companion.iosSafeAreas: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets(
        top = LocalSafeAreaTopState.current.value.dp,
        bottom = LocalSafeAreaBottomState.current.value.dp,
        left = LocalSafeAreaLeftState.current.value.dp,
        right = LocalSafeAreaRightState.current.value.dp,
    )

/**
 * An insets type representing the window of a caption bar.
 * It is useless for iOS.
 */
val WindowInsets.Companion.captionBar get() = EmptyInset

/**
 * This insets represents the area that the
 * display cutout (e.g. for camera) is and important content should be excluded from.
 */
val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = when (LocalUIDeviceOrientationState.current.value) {
        UIDeviceOrientation.UIDeviceOrientationPortrait ->
            iosSafeAreas.only(WindowInsetsSides.Top)

        /** TODO: On PortraitUpsideDown orientation Compose draws like on previous orientation.
         *   But, we can override interfaceOrientation in UIViewController to control it after Kotlin 1.8.20
         */
        UIDeviceOrientation.UIDeviceOrientationPortraitUpsideDown ->
            EmptyInset

        UIDeviceOrientation.UIDeviceOrientationLandscapeLeft ->
            iosSafeAreas.only(WindowInsetsSides.Left)

        UIDeviceOrientation.UIDeviceOrientationLandscapeRight ->
            iosSafeAreas.only(WindowInsetsSides.Right)

        else -> iosSafeAreas.only(WindowInsetsSides.Top)
    }

/**
 * An insets type representing the window of an "input method",
 * For iOS IME representing the software keyboard.
 * TODO Animation doesn't work on iOS yet
 */
val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = WindowInsets(bottom = LocalKeyboardOverlapHeightState.current.value.dp)

/**
 * These insets represents the space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO("")

/**
 * These insets represent where system UI places navigation bars.
 * Interactive UI should avoid the navigation bars area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO("")

/**
 * TODO
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO("")

/**
 * This insets represents all system bars. Includes {@link #statusBars()}, {@link #captionBar()} as well as
 * {@link #navigationBars()}, but not {@link #ime()}.
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeAreas

/**
 * TODO
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

/**
 * TODO copy/paste doc
 * Returns the tappable element insets.
 * <p>The tappable element insets represent how much tappable elements <b>must at least</b> be
 * inset to remain both tappable and visually unobstructed by persistent system windows.
 * <p>This may be smaller than {@link #getSystemWindowInsets()} if the system window is
 * largely transparent and lets through simple taps (but not necessarily more complex gestures).
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = TODO()

/**
 * The insets for the curved areas in a waterfall display.
 * It is useless for iOS.
 */
val WindowInsets.Companion.waterfall: WindowInsets get() = EmptyInset

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [system bars][systemBars], [display cutout][displayCutout], and
 * [soft keyboard][ime].
 */
val WindowInsets.Companion.safeDrawing
    @Composable
    @NonRestartableComposable
    get() = systemBars.union(ime).union(displayCutout)

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [system gestures][systemGestures],
 * [mandatory system gestures][mandatorySystemGestures],
 * [rounded display areas][waterfall], and [tappable areas][tappableElement].
 */
val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = tappableElement.union(mandatorySystemGestures).union(systemGestures).union(waterfall)

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = safeDrawing.union(safeGestures)

