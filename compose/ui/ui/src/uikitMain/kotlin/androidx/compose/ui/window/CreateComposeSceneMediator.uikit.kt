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

package androidx.compose.ui.window

import androidx.compose.runtime.CompositionContext
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.Dispatchers

private val coroutineDispatcher = Dispatchers.Main

internal fun ComposeContainer.createSingleLayerComposeSceneMediator(): ComposeSceneMediator =
    ComposeSceneMediator(
        container = this,
        focusable = true,
        transparentBackground = false
    ) { mediator ->
        val context = object : ComposeSceneContext {
            override val platformContext: PlatformContext get() = mediator.platformContext
            override fun createPlatformLayer(
                density: Density,
                layoutDirection: LayoutDirection,
                focusable: Boolean,
                compositionContext: CompositionContext
            ): ComposeSceneLayer =
                createLayer(
                    currentComposeSceneContext = this,
                    focusable = focusable,
                    sceneMediator = mediator,
                    coroutineDispatcher = compositionContext.effectCoroutineContext
                )
        }

        SingleLayerComposeScene(
            coroutineContext = coroutineDispatcher,
            density = mediator.densityProvider(),
            invalidate = mediator::needRedraw,
            layoutDirection = layoutDirection,
            composeSceneContext = context,
        )
    }

internal fun ComposeContainer.createMultiLayerComposeSceneMediator(): ComposeSceneMediator =
    ComposeSceneMediator(
        container = this,
        focusable = true,
        transparentBackground = false
    ) { mediator ->
        MultiLayerComposeScene(
            coroutineContext = coroutineDispatcher,
            composeSceneContext = object : ComposeSceneContext {
                override val platformContext: PlatformContext get() = mediator.platformContext
            },
            density = mediator.densityProvider(),
            invalidate = mediator::needRedraw,
            layoutDirection = layoutDirection,
        )
    }
