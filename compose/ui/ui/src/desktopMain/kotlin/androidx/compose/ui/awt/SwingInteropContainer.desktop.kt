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

package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.TrackInteropModifierElement
import java.awt.Component
import java.awt.Container

internal val LocalSwingInteropContainer = staticCompositionLocalOf<SwingInteropContainer> {
    error("LocalSwingInteropContainer not provided")
}

internal class SwingInteropContainer(
    val container: Container,
    private val placeInteropAbove: Boolean
): InteropContainer<Component>() {
    private var interopComponentsCount = 0

    override fun addInteropView(nativeView: Component) {
        val nonInteropComponents = container.componentCount - interopComponentsCount
        val index = interopComponentsCount - countInteropComponentsBefore(nativeView)
        container.add(nativeView, if (placeInteropAbove) {
            index
        } else {
            index + nonInteropComponents
        })
        interopComponentsCount++
    }

    override fun removeInteropView(nativeView: Component) {
        interopComponentsCount--
        container.remove(nativeView)
    }

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalSwingInteropContainer provides this,
        ) {
            TrackInteropContainer(
                container = container,
                content = content
            )
        }
    }
}

internal fun Modifier.trackSwingInterop(
    component: Component
): Modifier = this then TrackInteropModifierElement(
    nativeView = component
)
