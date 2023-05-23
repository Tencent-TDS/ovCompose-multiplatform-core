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
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.uikit.*
import androidx.compose.ui.unit.dp

private val EmptyInset = WindowInsets(0, 0, 0, 0)

/**
 * This insets represents iOS SafeAreas.
 */
private val WindowInsets.Companion.iosSafeArea: WindowInsets
    @Composable
    @NonRestartableComposable
    @OptIn(InternalComposeApi::class)
    get() = WindowInsets(
        top = LocalSafeAreaState.current.value.top,
        bottom = LocalSafeAreaState.current.value.bottom,
        left = LocalSafeAreaState.current.value.left,
        right = LocalSafeAreaState.current.value.right,
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
    @OptIn(InternalComposeApi::class)
    get() = when (LocalInterfaceOrientationState.current.value) {
        InterfaceOrientation.Portrait -> iosSafeArea.only(WindowInsetsSides.Top)
        InterfaceOrientation.PortraitUpsideDown -> iosSafeArea.only(WindowInsetsSides.Bottom)
        InterfaceOrientation.LandscapeLeft -> iosSafeArea.only(WindowInsetsSides.Right)
        InterfaceOrientation.LandscapeRight -> iosSafeArea.only(WindowInsetsSides.Left)
    }

/**
 * An insets type representing the window of an "input method",
 * For iOS IME representing the software keyboard.
 * TODO Animation doesn't work on iOS yet
 */
val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @NonRestartableComposable
    @OptIn(InternalComposeApi::class)
    get() = WindowInsets(bottom = LocalKeyboardOverlapHeightState.current.value.dp)

/**
 * These insets represents the space where system gestures have priority over application gestures.
 */
val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeArea.only(WindowInsetsSides.Top + WindowInsetsSides.Bottom)

/**
 * These insets represent where system UI places navigation bars.
 * Interactive UI should avoid the navigation bars area.
 */
val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeArea.only(WindowInsetsSides.Bottom)

/**
 * This insets represents status bar
 */
val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @NonRestartableComposable
    @OptIn(InternalComposeApi::class)
    get() = when (LocalInterfaceOrientationState.current.value) {
        InterfaceOrientation.Portrait -> iosSafeArea.only(WindowInsetsSides.Top)
        else -> EmptyInset
    }

/**
 * This insets represents all system bars.
 * Includes [statusBars], [captionBar] as well as [navigationBars], but not [ime].
 */
val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeArea

/**
 * The systemGestures insets represent the area of a window where system gestures have
 * priority and may consume some or all touch input, e.g. due to the system bar
 * occupying it, or it being reserved for touch-only gestures.
 */
val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeArea.add(WindowInsets(16.dp, 16.dp, 16.dp, 16.dp))

/**
 * Returns the tappable element insets.
 */
val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    @NonRestartableComposable
    get() = iosSafeArea.only(WindowInsetsSides.Top)

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
 * including [systemGestures], [mandatorySystemGestures], [waterfall], and [tappableElement].
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

