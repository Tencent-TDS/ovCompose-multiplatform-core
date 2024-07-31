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
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.rememberCompositionContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.materialize
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.UiApplier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

private val NoOp: InteropView.() -> Unit = {}

@Composable
private fun <T: InteropView> createInteropViewNodeFactory(
    factory: () -> T,
) {
    val compositeKeyHash = currentCompositeKeyHash
    val interopContainer = LocalInteropContainer.current
}

//@Composable
//@UiComposable
//fun <T: InteropView> InteropView(
//    factory: () -> T,
//    modifier: Modifier = Modifier,
//    update: (T) -> Unit = NoOp
//) {
//    val compositeKeyHash = currentCompositeKeyHash
//    val materializedModifier = currentComposer.materialize(modifier)
//
//    // TODO: ReusableComposeNode as in AndroidView
//    ComposeNode<LayoutNode, UiApplier>()(
//        factory =
//    )
//}