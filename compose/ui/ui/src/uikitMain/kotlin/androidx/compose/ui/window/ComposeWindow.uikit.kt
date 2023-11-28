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

@file:OptIn(ExperimentalComposeApi::class)

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
import androidx.compose.ui.input.InputMode
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
import androidx.compose.ui.window.di.ComposeViewWrapper
import androidx.compose.ui.window.di.ComposeViewWrapperImpl
import androidx.compose.ui.window.di.FocusStack
import androidx.compose.ui.window.di.FocusStackImpl
import androidx.compose.ui.window.di.KeyboardEventHandler
import androidx.compose.ui.window.di.PlatformContextImpl
import androidx.compose.ui.window.di.SkikoUIViewDelegateImpl
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineDispatcher
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
): UIViewController = object {
    val densityProvider = {
        val contentSizeCategory =
            uiViewController.traitCollection.preferredContentSizeCategory
                ?: UIContentSizeCategoryUnspecified

        val fontScale: Float = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        Density(
            uiViewController.attachedComposeContext?.viewWrapper?.view?.contentScaleFactor?.toFloat()
                ?: 1f,
            fontScale
        )
    }
    val createViewWrapper =
        { focusable: Boolean, keyboardEventHandler: KeyboardEventHandler, delegate: SkikoUIViewDelegate ->
            ComposeViewWrapperImpl(
                SkikoUIView(focusable, keyboardEventHandler, delegate)
            )
        }
    val focusStack: FocusStack = FocusStackImpl()
    val createSceneEntities = { focusable: Boolean, buildScene: SceneEntities.() -> ComposeScene ->
        class ViewDI(focusable: Boolean, buildScene: SceneEntities.() -> ComposeScene) :
            SceneEntities {
            override val scene: ComposeScene by lazy { buildScene() }
            override val viewWrapper: ComposeViewWrapper by lazy {
                createViewWrapper(
                    focusable,
                    keyboardEventHandler,
                    delegate
                )
            }
            override val interopContext: UIKitInteropContext by lazy {
                UIKitInteropContext(
                    requestRedraw = { viewWrapper.needRedraw() })
            }
            override val platformContext: PlatformContext by lazy {
                PlatformContextImpl(
                    inputServices = inputServices,
                    textToolbar = textToolbar,
                    windowInfo = uiViewController._windowInfo,
                    densityProvider = densityProvider,
                )
            }
            override val attachedComposeContext: AttachedComposeContext by lazy {
                AttachedComposeContext(
                    scene,
                    viewWrapper,
                    interopContext
                )
            }
            val uiKitTextInputService: UIKitTextInputService by lazy {
                UIKitTextInputService(
                    updateView = {
                        viewWrapper.view.setNeedsDisplay() // redraw on next frame
                        CATransaction.flush() // clear all animations
                        viewWrapper.view.reloadInputViews() // update input (like screen keyboard)//todo redundant?
                    },
                    rootViewProvider = { uiViewController.view },
                    densityProvider = densityProvider,
                    focusStack = focusStack,
                    keyboardEventHandler = keyboardEventHandler
                )
            }
            val inputServices: PlatformTextInputService get() = uiKitTextInputService
            val textToolbar: TextToolbar get() = uiKitTextInputService
            val keyboardEventHandler: KeyboardEventHandler by lazy {
                object : KeyboardEventHandler {
                    override fun onKeyboardEvent(event: SkikoKeyboardEvent) {
                        val composeEvent = KeyEvent(event)
                        if (!uiKitTextInputService.onPreviewKeyEvent(composeEvent)) {
                            scene.sendKeyEvent(composeEvent)
                        }
                    }
                }
            }
            val delegate: SkikoUIViewDelegate by lazy {
                SkikoUIViewDelegateImpl(
                    { scene },
                    interopContext,
                    densityProvider,
                )
            }
        }
        ViewDI(focusable, buildScene)
    }
    val uiViewController: UIViewController by lazy {
        ComposeWindow(
            configuration = ComposeUIViewControllerConfiguration().apply(configure),
            content = content,
            densityProvider = densityProvider,
            createViewWrapper = createViewWrapper,
            focusStack = focusStack,
            createSceneEntities = createSceneEntities,
        )
    }
}.uiViewController

