/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.isUnspecified
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitView
import androidx.compose.ui.interop.UIKitViewController
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.UIKitInteropViewControllerHolder
import androidx.compose.ui.viewinterop.UIKitInteropViewHolder
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * Compose a [UIViewController] of class [T] into the UI hierarchy.
 *
 * @param factory The block creating the [T] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update A callback to be invoked every time the state it reads changes.
 * Invoked once initially and then every time the state it reads changes.
 * @param onRelease A callback invoked as a signal that the [T] has exited the
 * composition forever. Use it to release resources and stop jobs associated with [T].
 * @param onReset If not null, this callback is invoked when the [T] is
 * reused in the composition instead of being recreated. Use it to reset the state of [T] to
 * some blank state. If null, this composable can not be reused.
 * @property properties The properties configuring the behavior of [T]. Default value is
 * [UIKitInteropProperties.Default]
 * @property callbacks Callbacks related to events of [T] transitioning to specific states you want
 * to associate some workload with.
 *
 * @see UIKitInteropProperties
 * @see UIKitInteropCallbacks
 */
@Composable
fun <T : UIViewController> UIKitViewController(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = NoOp,
    onRelease: (T) -> Unit = NoOp,
    onReset: ((T) -> Unit)? = null,
    properties: UIKitInteropProperties = UIKitInteropProperties.Default,
    callbacks: UIKitInteropCallbacks<T>? = null,
) {
    val interopContainer = LocalInteropContainer.current
    val parentViewController = LocalUIViewController.current

    InteropView(
        factory = { compositeKeyHash ->
            UIKitInteropViewControllerHolder(
                factory,
                interopContainer,
                parentViewController,
                properties,
                callbacks,
                compositeKeyHash
            )
        },
        modifier,
        onReset,
        onRelease,
        update
    )
}