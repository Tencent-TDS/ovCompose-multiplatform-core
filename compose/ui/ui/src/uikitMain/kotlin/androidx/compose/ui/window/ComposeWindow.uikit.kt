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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.*
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.uikit.*
import androidx.compose.ui.unit.*
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGAffineTransformIdentity
import platform.CoreGraphics.CGAffineTransformInvert
import platform.CoreGraphics.CGFloat
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeEqualToSize
import platform.Foundation.*
import platform.QuartzCore.CADisplayLink
import platform.QuartzCore.CATransaction
import platform.QuartzCore.kCATransactionDisableActions
import platform.UIKit.*
import platform.darwin.NSObject
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.sel_registerName

fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController = object: ComposeViewState<UIViewController, UIView> {

    override val sceneStates: MutableList<SceneViewState<UIView>> by lazy {
        mutableListOf()
    }

    override val densityProvider by lazy {
        DensityProviderImpl(
            uiViewControllerProvider = { rootView },
            sceneStates = sceneStates,
        )
    }

    override val focusStack: FocusStack = FocusStackImpl()

    @OptIn(ExperimentalComposeApi::class)
    override fun createSceneViewState(): SceneViewState<UIView> =
        if (configuration.singleLayerComposeScene) {
            createSingleLayerSceneUIViewState(updateContainerSize = ::updateContainerSize)
        } else {
            createMultiLayerSceneUIViewState()
        }

    override val configuration by lazy {
        ComposeUIViewControllerConfiguration().apply(configure)
    }
    override val windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    override fun updateContainerSize(size: IntSize) {
        windowInfo.containerSize = size
    }

    override fun updateLayout(sceneViewState: SceneViewState<UIView>) {
        val scale = densityProvider().density
        val size = rootView.view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        updateContainerSize(size)
        sceneViewState.scene.density = densityProvider()
        sceneViewState.scene.size = size

        sceneViewState.needRedraw()
    }

    override fun doBoilerplate(sceneViewState: SceneViewState<UIView>, focusable: Boolean) {
        rootView.view.addSubview(sceneViewState.sceneView)
        sceneViewState.setConstraintsToFillView(rootView.view)
        updateLayout(sceneViewState)
        if (focusable) {
            focusStack.push(sceneViewState.sceneView)
        }
    }

    override fun setContentWithProvider(
        scene: ComposeScene,
        isReadyToShowContent: State<Boolean>,
        interopContext: UIKitInteropContext,
        content: @Composable () -> Unit
    ) {
        rootView.setContentWithProvider(scene, isReadyToShowContent, interopContext, content)
    }

    val keyboardVisibilityListener = KeyboardVisibilityListenerImpl(
        configuration = configuration,
        uiViewControllerProvider = { rootView },
        sceneStates = sceneStates,
        densityProvider = densityProvider,
    )

    override val rootView: ComposeRootUIViewController by lazy {
        ComposeRootUIViewController(
            configuration = configuration,
            content = content,
            createSceneViewState = ::createSceneViewState,
            updateLayout = ::updateLayout,
            doBoilerplate = ::doBoilerplate,
            keyboardVisibilityListener = keyboardVisibilityListener,
            sceneStates = sceneStates,
        )
    }
}.rootView