@OptIn(InternalComposeApi::class)
@ExportObjCClass
private class ComposeWindow(
    val configuration: ComposeUIViewControllerConfiguration,
    val content: @Composable () -> Unit,
    val densityProvider: () -> Density,
    val createViewWrapper: (focusable: Boolean, KeyboardEventHandler, SkikoUIViewDelegate) -> ComposeViewWrapper,
    val focusStack: FocusStack,
    val createSceneEntities: (focusable: Boolean, buildScene: SceneEntities.() -> ComposeScene) -> SceneEntities,
) : UIViewController(nibName = null, bundle = null) {
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

    internal val _windowInfo = WindowInfoImpl().apply {
        isWindowFocused = true
    }

    internal var attachedComposeContext: AttachedComposeContext? = null

    private val keyboardVisibilityListener = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun keyboardWillShow(arg: NSNotification) {
            animateKeyboard(arg, true)

            val scene = attachedComposeContext?.scene ?: return
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
            val viewHeight = attachedComposeContext?.viewWrapper?.view?.frame?.useContents {
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

        attachedComposeContext?.let {
            updateLayout(it)
        }
    }

    private fun updateLayout(context: AttachedComposeContext) {
        val scale = densityProvider().density
        val size = view.frame.useContents {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        _windowInfo.containerSize = size
        context.scene.density = densityProvider()
        context.scene.size = size

        context.viewWrapper.needRedraw()
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

        val attachedComposeContext = attachedComposeContext ?: return

        // Happens during orientation change from LandscapeLeft to LandscapeRight, for example
        val isSameSizeTransition = view.frame.useContents {
            CGSizeEqualToSize(size, this.size.readValue())
        }
        if (isSameSizeTransition) {
            return
        }

        val startSnapshotView =
            attachedComposeContext.viewWrapper.view.snapshotViewAfterScreenUpdates(false) ?: return

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

        attachedComposeContext.viewWrapper.isForcedToPresentWithTransactionEveryFrame = true

        attachedComposeContext.setConstraintsToCenterInView(view, size)
        attachedComposeContext.viewWrapper.view.transform = withTransitionCoordinator.targetTransform

        view.layoutIfNeeded()

        withTransitionCoordinator.animateAlongsideTransition(
            animation = {
                startSnapshotView.alpha = 0.0
                startSnapshotView.transform =
                    CGAffineTransformInvert(withTransitionCoordinator.targetTransform)
                attachedComposeContext.viewWrapper.view.transform = CGAffineTransformIdentity.readValue()
            },
            completion = {
                startSnapshotView.removeFromSuperview()
                attachedComposeContext.setConstraintsToFillView(view)
                attachedComposeContext.viewWrapper.isForcedToPresentWithTransactionEveryFrame = false
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
        attachedComposeContext?.dispose()
        attachedComposeContext = null
    }

    private fun attachComposeIfNeeded() {
        if (attachedComposeContext != null) {
            return // already attached
        }

        val coroutineDispatcher: CoroutineDispatcher = Dispatchers.Main

        fun SceneEntities.doBoilerplate(focusable: Boolean) {
            view.addSubview(viewWrapper.view)
            attachedComposeContext.setConstraintsToFillView(view)
            updateLayout(attachedComposeContext)
            if (focusable) {
                focusStack.push(viewWrapper.view)
            }
        }

        if (configuration.singleLayerComposeScene) {
            fun prepareSingleLayerComposeScene(
                density: Density,
                layoutDirection: LayoutDirection,
                focusable: Boolean,
                coroutineContext: CoroutineContext,
                prepareComposeSceneContext: () -> ComposeSceneContext,
            ): SceneEntities {
                return createSceneEntities(focusable) {
                    SingleLayerComposeScene(
                        coroutineContext = coroutineContext,
                        composeSceneContext = object :
                            ComposeSceneContext by prepareComposeSceneContext() {
                            //todo do we need new platform context on every SingleLayerComposeScene?
                            override val platformContext: PlatformContext get() = this@createSceneEntities.platformContext
                        },
                        density = density,
                        invalidate = viewWrapper::needRedraw,
                        layoutDirection = layoutDirection,
                    )
                }
            }

            prepareSingleLayerComposeScene(
                density = densityProvider(),
                layoutDirection = LayoutDirection.Ltr,//todo get from system?
                focusable = true,
                coroutineContext = coroutineDispatcher
            ) {
                object : ComposeSceneContext {
                    override fun createPlatformLayer(
                        density: Density,
                        layoutDirection: LayoutDirection,
                        focusable: Boolean,
                        compositionContext: CompositionContext
                    ): ComposeSceneLayer {
                        return prepareSingleLayerComposeScene(
                            density,
                            layoutDirection,
                            focusable,
                            compositionContext.effectCoroutineContext,
                            { this },
                        ).run {
                            doBoilerplate(focusable)
                            viewWrapper.view.alpha = 0.5

                            object : ComposeSceneLayer {
                                override var density: Density = density
                                override var layoutDirection: LayoutDirection = layoutDirection
                                override var bounds: IntRect
                                    get() = IntRect(
                                        offset = IntOffset(
                                            x = viewWrapper.view.bounds.useContents { origin.x.toInt() },
                                            y = viewWrapper.view.bounds.useContents { origin.y.toInt() },
                                        ),
                                        size = IntSize(
                                            width = viewWrapper.view.bounds.useContents { size.width.toInt() },
                                            height = viewWrapper.view.bounds.useContents { size.height.toInt() },
                                        )
                                    )
                                    set(value) {
                                        println("ComposeSceneLayer, set bounds $value")
                                        viewWrapper.view.setBounds(
                                            CGRectMake(
                                                value.left.toDouble(),
                                                value.top.toDouble(),
                                                value.width.toDouble(),
                                                value.height.toDouble()
                                            )
                                        )
                                    }
                                override var scrimColor: Color? = null
                                override var focusable: Boolean = true

                                override fun close() {
                                    println("ComposeSceneContext close")
                                    focusStack.popUntilNext(viewWrapper.view)
                                    viewWrapper.dispose()
                                    viewWrapper.view.removeFromSuperview()
                                }

                                override fun setContent(content: @Composable () -> Unit) {
                                    //todo New Compose Scene  Размер сцены scene.size - полный экран
                                    //  Сделать translate Canvas по размеру -bounds.position, размер канвы bounds.size
                                    //  translate делать при каждой отрисовке
                                    //  canvas.translate(x, y)
                                    //  drawContainedDrawModifiers(canvas)
                                    //  canvas.translate(-x, -y)
                                    //  А размер канвы задавать в bounds set(value) {...
                                    scene.setContentWithProvider(
                                        viewWrapper.isReadyToShowContent,
                                        interopContext,
                                        content
                                    )
                                }

                                override fun setKeyEventListener(
                                    onPreviewKeyEvent: ((KeyEvent) -> Boolean)?,
                                    onKeyEvent: ((KeyEvent) -> Boolean)?
                                ) {

                                }

                                override fun setOutsidePointerEventListener(onOutsidePointerEvent: ((mainEvent: Boolean) -> Unit)?) {

                                }
                            }
                        }
                    }
                }
            }
        } else {
            createSceneEntities(true) {
                MultiLayerComposeScene(
                    coroutineContext = coroutineDispatcher,
                    composeSceneContext = object : ComposeSceneContext {
                        override val platformContext: PlatformContext get() = this@createSceneEntities.platformContext
                    },
                    density = densityProvider(),
                    invalidate = viewWrapper::needRedraw,
                )
            } as SceneEntities
        }.apply {
            scene.setContentWithProvider(viewWrapper.isReadyToShowContent, interopContext, content)
            doBoilerplate(true)
            this@ComposeWindow.attachedComposeContext = attachedComposeContext//todo bad
        }
    }

    private fun ComposeScene.setContentWithProvider(isReadyToShowContent: State<Boolean>, interopContext: UIKitInteropContext, content: @Composable ()->Unit) {
        setContent {
            if (!isReadyToShowContent.value) return@setContent
            CompositionLocalProvider(
                LocalLayerContainer provides view,
                LocalUIViewController provides this@ComposeWindow,
                LocalKeyboardOverlapHeight provides keyboardOverlapHeight,
                LocalSafeArea provides safeArea,
                LocalLayoutMargins provides layoutMargins,
                LocalInterfaceOrientation provides interfaceOrientation,
                LocalSystemTheme provides systemTheme.value,
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

private interface SceneEntities {
    val scene: ComposeScene
    val viewWrapper:ComposeViewWrapper
    val interopContext:UIKitInteropContext
    val platformContext: PlatformContext
    val attachedComposeContext: AttachedComposeContext
}

//todo New Compose Scene FIXME: It's better to rename it now
private class AttachedComposeContext(
    val scene: ComposeScene,
    val viewWrapper: ComposeViewWrapper,
    val interopContext: UIKitInteropContext,
) {
    private var constraints: List<NSLayoutConstraint> = emptyList()
        set(value) {
            if (field.isNotEmpty()) {
                NSLayoutConstraint.deactivateConstraints(field)
            }
            field = value
            NSLayoutConstraint.activateConstraints(value)
        }

    fun setConstraintsToCenterInView(parentView: UIView, size: CValue<CGSize>) {
        size.useContents {
            constraints = listOf(
                viewWrapper.view.centerXAnchor.constraintEqualToAnchor(parentView.centerXAnchor),
                viewWrapper.view.centerYAnchor.constraintEqualToAnchor(parentView.centerYAnchor),
                viewWrapper.view.widthAnchor.constraintEqualToConstant(width),
                viewWrapper.view.heightAnchor.constraintEqualToConstant(height)
            )
        }
    }

    fun setConstraintsToFillView(parentView: UIView) {
        constraints = listOf(
            viewWrapper.view.leftAnchor.constraintEqualToAnchor(parentView.leftAnchor),
            viewWrapper.view.rightAnchor.constraintEqualToAnchor(parentView.rightAnchor),
            viewWrapper.view.topAnchor.constraintEqualToAnchor(parentView.topAnchor),
            viewWrapper.view.bottomAnchor.constraintEqualToAnchor(parentView.bottomAnchor)
        )
    }

    fun dispose() {
        scene.close()
        //todo New Compose Scene почистить все слои
        // After scene is disposed all UIKit interop actions can't be deferred to be synchronized with rendering
        // Thus they need to be executed now.
        interopContext.retrieve().actions.forEach { it.invoke() }
        viewWrapper.dispose()
    }
}
