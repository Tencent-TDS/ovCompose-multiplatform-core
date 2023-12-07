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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.coroutines.CoroutineContext
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue

fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController = ComposeUIViewController(
    configuration = ComposeUIViewControllerConfiguration().apply(configure),
    content = content,
)

@OptIn(InternalComposeApi::class)
@ExportObjCClass
internal class ComposeUIViewController(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    inner class ContainerImpl : ComposeContainer {
        override val rootViewController = this@ComposeUIViewController
        override val configuration = this@ComposeUIViewController.configuration

        override fun createLayer(
            currentComposeSceneContext: ComposeSceneContext,
            focusable: Boolean,
            sceneBridge: ComposeSceneMediator,
            coroutineDispatcher: CoroutineContext,
        ): ComposeSceneLayer {
            val layerMediator: ComposeSceneMediator =
                createComposeSceneBridge(focusable = focusable, transparentBackground = true) {
                    SingleLayerComposeScene(
                        coroutineContext = coroutineDispatcher,
                        composeSceneContext = currentComposeSceneContext,
                        density = sceneBridge.densityProvider(),
                        invalidate = sceneBridge::needRedraw,
                        layoutDirection = layoutDirection,
                    )
                }
            val layer = object : ComposeSceneLayer {
                override var density: Density = sceneBridge.densityProvider()
                override var layoutDirection: LayoutDirection = this@ContainerImpl.layoutDirection
                override var bounds: IntRect
                    get() = layerMediator.getViewBounds()
                    set(value) {
                        layerMediator.setLayout(
                            SceneLayout.Bounds(rect = value)
                        )
                    }
                override var scrimColor: Color? = null
                override var focusable: Boolean = focusable

                override fun close() {
                    layerMediator.dispose()
                }

                override fun setContent(content: @Composable () -> Unit) {
                    layerMediator.setContent(content)
                }

                override fun setKeyEventListener(
                    onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
                    onKeyEvent: ((KeyEvent) -> Boolean)?
                ) {
                    //todo
                }

                override fun setOutsidePointerEventListener(onOutsidePointerEvent: ((mainEvent: Boolean) -> Unit)?) {
                    //todo
                }
            }
            layerMediator.display(focusable = focusable, onDisplayed = {})
            layers.add(layer)
            return layer
        }

        private val layers: MutableList<ComposeSceneLayer> = mutableListOf()
        override val layoutDirection get() = getLayoutDirection()
        val composeSceneMediators: MutableList<ComposeSceneMediator> = mutableListOf()

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
        fun createRootComposeSceneBridge(): ComposeSceneMediator =
            if (configuration.platformLayers) {
                createSingleLayerComposeSceneBridge()
            } else {
                createMultiLayerComposeSceneBridge()
            }

        val keyboardVisibilityListener = object : KeyboardVisibilityListener {
            override fun keyboardWillShow(arg: NSNotification) = composeSceneMediators.forEach {
                it.keyboardVisibilityListener.keyboardWillShow(arg)
            }

            override fun keyboardWillHide(arg: NSNotification) = composeSceneMediators.forEach {
                it.keyboardVisibilityListener.keyboardWillHide(arg)
            }
        }
    }

    private val container = ContainerImpl()
    private var isInsideSwiftUI = false

    init {
        container.systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    /*
     * On iOS >= 13.0 interfaceOrientation will be deduced from [UIWindowScene] of [UIWindow]
     * to which our [RootUIViewController] is attached.
     * It's never UIInterfaceOrientationUnknown, if accessed after owning [UIWindow] was made key and visible:
     * https://developer.apple.com/documentation/uikit/uiwindow/1621601-makekeyandvisible?language=objc
     */
    private val currentInterfaceOrientation: InterfaceOrientation?
        get() {
            // Modern: https://developer.apple.com/documentation/uikit/uiwindowscene/3198088-interfaceorientation?language=objc
            // Deprecated: https://developer.apple.com/documentation/uikit/uiapplication/1623026-statusbarorientation?language=objc
            return if (available(OS.Ios to OSVersion(13))) {
                view.window?.windowScene?.interfaceOrientation?.let {
                    InterfaceOrientation.getByRawValue(it)
                }
            } else {
                InterfaceOrientation.getByRawValue(UIApplication.sharedApplication.statusBarOrientation)
            }
        }

    private val nativeKeyboardVisibilityListener = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun keyboardWillShow(arg: NSNotification) {
            container.keyboardVisibilityListener.keyboardWillShow(arg)
        }

        @Suppress("unused")
        @ObjCAction
        fun keyboardWillHide(arg: NSNotification) {
            container.keyboardVisibilityListener.keyboardWillHide(arg)
        }
    }

    @Suppress("unused")
    @ObjCAction
    fun viewSafeAreaInsetsDidChange() {
        // super.viewSafeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        container.composeSceneMediators.fastForEach {
            it.updateSafeArea()
        }
    }

    override fun loadView() {
        view = UIView().apply {
            backgroundColor = UIColor.whiteColor
            setClipsToBounds(true)
        } // rootView needs to interop with UIKit
    }

    override fun viewDidLoad() {
        super.viewDidLoad()

        PlistSanityCheck.performIfNeeded()

        configuration.delegate.viewDidLoad()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        container.systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        // UIKit possesses all required info for layout at this point
        currentInterfaceOrientation?.let {
            container.interfaceOrientationState.value = it
        }

        container.composeSceneMediators.forEach {
            it.updateLayout()
        }
    }

    override fun viewWillTransitionToSize(
        size: CValue<CGSize>,
        withTransitionCoordinator: UIViewControllerTransitionCoordinatorProtocol
    ) {
        super.viewWillTransitionToSize(size, withTransitionCoordinator)

        if (isInsideSwiftUI || presentingViewController != null) {
            // SwiftUI will do full layout and scene constraints update on each frame of orientation change animation
            // This logic is not needed

            // When presented modally, UIKit performs non-trivial hierarchy update durting orientation change,
            // its logic is not feasible to integrate into
            return
        }

        // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
        val isSameSizeTransition = view.frame.useContents {
            CGSizeEqualToSize(size, this.size.readValue())
        }
        if (isSameSizeTransition) {
            return
        }

        container.composeSceneMediators.forEach { sceneViewState ->
            sceneViewState.animateTransition(
                targetSize = size,
                coordinator = withTransitionCoordinator
            )
        }
        view.layoutIfNeeded()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        isInsideSwiftUI = checkIfInsideSwiftUI()
        attachComposeIfNeeded()
        configuration.delegate.viewWillAppear(animated)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)

        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = nativeKeyboardVisibilityListener,
            selector = NSSelectorFromString(nativeKeyboardVisibilityListener::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )

        configuration.delegate.viewDidAppear(animated)
    }

    // viewDidUnload() is deprecated and not called.
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = nativeKeyboardVisibilityListener,
            name = UIKeyboardWillHideNotification,
            `object` = null
        )

        configuration.delegate.viewWillDisappear(animated)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        dispose()

        dispatch_async(dispatch_get_main_queue()) {
            kotlin.native.internal.GC.collect()
        }

        configuration.delegate.viewDidDisappear(animated)
    }

    override fun didReceiveMemoryWarning() {
        println("didReceiveMemoryWarning")
        kotlin.native.internal.GC.collect()
        super.didReceiveMemoryWarning()
    }

    private fun dispose() {
        container.composeSceneMediators.fastForEachReversed {
            it.dispose()
        }
        container.composeSceneMediators.clear()
    }

    private fun attachComposeIfNeeded() {
        if (container.composeSceneMediators.isNotEmpty()) {
            return // already attached
        }
        val sceneViewState = container.createRootComposeSceneBridge()
        container.composeSceneMediators.add(sceneViewState)
        sceneViewState.display(
            focusable = true,
            onDisplayed = {
                sceneViewState.setContent(content)
            }
        )
        sceneViewState.setLayout(SceneLayout.UseConstraintsToFillContainer)
    }

}

private fun UIViewController.checkIfInsideSwiftUI(): Boolean {
    var parent = parentViewController

    while (parent != null) {
        val isUIHostingController = parent.`class`()?.let {
            val className = NSStringFromClass(it)
            // SwiftUI UIHostingController has mangled name depending on generic instantiation type,
            // It always contains UIHostingController substring though
            return className.contains("UIHostingController")
        } ?: false

        if (isUIHostingController) {
            return true
        }

        parent = parent.parentViewController
    }

    return false
}

private fun UIUserInterfaceStyle.asComposeSystemTheme(): SystemTheme {
    return when (this) {
        UIUserInterfaceStyle.UIUserInterfaceStyleLight -> SystemTheme.Light
        UIUserInterfaceStyle.UIUserInterfaceStyleDark -> SystemTheme.Dark
        else -> SystemTheme.Unknown
    }
}

private fun getLayoutDirection() =
    when (UIApplication.sharedApplication().userInterfaceLayoutDirection) {
        UIUserInterfaceLayoutDirection.UIUserInterfaceLayoutDirectionRightToLeft -> LayoutDirection.Rtl
        else -> LayoutDirection.Ltr
    }
