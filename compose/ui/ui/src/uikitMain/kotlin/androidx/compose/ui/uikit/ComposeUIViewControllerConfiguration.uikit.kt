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

package androidx.compose.ui.uikit

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.AccessibilitySyncOptions
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake

/**
 * Configuration of ComposeUIViewController behavior.
 */
// region Tencent Code
abstract class ComposeConfiguration {
    /**
     * Control Compose behaviour on focus changed inside Compose.
     */
    var onFocusBehavior: OnFocusBehavior = OnFocusBehavior.FocusableAboveKeyboard

    @ExperimentalComposeApi
    var platformLayers: Boolean = true

    /**
     * @see [AccessibilitySyncOptions]
     *
     * By default, accessibility sync is enabled when required by accessibility services and debug
     * logging is disabled.
     */
    @ExperimentalComposeApi
    var accessibilitySyncOptions: AccessibilitySyncOptions =
        AccessibilitySyncOptions.WhenRequiredByAccessibilityServices(debugLogger = null)

    /**
     * Determines whether the Compose view should have an opaque background.
     * Warning: disabling opaque layer may affect performance.
     */
    @ExperimentalComposeApi
    var opaque: Boolean = true

    var renderBackend: RenderBackend = RenderBackend.UIView

    var canBecomeFirstResponder = false

    var boundsPositionCalculator: ((bounds: Rect) -> Rect)? = null
}

class ComposeUIViewControllerConfiguration : ComposeConfiguration() {
    /**
     * Reassign this property with an object implementing [ComposeUIViewControllerDelegate] to receive
     * UIViewController lifetime events.
     */
    var delegate: ComposeUIViewControllerDelegate = object : ComposeUIViewControllerDelegate {}

    /**
     * Compose containers decide whether to recursively clip all child views.
     */
    var clipChildren = true

    /* first-frame config */
    var firstFrameRenderConfig: FirstFrameRenderConfig? = null
}
// endregion

// region Tencent Code
interface FirstFrameRenderConfig {

    /* Flag whether the first frame has finished rendering; if ready, trigger the callback. */
    var firstFrameReady: Boolean

    /* First frame rendering callback */
    var firstFrameRenderCallback: (() -> Unit)?
}

/**
 * Configuration of ComposeUIView behavior.
 */
class ComposeUIViewConfiguration : ComposeConfiguration() {

    var delegate: ComposeUIViewDelegate = object : ComposeUIViewDelegate {}

    /* first-frame config */
    var firstFrameRenderConfig: FirstFrameRenderConfig? = null

    /**
     * The frame of ComposeUIView.
     * It takes precedence over [position] and [size].
     */
    var frame: CValue<CGRect>? = null

    /**
     * The position of ComposeUIView in its superview's coordinate system.
     * This value takes effect only when [frame] is null.
     */
    var position: DpOffset = DpOffset.Zero

    /**
     * The size of ComposeUIView in its superview's coordinate system.
     * This value takes effect only when [frame] is null.
     */
    var size: DpSize = DpSize.Zero

    /**
     * The constraints applied to Compose contents.
     * This value only takes effects when frame or size are not set, and
     * contents are measured without any constraints by default.
     */
    var maxSize: DpSize = DpSize(Dp.Infinity, Dp.Infinity)

    /**
     * Compose containers decide whether to recursively clip all child views.
     */
    var clipChildren = true

    internal fun effectFrame(): CValue<CGRect> {
        return when (val frame = this.frame) {
            null -> CGRectMake(
                position.x.value.toDouble(),
                position.y.value.toDouble(),
                size.width.value.toDouble(),
                size.height.value.toDouble()
            )
            else -> frame
        }
    }
}
// endregion

/**
 * Interface for UIViewController specific lifetime callbacks to allow injecting logic without overriding internal ComposeWindow.
 * All of those callbacks are invoked at the very end of overrided function implementation.
 */
interface ComposeUIViewControllerDelegate {
    fun viewDidLoad() = Unit
    fun viewWillAppear(animated: Boolean) = Unit
    fun viewDidAppear(animated: Boolean) = Unit
    fun viewWillDisappear(animated: Boolean) = Unit
    fun viewDidDisappear(animated: Boolean) = Unit
}

// region Tencent Code
interface ComposeUIViewDelegate : ComposeUIViewControllerDelegate {
    fun viewSizeDidChange(size: DpSize) = Unit
}
// endregion

sealed interface OnFocusBehavior {
    /**
     * The Compose view will stay on the current position.
     */
    object DoNothing : OnFocusBehavior

    /**
     * The Compose view will be panned in "y" coordinates.
     * A focusable element should be displayed above the keyboard.
     */
    object FocusableAboveKeyboard : OnFocusBehavior

    // TODO Better to control OnFocusBehavior with existing WindowInsets.
    // Definition: object: FocusableBetweenInsets(insets: WindowInsets) : OnFocusBehavior
    // Usage: onFocusBehavior = FocusableBetweenInsets(WindowInsets.ime.union(WindowInsets.systemBars))
}

/**
 * The CompositionLocal to provide renderType.
 */
val LocalDrawInSkia = staticCompositionLocalOf<Boolean> {
    false
}