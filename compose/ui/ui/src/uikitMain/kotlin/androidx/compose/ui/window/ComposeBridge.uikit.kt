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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import platform.Foundation.NSNotification
import platform.UIKit.UIApplication
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIView
import platform.UIKit.UIViewController

internal interface ComposeBridge {
    val rootViewController: UIViewController
    val layers: MutableList<ComposeSceneLayerBridge>
    val layoutDirection: LayoutDirection
    val focusStack: FocusStack<UIView>
    val configuration: ComposeUIViewControllerConfiguration

    @Composable
    fun ProvideRootCompositionLocals(content: @Composable () -> Unit)
}

@OptIn(InternalComposeApi::class)
internal fun createComposeBridge(
    configuration: ComposeUIViewControllerConfiguration,
    content: @Composable () -> Unit,
) = object : ComposeBridge {
    override val layers: MutableList<ComposeSceneLayerBridge> = mutableListOf()
    override val configuration = configuration
    override val layoutDirection get() = getLayoutDirection()
    private val composeSceneBridges: MutableList<ComposeSceneBridge> = mutableListOf()

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)

    override val focusStack: FocusStack<UIView> = FocusStackImpl()

    @Composable
    override fun ProvideRootCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIViewController provides rootViewController,
            LocalLayerContainer provides rootViewController.view,
            LocalInterfaceOrientation provides interfaceOrientationState.value,
            LocalSystemTheme provides systemThemeState.value,
            content = content
        )

    @OptIn(ExperimentalComposeApi::class)
    fun createRootComposeSceneBridge(): ComposeSceneBridge =
        if (configuration.platformLayers) {
            createSingleLayerComposeSceneBridge()
        } else {
            createMultiLayerComposeSceneBridge()
        }

    val keyboardVisibilityListener = object : KeyboardVisibilityListener {
        override fun keyboardWillShow(arg: NSNotification) = composeSceneBridges.forEach {
            it.keyboardVisibilityListener.keyboardWillShow(arg)
        }

        override fun keyboardWillHide(arg: NSNotification) = composeSceneBridges.forEach {
            it.keyboardVisibilityListener.keyboardWillHide(arg)
        }
    }

    override val rootViewController: RootUIViewController by lazy {
        RootUIViewController(
            configuration = configuration,
            content = content,
            createRootComposeBridge = ::createRootComposeSceneBridge,
            keyboardVisibilityListener = keyboardVisibilityListener,
            composeSceneBridges = composeSceneBridges,
            interfaceOrientationState = interfaceOrientationState,
            systemThemeState = systemThemeState,
            onViewSafeAreaInsetsDidChange = {
                composeSceneBridges.fastForEach {
                    it.updateSafeArea()
                }
            }
        )
    }

}

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
