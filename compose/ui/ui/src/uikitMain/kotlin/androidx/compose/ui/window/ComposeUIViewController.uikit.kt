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
import androidx.compose.runtime.CompositionContext
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
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfoImpl
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGRect
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

private val coroutineDispatcher = Dispatchers.Main

fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController = ComposeContainer(
    configuration = ComposeUIViewControllerConfiguration().apply(configure),
    content = content,
)

@OptIn(InternalComposeApi::class)
@ExportObjCClass
private class ComposeContainer(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : UIViewController(nibName = null, bundle = null) {

    private var isInsideSwiftUI = false
    private val composeSceneMediators: MutableList<ComposeSceneMediator> = mutableListOf()
    private val layers: MutableList<ComposeSceneLayer> = mutableListOf()
    private val layoutDirection get() = getLayoutDirection()

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)
    val focusStack: FocusStack<UIView> = FocusStackImpl()
    private val windowInfo = WindowInfoImpl()

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
            keyboardVisibilityListener.keyboardWillShow(arg)
        }

        @Suppress("unused")
        @ObjCAction
        fun keyboardWillHide(arg: NSNotification) {
            keyboardVisibilityListener.keyboardWillHide(arg)
        }
    }

    @Suppress("unused")
    @ObjCAction
    fun viewSafeAreaInsetsDidChange() {
        // super.viewSafeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        composeSceneMediators.fastForEach {
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
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        // UIKit possesses all required info for layout at this point
        currentInterfaceOrientation?.let {
            interfaceOrientationState.value = it
        }

        val window = checkNotNull(view.window) {
            "ComposeUIViewController.view should be attached to window"
        }
        val scale = window.screen.scale
        val size = window.frame.useContents<CGRect, IntSize> {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowInfo.containerSize = size
        composeSceneMediators.fastForEach {
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

        composeSceneMediators.fastForEach { sceneViewState ->
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

    private fun attachComposeIfNeeded() {
        setContent(content)
    }

    @Composable
    fun ProvideContainerCompositionLocals(content: @Composable () -> Unit) =
        CompositionLocalProvider(
            LocalUIViewController provides this,
            LocalLayerContainer provides view,
            LocalInterfaceOrientation provides interfaceOrientationState.value,
            LocalSystemTheme provides systemThemeState.value,
            content = content
        )

    @OptIn(ExperimentalComposeApi::class)
    fun setContent(content: @Composable () -> Unit) {
        if (composeSceneMediators.isNotEmpty()) {
            return // already attached
        }

        val mediator = if (configuration.platformLayers) {
            createSingleLayerComposeSceneMediator()
        } else {
            createMultiLayerComposeSceneMediator()
        }
        composeSceneMediators.add(mediator)
        mediator.display(
            focusable = true,
            onDisplayed = {
                mediator.setContent {
                    ProvideContainerCompositionLocals {
                        content()
                    }
                }
            }
        )
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
    }

    val keyboardVisibilityListener = object : KeyboardVisibilityListener {
        override fun keyboardWillShow(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardWillShow(arg)
        }

        override fun keyboardWillHide(arg: NSNotification) = composeSceneMediators.fastForEach {
            it.keyboardWillHide(arg)
        }
    }

    fun createLayer(
        currentComposeSceneContext: ComposeSceneContext,
        focusable: Boolean,
        sceneMediator: ComposeSceneMediator,
        coroutineDispatcher: CoroutineContext,
    ): ComposeSceneLayer {
        val mediator = ComposeSceneMediator(
            viewController = this,
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = true,
        ) {
            SingleLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = currentComposeSceneContext,
                density = sceneMediator.densityProvider(),
                invalidate = sceneMediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
        }
        val layer = object : ComposeSceneLayer {
            override var density: Density = sceneMediator.densityProvider()
            override var layoutDirection: LayoutDirection = this@ComposeContainer.layoutDirection
            override var bounds: IntRect
                get() = mediator.getViewBounds()
                set(value) {
                    mediator.setLayout(
                        SceneLayout.Bounds(rect = value)
                    )
                }
            override var scrimColor: Color? = null
            override var focusable: Boolean = focusable

            override fun close() {
                mediator.dispose()
                composeSceneMediators.remove(mediator)
                layers.remove(this)
            }

            override fun setContent(content: @Composable () -> Unit) {
                mediator.setContent {
                    ProvideContainerCompositionLocals {
                        content()
                    }
                }
            }

            override fun setKeyEventListener(
                onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
                onKeyEvent: ((KeyEvent) -> Boolean)?
            ) {
                //todo
            }

            override fun setOutsidePointerEventListener(
                onOutsidePointerEvent: ((mainEvent: Boolean) -> Unit)?
            ) {
                //todo
            }
        }

        mediator.display(focusable = focusable, onDisplayed = {})
        composeSceneMediators.add(mediator)
        layers.add(layer)
        return layer
    }

    private fun dispose() {
        composeSceneMediators.fastForEachReversed {
            it.dispose()
        }
        composeSceneMediators.clear()
    }

    private fun createSingleLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewController = this,
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false
        ) { mediator: ComposeSceneMediator ->
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
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
                composeSceneContext = context,
            )
        }

    private fun createMultiLayerComposeSceneMediator(): ComposeSceneMediator =
        ComposeSceneMediator(
            viewController = this,
            configuration = configuration,
            focusStack = focusStack,
            windowInfo = windowInfo,
            transparency = false,
        ) { mediator ->
            MultiLayerComposeScene(
                coroutineContext = coroutineDispatcher,
                composeSceneContext = object : ComposeSceneContext {
                    override val platformContext: PlatformContext get() = mediator.platformContext
                },
                density = mediator.densityProvider(),
                invalidate = mediator::onComposeSceneInvalidate,
                layoutDirection = layoutDirection,
            )
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
