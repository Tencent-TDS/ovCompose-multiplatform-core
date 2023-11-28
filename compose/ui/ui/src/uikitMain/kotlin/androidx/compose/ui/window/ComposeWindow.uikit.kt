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
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.interop.LocalUIKitInteropContext
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.interop.UIKitInteropContext
import androidx.compose.ui.platform.*
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.text.input.PlatformTextInputService
import androidx.compose.ui.uikit.*
import androidx.compose.ui.unit.*
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
import org.jetbrains.skiko.SkikoKeyboardEvent
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

private val uiContentSizeCategoryToFontScaleMap = mapOf(
    UIContentSizeCategoryExtraSmall to 0.8f,
    UIContentSizeCategorySmall to 0.85f,
    UIContentSizeCategoryMedium to 0.9f,
    UIContentSizeCategoryLarge to 1f, // default preference
    UIContentSizeCategoryExtraLarge to 1.1f,
    UIContentSizeCategoryExtraExtraLarge to 1.2f,
    UIContentSizeCategoryExtraExtraExtraLarge to 1.3f,

    // These values don't work well if they match scale shown by
    // Text Size control hint, because iOS uses non-linear scaling
    // calculated by UIFontMetrics, while Compose uses linear.
    UIContentSizeCategoryAccessibilityMedium to 1.4f, // 160% native
    UIContentSizeCategoryAccessibilityLarge to 1.5f, // 190% native
    UIContentSizeCategoryAccessibilityExtraLarge to 1.6f, // 235% native
    UIContentSizeCategoryAccessibilityExtraExtraLarge to 1.7f, // 275% native
    UIContentSizeCategoryAccessibilityExtraExtraExtraLarge to 1.8f, // 310% native

    // UIContentSizeCategoryUnspecified
)

fun ComposeUIViewController(content: @Composable () -> Unit): UIViewController =
    ComposeUIViewController(configure = {}, content = content)

fun ComposeUIViewController(
    configure: ComposeUIViewControllerConfiguration.() -> Unit = {},
    content: @Composable () -> Unit
): UIViewController = object: ComposeViewState<ComposeWindow, UIView> {
    override val densityProvider = {
        val contentSizeCategory =
            view.traitCollection.preferredContentSizeCategory
                ?: UIContentSizeCategoryUnspecified

        val fontScale: Float = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        Density(
            view.rootSceneViewState?.sceneView?.contentScaleFactor?.toFloat() ?: 1f,
            fontScale
        )
    }
    override val focusStack: FocusStack = FocusStackImpl()
    override fun createSceneViewState(): SceneViewState<UIView> = createSceneUIViewState()
    override val configuration by lazy {
        ComposeUIViewControllerConfiguration().apply(configure)
    }
    override val view: ComposeWindow by lazy {//todo UIViewController
        ComposeWindow(
            configuration = configuration,
            content = content,
            densityProvider = densityProvider,
            focusStack = focusStack,
            createSceneViewState = ::createSceneViewState,
        )
    }
}.view

