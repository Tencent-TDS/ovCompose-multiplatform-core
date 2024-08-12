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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.viewinterop.NoOp
import androidx.compose.ui.viewinterop.UIKitInteropInteractionMode
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import androidx.compose.ui.viewinterop.UIKitView as UIKitView2
import androidx.compose.ui.viewinterop.UIKitViewController as UIKitViewController2

private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit =
    { rect -> this.view.setFrame(rect) }

@Deprecated(
    message = "Use androidx.compose.ui.viewinterop.UIKitView instead"
)
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = NoOp,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = NoOp,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true
) {
    require(onResize == DefaultViewResize) {
        "Custom onResize is not supported in deprecated API"
    }

    val backgroundColor by remember(background) { mutableStateOf(background.toUIColor()) }

    val interactionMode =
        if (interactive) {
            UIKitInteropInteractionMode.Cooperative()
        } else {
            null
        }

    val updateWithBackground = { it: T ->
        backgroundColor?.let { color ->
            it.backgroundColor = color
        }
        update(it)
    }

    UIKitView2(
        factory,
        modifier,
        update = updateWithBackground,
        onRelease,
        onReset = null,
        properties = UIKitInteropProperties(
            interactionMode = interactionMode,
            isNativeAccessibilityEnabled = accessibilityEnabled
        )
    )
}

@Deprecated(
    message = "Use androidx.compose.ui.viewinterop.UIKitViewController instead"
)
@Composable
fun <T : UIViewController> UIKitViewController(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = NoOp,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = NoOp,
    onResize: (viewController: T, rect: CValue<CGRect>) -> Unit = DefaultViewControllerResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true
) {
    require(onResize == DefaultViewControllerResize) {
        "Custom onResize is not supported in deprecated API"
    }

    val backgroundColor by remember(background) { mutableStateOf(background.toUIColor()) }

    val interactionMode =
        if (interactive) {
            UIKitInteropInteractionMode.Cooperative()
        } else {
            null
        }

    val updateWithBackground = { it: T ->
        backgroundColor?.let { color ->
            it.view.backgroundColor = color
        }
        update(it)
    }

    UIKitViewController2(
        factory,
        modifier,
        update = updateWithBackground,
        onRelease,
        onReset = null,
        properties = UIKitInteropProperties(
            interactionMode = interactionMode,
            isNativeAccessibilityEnabled = accessibilityEnabled
        )
    )
}