@OptIn(InternalComposeApi::class)
@ExportObjCClass
internal class ComposeRootUIViewController(
    val configuration: ComposeUIViewControllerConfiguration,
    val content: @Composable () -> Unit,
    val createSceneViewState: () -> SceneViewState<UIView>,
    val updateLayout: (sceneViewState: SceneViewState<UIView>) -> Unit,
    val doBoilerplate: (sceneViewState: SceneViewState<UIView>, focusable: Boolean) -> Unit,
    val keyboardVisibilityListener: KeyboardVisibilityListener,
    val sceneStates: MutableList<SceneViewState<UIView>>,
) : UIViewController(nibName = null, bundle = null) {

    private var isInsideSwiftUI = false
    private var safeArea by mutableStateOf(PlatformInsets())
    private var layoutMargins by mutableStateOf(PlatformInsets())

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    private var interfaceOrientation by mutableStateOf(
        InterfaceOrientation.Portrait
    )

    private val systemTheme = mutableStateOf(
        traitCollection.userInterfaceStyle.asComposeSystemTheme()
    )

    /*
     * On iOS >= 13.0 interfaceOrientation will be deduced from [UIWindowScene] of [UIWindow]
     * to which our [ComposeRootUIViewController] is attached.
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
        view.safeAreaInsets.useContents {
            safeArea = PlatformInsets(
                left = left.dp,
                top = top.dp,
                right = right.dp,
                bottom = bottom.dp,
            )
        }
        view.directionalLayoutMargins.useContents {
            layoutMargins = PlatformInsets(
                left = leading.dp, // TODO: Check RTL support
                top = top.dp,
                right = trailing.dp, // TODO: Check RTL support
                bottom = bottom.dp,
            )
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

        systemTheme.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun viewWillLayoutSubviews() {
        super.viewWillLayoutSubviews()

        // UIKit possesses all required info for layout at this point
        currentInterfaceOrientation?.let {
            interfaceOrientation = it
        }

        sceneStates.forEach {
            updateLayout(it)
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

        sceneStates.forEach { sceneViewState ->
            // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
            val isSameSizeTransition = view.frame.useContents {
                CGSizeEqualToSize(size, this.size.readValue())
            }
            if (isSameSizeTransition) {
                return
            }

            val startSnapshotView =
                sceneViewState.sceneView.snapshotViewAfterScreenUpdates(false) ?: return

            startSnapshotView.translatesAutoresizingMaskIntoConstraints = false
            view.addSubview(startSnapshotView)
            size.useContents {
                NSLayoutConstraint.activateConstraints(
                    listOf(
                        startSnapshotView.widthAnchor.constraintEqualToConstant(height),
                        startSnapshotView.heightAnchor.constraintEqualToConstant(width),
                        startSnapshotView.centerXAnchor.constraintEqualToAnchor(view.centerXAnchor),
                        startSnapshotView.centerYAnchor.constraintEqualToAnchor(view.centerYAnchor)
                    )
                )
            }

            sceneViewState.isForcedToPresentWithTransactionEveryFrame = true

            sceneViewState.setConstraintsToCenterInView(view, size)
            sceneViewState.sceneView.transform = withTransitionCoordinator.targetTransform

            view.layoutIfNeeded()

            withTransitionCoordinator.animateAlongsideTransition(
                animation = {
                    startSnapshotView.alpha = 0.0
                    startSnapshotView.transform =
                        CGAffineTransformInvert(withTransitionCoordinator.targetTransform)
                    sceneViewState.sceneView.transform = CGAffineTransformIdentity.readValue()
                },
                completion = {
                    startSnapshotView.removeFromSuperview()
                    sceneViewState.setConstraintsToFillView(view)
                    sceneViewState.isForcedToPresentWithTransactionEveryFrame = false
                }
            )
        }
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
        sceneStates.reversed().forEach {
            it.dispose()
        }
        sceneStates.clear()
    }

    private fun attachComposeIfNeeded() {
        if (sceneStates.isNotEmpty()) {
            return // already attached
        }
        val sceneViewState = createSceneViewState()
        setContentWithProvider(
            sceneViewState.scene,
            sceneViewState.isReadyToShowContent,
            sceneViewState.interopContext,
            content
        )
        doBoilerplate(sceneViewState, true)
        sceneStates.add(sceneViewState)
    }

    fun setContentWithProvider(
        scene: ComposeScene,
        isReadyToShowContent: State<Boolean>,
        interopContext: UIKitInteropContext,
        content: @Composable () -> Unit
    ) {
        scene.setContent {
            if (!isReadyToShowContent.value) return@setContent // TODO add link to issue with recomposition twice
            CompositionLocalProvider(
                LocalLayerContainer provides this.view,
                LocalUIViewController provides this,
                LocalKeyboardOverlapHeight provides keyboardVisibilityListener.keyboardOverlapHeightState.value,
                LocalSafeArea provides this.safeArea,
                LocalLayoutMargins provides this.layoutMargins,
                LocalInterfaceOrientation provides this.interfaceOrientation,
                LocalSystemTheme provides this.systemTheme.value,
                LocalUIKitInteropContext provides interopContext,
                content = content
            )
        }
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