@OptIn(InternalComposeApi::class)
@ExportObjCClass
internal class ComposeWindow(
    val configuration: ComposeUIViewControllerConfiguration,
    val content: @Composable () -> Unit,
    val densityProvider: () -> Density,
    val focusStack: FocusStack,
    val createSceneViewState: () -> SceneViewState<UIView>,
) : UIViewController(nibName = null, bundle = null) {

    fun doBoilerplate(sceneViewState: SceneViewState<UIView>, focusable: Boolean) {
        view.addSubview(sceneViewState.sceneView)
        sceneViewState.setConstraintsToFillView(view)
        updateLayout(sceneViewState)
        if (focusable) {
            focusStack.push(sceneViewState.sceneView)
        }
    }

    private var keyboardOverlapHeight by mutableStateOf(0f)
    private var isInsideSwiftUI = false
    private var safeArea by mutableStateOf(PlatformInsets())
    private var layoutMargins by mutableStateOf(PlatformInsets())

    //invisible view to track system keyboard animation
    private val keyboardAnimationView: UIView by lazy {
        UIView(CGRectMake(0.0, 0.0, 0.0, 0.0)).apply {
            hidden = true
        }
    }
    private var keyboardAnimationListener: CADisplayLink? = null

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
     * to which our [ComposeWindow] is attached.
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

    val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    var rootSceneViewState: SceneViewState<UIView>? = null

    private val keyboardVisibilityListener = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun keyboardWillShow(arg: NSNotification) {
            animateKeyboard(arg, true)

            val scene = rootSceneViewState?.scene ?: return
            val userInfo = arg.userInfo ?: return
            val keyboardInfo = userInfo[UIKeyboardFrameEndUserInfoKey] as NSValue
            val keyboardHeight = keyboardInfo.CGRectValue().useContents { size.height }
            if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
                val focusedRect = scene.focusManager.getFocusRect()?.toDpRect(densityProvider())

                if (focusedRect != null) {
                    updateViewBounds(
                        offsetY = calcFocusedLiftingY(focusedRect, keyboardHeight)
                    )
                }
            }
        }

        @Suppress("unused")
        @ObjCAction
        fun keyboardWillHide(arg: NSNotification) {
            animateKeyboard(arg, false)

            if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
                updateViewBounds(offsetY = 0.0)
            }
        }

        private fun animateKeyboard(arg: NSNotification, isShow: Boolean) {
            val userInfo = arg.userInfo!!

            //return actual keyboard height during animation
            fun getCurrentKeyboardHeight(): CGFloat {
                val layer = keyboardAnimationView.layer.presentationLayer() ?: return 0.0
                return layer.frame.useContents { origin.y }
            }

            //attach to root view if needed
            if (keyboardAnimationView.superview == null) {
                this@ComposeWindow.view.addSubview(keyboardAnimationView)
            }

            //cancel previous animation
            keyboardAnimationView.layer.removeAllAnimations()
            keyboardAnimationListener?.invalidate()

            //synchronize actual keyboard height with keyboardAnimationView without animation
            val current = getCurrentKeyboardHeight()
            CATransaction.begin()
            CATransaction.setValue(true, kCATransactionDisableActions)
            keyboardAnimationView.setFrame(CGRectMake(0.0, current, 0.0, 0.0))
            CATransaction.commit()

            //animation listener
            keyboardAnimationListener = CADisplayLink.displayLinkWithTarget(
                target = object : NSObject() {
                    val bottomIndent: CGFloat

                    init {
                        val screenHeight = UIScreen.mainScreen.bounds.useContents { size.height }
                        val composeViewBottomY = UIScreen.mainScreen.coordinateSpace.convertPoint(
                            point = CGPointMake(0.0, view.frame.useContents { size.height }),
                            fromCoordinateSpace = view.coordinateSpace
                        ).useContents { y }
                        bottomIndent = screenHeight - composeViewBottomY
                    }

                    @Suppress("unused")
                    @ObjCAction
                    fun animationDidUpdate() {
                        val currentHeight = getCurrentKeyboardHeight()
                        if (bottomIndent < currentHeight) {
                            keyboardOverlapHeight = (currentHeight - bottomIndent).toFloat()
                        }
                    }
                },
                selector = sel_registerName("animationDidUpdate")
            ).apply {
                addToRunLoop(NSRunLoop.mainRunLoop(), NSDefaultRunLoopMode)
            }

            //start system animation with duration
            val duration = userInfo[UIKeyboardAnimationDurationUserInfoKey] as? Double ?: 0.0
            val toValue: CGFloat = if (isShow) {
                val keyboardInfo = userInfo[UIKeyboardFrameEndUserInfoKey] as NSValue
                keyboardInfo.CGRectValue().useContents { size.height }
            } else {
                0.0
            }
            UIView.animateWithDuration(
                duration = duration,
                animations = {
                    //set final destination for animation
                    keyboardAnimationView.setFrame(CGRectMake(0.0, toValue, 0.0, 0.0))
                },
                completion = { isFinished ->
                    if (isFinished) {
                        keyboardAnimationListener?.invalidate()
                        keyboardAnimationListener = null
                        keyboardAnimationView.removeFromSuperview()
                    } else {
                        //animation was canceled by other animation
                    }
                }
            )
        }

        private fun calcFocusedLiftingY(focusedRect: DpRect, keyboardHeight: Double): Double {
            val viewHeight = rootSceneViewState?.sceneView?.frame?.useContents {
                size.height
            } ?: 0.0

            val hiddenPartOfFocusedElement: Double =
                keyboardHeight - viewHeight + focusedRect.bottom.value
            return if (hiddenPartOfFocusedElement > 0) {
                // If focused element is partially hidden by the keyboard, we need to lift it upper
                val focusedTopY = focusedRect.top.value
                val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
                if (isFocusedElementRemainsVisible) {
                    // We need to lift focused element to be fully visible
                    hiddenPartOfFocusedElement
                } else {
                    // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                    // Top edge of focused element should be visible. Same logic on Android.
                    maxOf(focusedTopY, 0f).toDouble()
                }
            } else {
                // Focused element is not hidden by the keyboard.
                0.0
            }
        }

        private fun updateViewBounds(offsetX: Double = 0.0, offsetY: Double = 0.0) {
            view.layer.setBounds(
                view.frame.useContents {
                    CGRectMake(
                        x = offsetX,
                        y = offsetY,
                        width = size.width,
                        height = size.height
                    )
                }
            )
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

        rootSceneViewState?.let {
            updateLayout(it)
        }
    }

    private fun updateLayout(sceneViewState: SceneViewState<UIView>) {
        val scale = densityProvider().density
        val size = view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        _windowInfo.containerSize = size
        sceneViewState.scene.density = densityProvider()
        sceneViewState.scene.size = size

        sceneViewState.needRedraw()
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

        val rootSceneViewState = rootSceneViewState ?: return

        // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
        val isSameSizeTransition = view.frame.useContents {
            CGSizeEqualToSize(size, this.size.readValue())
        }
        if (isSameSizeTransition) {
            return
        }

        val startSnapshotView =
            rootSceneViewState.sceneView.snapshotViewAfterScreenUpdates(false) ?: return

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

        rootSceneViewState.isForcedToPresentWithTransactionEveryFrame = true

        rootSceneViewState.setConstraintsToCenterInView(view, size)
        rootSceneViewState.sceneView.transform = withTransitionCoordinator.targetTransform

        view.layoutIfNeeded()

        withTransitionCoordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform =
                    CGAffineTransformInvert(withTransitionCoordinator.targetTransform)
                rootSceneViewState.sceneView.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                rootSceneViewState.setConstraintsToFillView(view)
                rootSceneViewState.isForcedToPresentWithTransactionEveryFrame = false
            }
        )
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
            observer = keyboardVisibilityListener,
            selector = NSSelectorFromString(keyboardVisibilityListener::keyboardWillShow.name + ":"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = keyboardVisibilityListener,
            selector = NSSelectorFromString(keyboardVisibilityListener::keyboardWillHide.name + ":"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )

        configuration.delegate.viewDidAppear(animated)

    }

    // viewDidUnload() is deprecated and not called.
    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)

        NSNotificationCenter.defaultCenter.removeObserver(
            observer = keyboardVisibilityListener,
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.removeObserver(
            observer = keyboardVisibilityListener,
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
        rootSceneViewState?.dispose()
        rootSceneViewState = null
    }

    private fun attachComposeIfNeeded() {
        if (rootSceneViewState != null) {
            return // already attached
        }
        createSceneViewState().let {
            setContentWithProvider(it.scene, it.isReadyToShowContent, it.interopContext, content)
            doBoilerplate(it, true)
            rootSceneViewState = it//todo bad
        }
    }

    fun setContentWithProvider(
        scene: ComposeScene,
        isReadyToShowContent: State<Boolean>,
        interopContext: UIKitInteropContext,
        content: @Composable () -> Unit
    ) {
        scene.setContent {
            if (!isReadyToShowContent.value) return@setContent
            CompositionLocalProvider(
                LocalLayerContainer provides this.view,
                LocalUIViewController provides this,
                LocalKeyboardOverlapHeight provides this.keyboardOverlapHeight,
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

internal interface ComposeViewState<View, SceneView> {
    val densityProvider: () -> Density
    val focusStack: FocusStack
    fun createSceneViewState(): SceneViewState<SceneView>
    val configuration: ComposeUIViewControllerConfiguration
    val view: View
}
