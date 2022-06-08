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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentComposer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.node.ComposeUiNode

class ClassWithProperty1 {
    internal /*or public*/ val someProperty = ValueOrInlineClass()

    @Composable
    fun composableFun() {
        Layout2(
            measurePolicy = { _, _ ->
                println(someProperty)
                layout(1, 1) {}
            }
        )
    }
}

@kotlin.jvm.JvmInline
value /*or inline*/ class ValueOrInlineClass(val innerValue: String = "")

/**
 * minimize [Layout] to easily understand bug
 */
@Composable
fun Layout2(
    measurePolicy: MeasurePolicy
) {
    currentComposer.startReusableNode()
    currentComposer.createNode {
        val node = ComposeUiNode.Constructor()
        node.measurePolicy = measurePolicy
        node
    }
    currentComposer.disableReusing()
    currentComposer.endNode()
}
