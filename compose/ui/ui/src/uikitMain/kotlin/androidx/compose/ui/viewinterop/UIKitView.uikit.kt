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
import androidx.compose.ui.Modifier
import platform.UIKit.UIView

/**
 * Compose [UIView] into the UI hierarchy.
 *
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update A callback to be invoked every time the state it reads changes.
 * Invoked once when it's assigned to the view, and then every time the state it reads changes.
 * @param onRelease A callback invoked as a signal that this view has exited the composition forever
 * Use it release resources and stop jobs associated with the view.
 */
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = NoOp,
    onRelease: (T) -> Unit = NoOp,
    onReset: ((T) -> Unit)? = null,
    properties: UIKitInteropProperties = UIKitInteropProperties.Default
) {
    val interopContainer = LocalInteropContainer.current

    InteropView(
        factory = { compositeKeyHash ->
            UIKitInteropViewHolder(
                factory = factory,
                interopContainer = interopContainer,
                properties = properties,
                compositeKeyHash = compositeKeyHash,
            )
        },
        modifier = modifier,
        onReset = onReset,
        onRelease = onRelease,
        update = update
    )
}