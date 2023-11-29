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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import platform.UIKit.UIView
import platform.UIKit.UIViewController

internal interface RootViewControllerState<RootView, SceneView> { //TODO rename to Entrypoint or ViewController
    val rootView: RootView
    val densityProvider: DensityProvider
    val focusStack: FocusStack<UIView>
    val configuration: ComposeUIViewControllerConfiguration
    val windowInfo: WindowInfo //TODO maybe we need windowInfo on each scene
    val sceneStates:List<SceneState<SceneView>>
    fun createSceneViewState(): SceneState<SceneView>
    fun updateContainerSize(size: IntSize)
    fun updateLayout(sceneViewState: SceneState<SceneView>)
    @Composable
    fun EntrypointCompositionLocals(content: @Composable () -> Unit)
}
