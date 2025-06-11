/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.PlatformInsetsHolder
import androidx.compose.ui.platform.PlatformInsetsValues

private val ZeroInsets = WindowInsets(0, 0, 0, 0)

/**
 * An insets type representing the window of a caption bar.
 * It is probably useless for OHOS.
 */
actual val WindowInsets.Companion.captionBar: WindowInsets
    get() = ZeroInsets

/**
 * This [WindowInsets] represents the area with the display cutout (e.g. for camera).
 */
actual val WindowInsets.Companion.displayCutout: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class, ExperimentalComposeUiApi::class)
    get() = PlatformInsetsHolder.current().displayCutout.windowInsets

/**
 * An insets type representing the window of an "input method",
 * for iOS IME representing the software keyboard.
 *
 * TODO: Animation doesn't work on OHOS yet
 */
actual val WindowInsets.Companion.ime: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class, ExperimentalComposeUiApi::class)
    get() = PlatformInsetsHolder.current().ime.windowInsets

/**
 * These insets represent the space where system gestures have priority over application gestures.
 */
actual val WindowInsets.Companion.mandatorySystemGestures: WindowInsets
    @Composable
    get() = ZeroInsets

/**
 * These insets represent where system UI places navigation bars.
 * Interactive UI should avoid the navigation bars area.
 */
actual val WindowInsets.Companion.navigationBars: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class, ExperimentalComposeUiApi::class)
    get() = PlatformInsetsHolder.current().navigationBars.windowInsets

/**
 * These insets represent status bar.
 */
actual val WindowInsets.Companion.statusBars: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class, ExperimentalComposeUiApi::class)
    get() = PlatformInsetsHolder.current().statusBars.windowInsets

/**
 * These insets represent all system bars.
 * Includes [statusBars], [captionBar] as well as [navigationBars], but not [ime].
 */
actual val WindowInsets.Companion.systemBars: WindowInsets
    @Composable
    get() = statusBars.union(navigationBars)

/**
 * The [systemGestures] insets represent the area of a window where system gestures have
 * priority and may consume some or all touch input, e.g. due to the system bar
 * occupying it, or it being reserved for touch-only gestures.
 */
actual val WindowInsets.Companion.systemGestures: WindowInsets
    @Composable
    @OptIn(InternalComposeApi::class, ExperimentalComposeUiApi::class)
    get() = PlatformInsetsHolder.current().systemGestures.windowInsets

/**
 * Returns the tappable element insets.
 */
actual val WindowInsets.Companion.tappableElement: WindowInsets
    @Composable
    get() = navigationBars

/**
 * The insets for the curved areas in a waterfall display.
 * It is useless for OHOS.
 */
actual val WindowInsets.Companion.waterfall: WindowInsets
    get() = ZeroInsets

/**
 * The insets that include areas where content may be covered by other drawn content.
 * This includes all [systemBars], [displayCutout], and [ime].
 */
actual val WindowInsets.Companion.safeDrawing: WindowInsets
    @Composable
    get() = systemBars.union(ime).union(displayCutout)

/**
 * The insets that include areas where gestures may be confused with other input,
 * including [systemGestures], [mandatorySystemGestures], [waterfall], and [tappableElement].
 */
actual val WindowInsets.Companion.safeGestures: WindowInsets
    @Composable
    get() = tappableElement.union(mandatorySystemGestures).union(systemGestures).union(waterfall)

/**
 * The insets that include all areas that may be drawn over or have gesture confusion,
 * including everything in [safeDrawing] and [safeGestures].
 */
actual val WindowInsets.Companion.safeContent: WindowInsets
    @Composable
    get() = safeDrawing.union(safeGestures)


@OptIn(ExperimentalComposeUiApi::class, InternalComposeApi::class)
private val PlatformInsetsValues.windowInsets: WindowInsets
    @Composable get() = WindowInsets(left = left, top = top, right = right, bottom = bottom)

