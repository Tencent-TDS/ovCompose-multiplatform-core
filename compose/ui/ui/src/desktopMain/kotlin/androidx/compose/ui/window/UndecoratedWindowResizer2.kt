/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.Applier
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.ExplicitGroupsComposable
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.SkippableUpdater
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.layout.DefaultIntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasurable
import androidx.compose.ui.layout.IntrinsicMeasureScope
import androidx.compose.ui.layout.IntrinsicMinMax
import androidx.compose.ui.layout.IntrinsicWidthHeight
import androidx.compose.ui.layout.IntrinsicsMeasureScope
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.MeasureResult
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.Placeable.PlacementScope.Companion.place
import androidx.compose.ui.layout.materializerOf
import androidx.compose.ui.node.ComposeUiNode
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.util.fastMap
import kotlin.system.exitProcess

@UiComposable
@Composable
inline fun fun1(sam1: Sam1) {
    sam1.do1(listOf(), Constraints(0, 0, 1, 1))
}

fun interface Sam1 {
    fun do1(
        measurables: List<Measurable>,
        constraints: Constraints
    )
}

class UndecoratedWindowResizer2 {
    internal /*or public*/ val someProperty = ValueClass()

    @Composable
    fun composableFun() {
        fun1 { _, _ ->
            println(someProperty)
        }
        Layout2(
            measurePolicy = { _, _ ->
                println(someProperty)
                layout(1, 1) {}
            }
        )
        Layout(
            content = {},
            measurePolicy = { _, _ ->
//                println(someProperty)//Exception here
                layout(1, 1) {}
                exitProcess(0)
            }
        )
    }
}

@kotlin.jvm.JvmInline
value class ValueClass(val innerValue: String = "")

//@Suppress("ComposableLambdaParameterPosition")
//@UiComposable
@Composable
inline fun Layout2(
    measurePolicy: MeasurePolicy
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val viewConfiguration = LocalViewConfiguration.current
    ReusableComposeNode2<ComposeUiNode, Applier<Any>>(
        factory = ComposeUiNode.Constructor,
        update = {
            set(measurePolicy, ComposeUiNode.SetMeasurePolicy)
            set(density, ComposeUiNode.SetDensity)
            set(layoutDirection, ComposeUiNode.SetLayoutDirection)
            set(viewConfiguration, ComposeUiNode.SetViewConfiguration)
        }
    )
}

@Composable @ExplicitGroupsComposable
inline fun <T, reified E : Applier<*>> ReusableComposeNode2(
    noinline factory: () -> T,
    update: @DisallowComposableCalls Updater<T>.() -> Unit
) {
    currentComposer.startReusableNode()
    if (currentComposer.inserting) {
        currentComposer.createNode(factory)
    } else {
        currentComposer.useNode()
    }
    currentComposer.disableReusing()
    Updater<T>(currentComposer).update()
    currentComposer.endNode()
}
