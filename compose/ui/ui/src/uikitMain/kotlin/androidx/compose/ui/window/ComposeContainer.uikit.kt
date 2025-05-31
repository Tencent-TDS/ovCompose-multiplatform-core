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
import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.CompositionContext
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.LocalSystemTheme
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.extention.DelicateComposeApi
import androidx.compose.ui.extention.GlobalContentScope
import androidx.compose.ui.interop.LocalUIView
import androidx.compose.ui.interop.LocalUIViewController
import androidx.compose.ui.platform.AccessibilityMediator
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.PlatformWindowContext
import androidx.compose.ui.platform.scheduleGCAsync
import androidx.compose.ui.platform.v2.UIKitLifecycle
import androidx.compose.ui.platform.v2.nativefoundation.injectForCompose
import androidx.compose.ui.scene.ComposeScene
import androidx.compose.ui.scene.ComposeSceneContext
import androidx.compose.ui.scene.ComposeSceneLayer
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.scene.MultiLayerComposeScene
import androidx.compose.ui.scene.SceneLayout
import androidx.compose.ui.scene.SingleLayerComposeScene
import androidx.compose.ui.scene.UIViewComposeSceneLayer
import androidx.compose.ui.uikit.ComposeUIViewControllerConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.LocalInterfaceOrientation
import androidx.compose.ui.uikit.LocalRenderBackend
import androidx.compose.ui.uikit.PlistSanityCheck
import androidx.compose.ui.uikit.RenderBackend
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.uikit.utils.CMPRenderBackend
import androidx.compose.ui.uikit.utils.CMPViewController
import androidx.compose.ui.uikit.utils.TMMComposeMarkViewForVideoReport
import androidx.compose.ui.uikit.utils.TMMNativeCreateComposeSceneReusePool
import androidx.compose.ui.uikit.utils.TMMNativeReleaseComposeSceneReusePool
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.util.fastForEach
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
import platform.Foundation.NSStringFromClass
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityExtraLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityLarge
import platform.UIKit.UIContentSizeCategoryAccessibilityMedium
import platform.UIKit.UIContentSizeCategoryExtraExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraLarge
import platform.UIKit.UIContentSizeCategoryExtraSmall
import platform.UIKit.UIContentSizeCategoryLarge
import platform.UIKit.UIContentSizeCategoryMedium
import platform.UIKit.UIContentSizeCategorySmall
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.UIViewControllerTransitionCoordinatorProtocol
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

// region Tencent Code
internal interface ComposeContainer {
    val view: UIView

    val viewController: UIViewController

    val renderBackend: RenderBackend

    @OptIn(InternalComposeApi::class)
    val interfaceOrientationState: MutableState<InterfaceOrientation>

    val systemThemeState: MutableState<SystemTheme>

    fun attachLayer(layer: UIViewComposeSceneLayer)

    fun detachLayer(layer: UIViewComposeSceneLayer)

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext
}

internal class ComposeUIViewControllerContainer(
    private val composeUIViewController: ComposeUIViewController
) : ComposeContainer {
    override val view: UIView
        get() = composeUIViewController.view
    override val viewController: UIViewController
        get() = composeUIViewController
    override val renderBackend: RenderBackend
        get() = composeUIViewController.renderBackend
    @InternalComposeApi
    override val interfaceOrientationState: MutableState<InterfaceOrientation>
        get() = composeUIViewController.interfaceOrientationState
    override val systemThemeState: MutableState<SystemTheme>
        get() = composeUIViewController.systemThemeState

    override fun attachLayer(layer: UIViewComposeSceneLayer) {
        composeUIViewController.attachLayer(layer)
    }

    override fun detachLayer(layer: UIViewComposeSceneLayer) {
        composeUIViewController.detachLayer(layer)
    }

    override fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext {
        return composeUIViewController.createComposeSceneContext(platformContext)
    }
}
// region Tencent Code

private val coroutineDispatcher = Dispatchers.Main

@OptIn(InternalComposeApi::class)
@ExportObjCClass
// region Tencent Code
internal class ComposeUIViewController(
    private val configuration: ComposeUIViewControllerConfiguration,
    private val content: @Composable () -> Unit,
) : CMPViewController(nibName = null, bundle = null) {

    init {
        injectForCompose(configuration.renderBackend)
        setRenderBackend(configuration.renderBackend.asNative())
    }
// endregion
    private var isInsideSwiftUI = false
    private var mediator: ComposeSceneMediator? = null
    private val layers: MutableList<UIViewComposeSceneLayer> = mutableListOf()
    private val layoutDirection get() = getLayoutDirection()

    // region Tencent Code
    private var nativeReusePool: Long = 0
    // endregion

    @OptIn(ExperimentalComposeApi::class)
    private val windowContainer: UIView
        get() = if (configuration.platformLayers) {
            view.window ?: view
        } else view

    // region Tencent Code
    val viewController: UIViewController
        get() = this

    val container: ComposeContainer = ComposeUIViewControllerContainer(this)
    // endregion

    /*
     * Initial value is arbitrarily chosen to avoid propagating invalid value logic
     * It's never the case in real usage scenario to reflect that in type system
     */
    val interfaceOrientationState: MutableState<InterfaceOrientation> = mutableStateOf(
        InterfaceOrientation.Portrait
    )
    val systemThemeState: MutableState<SystemTheme> = mutableStateOf(SystemTheme.Unknown)
    private val focusStack: FocusStack<UIView> = FocusStackImpl()
    private val windowContext = PlatformWindowContext().apply {
        setWindowFocused(true)
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

    // region Tencent Code
    val renderBackend: RenderBackend
        get() = configuration.renderBackend

    private var controllerLifecycle: UIKitLifecycle? = null
    // endregion

    @Suppress("unused")
    @ObjCAction
    fun viewSafeAreaInsetsDidChange() {
        // super.viewSafeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        mediator?.viewSafeAreaInsetsDidChange()
        layers.fastForEach {
            it.viewSafeAreaInsetsDidChange()
        }
    }

    // region Tencent Code
    @OptIn(ExperimentalComposeApi::class)
    override fun loadView() {
        view = UIView().apply {
            if (configuration.clipChildren) {
                setClipsToBounds(true)
            }
            opaque = configuration.opaque
            backgroundColor = if (configuration.opaque) UIColor.whiteColor else UIColor.clearColor
        } // rootView needs to interop with UIKit
        TMMComposeMarkViewForVideoReport(view)
    }
    // endregion

    override fun viewDidLoad() {
        super.viewDidLoad()
        PlistSanityCheck.performIfNeeded()
        configuration.delegate.viewDidLoad()
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
        // region Tencent Code
        controllerLifecycle?.didLoadView()
        // endregion
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

        updateWindowContainer()
    }

    private fun updateWindowContainer() {
        val scale = windowContainer.systemDensity.density
        val size = windowContainer.frame.useContents<CGRect, IntSize> {
            IntSize(
                width = (size.width * scale).roundToInt(),
                height = (size.height * scale).roundToInt()
            )
        }
        windowContext.setContainerSize(size)
        windowContext.setWindowContainer(windowContainer)
        mediator?.viewWillLayoutSubviews()
        layers.fastForEach {
            it.viewWillLayoutSubviews()
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

        mediator?.viewWillTransitionToSize(
            targetSize = size,
            coordinator = withTransitionCoordinator,
        )
        layers.fastForEach {
            it.viewWillTransitionToSize(
                targetSize = size,
                coordinator = withTransitionCoordinator,
            )
        }
        view.layoutIfNeeded()
    }

    override fun viewWillAppear(animated: Boolean) {
        super.viewWillAppear(animated)

        isInsideSwiftUI = checkIfInsideSwiftUI()
        createMediatorIfNeeded()
        configuration.delegate.viewWillAppear(animated)
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        mediator?.viewDidAppear(animated)
        layers.fastForEach {
            it.viewDidAppear(animated)
        }

        updateWindowContainer()

        configuration.delegate.viewDidAppear(animated)
        // region Tencent Code
        controllerLifecycle?.viewDidAppear()
        // endregion
    }

    override fun viewWillDisappear(animated: Boolean) {
        super.viewWillDisappear(animated)
        mediator?.viewWillDisappear(animated)
        layers.fastForEach {
            it.viewWillDisappear(animated)
        }
        configuration.delegate.viewWillDisappear(animated)
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)

        // region Tencent Code
        scheduleGCAsync()
        // endregion

        configuration.delegate.viewDidDisappear(animated)
        // region Tencent Code
        controllerLifecycle?.viewDidDisappear()
        // endregion
    }

    override fun viewControllerDidLeaveWindowHierarchy() {
        super.viewControllerDidLeaveWindowHierarchy()
        dispose()
    }

    // region Tencent Code
    override fun viewControllerPrepareForReuse() {
        super.viewControllerPrepareForReuse()
        createMediatorIfNeeded()
    }
    // endregion

    override fun didReceiveMemoryWarning() {
        println("didReceiveMemoryWarning")
        // region Tencent Code
        scheduleGCAsync()
        // endregion
        super.didReceiveMemoryWarning()
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    // region Tencent Code
    @OptIn(ExperimentalComposeApi::class)
    private fun createRenderingComponent(renderDelegate: RenderingComponent.Delegate) =
        RenderingComponent(
            configuration.renderBackend,
            renderDelegate = renderDelegate,
            firstFrameRenderConfig = configuration.firstFrameRenderConfig
        ).apply {
            view.opaque = configuration.opaque
        }
    // endregion

    @OptIn(ExperimentalComposeApi::class)
    private fun createComposeScene(
        invalidate: () -> Unit,
        platformContext: PlatformContext,
        coroutineContext: CoroutineContext,
    ): ComposeScene = if (configuration.platformLayers) {
        SingleLayerComposeScene(
            coroutineContext = coroutineContext,
            density = systemDensity,
            invalidate = invalidate,
            layoutDirection = layoutDirection,
            composeSceneContext = ComposeSceneContextImpl(
                platformContext = platformContext
            ),
        )
    } else {
        MultiLayerComposeScene(
            coroutineContext = coroutineContext,
            composeSceneContext = ComposeSceneContextImpl(
                platformContext = platformContext
            ),
            density = systemDensity,
            invalidate = invalidate,
            layoutDirection = layoutDirection,
        )
    }

    private fun createMediatorIfNeeded() {
        if (mediator == null) {
            mediator = createMediator()
        }
    }

    // region Tencent Code
    private fun createMediator(): ComposeSceneMediator {
        if (ComposeTabService.viewProxyReuseEnable) {
            releaseReusePoolIfNeeded()
            nativeReusePool = TMMNativeCreateComposeSceneReusePool()
        }
        val mediator = ComposeSceneMediator(
            container = view,
            configuration = configuration,
            focusStack = focusStack,
            windowContext = windowContext,
            coroutineContext = coroutineDispatcher,
            renderingComponentFactory = ::createRenderingComponent,
            boundsInWindow = null,
            onContentSizeChanged = null,
            composeSceneFactory = ::createComposeScene,
            nativeReusePool = nativeReusePool
        )
        val controllerLifecycle =  UIKitLifecycle()
        this.controllerLifecycle = controllerLifecycle
        mediator.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides controllerLifecycle) {
                ProvideContainerCompositionLocals(container, content)
            }
        }
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
        return mediator
    }

    private inline fun releaseReusePoolIfNeeded() {
        if (!ComposeTabService.viewProxyReuseEnable) {
            return
        }
        if (nativeReusePool != 0L) {
            TMMNativeReleaseComposeSceneReusePool(nativeReusePool)
            nativeReusePool = 0L
        }
    }

    private fun dispose() {
        controllerLifecycle?.dispose()
        controllerLifecycle = null
        mediator?.dispose()
        mediator = null
        layers.fastForEach {
            it.close()
        }
        releaseReusePoolIfNeeded()
    }
    // endregion

    fun attachLayer(layer: UIViewComposeSceneLayer) {
        layers.add(layer)
    }

    fun detachLayer(layer: UIViewComposeSceneLayer) {
        layers.remove(layer)
    }

    private inner class ComposeSceneContextImpl(
        override val platformContext: PlatformContext,
    ) : ComposeSceneContext {
        override fun createPlatformLayer(
            density: Density,
            layoutDirection: LayoutDirection,
            focusable: Boolean,
            compositionContext: CompositionContext
        ): ComposeSceneLayer =
            UIViewComposeSceneLayer(
                composeContainer = container,
                initDensity = density,
                initLayoutDirection = layoutDirection,
                configuration = configuration,
                focusStack = if (focusable) focusStack else null,
                windowContext = windowContext,
                compositionContext = compositionContext,
            )
    }

    // region Tencent Code
    internal fun accessibilityMediator(): AccessibilityMediator? {
        return mediator?.accessibilityMediator()
    }
    // endregion
}

// region Tencent Code
private inline fun RenderBackend.asNative(): CMPRenderBackend {
    return when (this) {
        RenderBackend.UIView -> CMPRenderBackend.CMPRenderBackendUIView
        RenderBackend.Skia  -> CMPRenderBackend.CMPRenderBackendSkia
    }
}
// endregion

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

// region Tencent Code
@OptIn(InternalComposeApi::class, DelicateComposeApi::class)
@Composable
internal fun ProvideContainerCompositionLocals(
    composeContainer: ComposeContainer,
    content: @Composable () -> Unit,
) = with(composeContainer) {
    val lazyViewController = remember(composeContainer) {
        lazy { viewController }
    }
    CompositionLocalProvider(
        // Crucial to make this lazy, for ViewController is not present util UIView attached.
        LocalUIViewController providesLazy lazyViewController,
        LocalUIView provides view,
        LocalInterfaceOrientation provides interfaceOrientationState.value,
        LocalSystemTheme provides systemThemeState.value,
        LocalRenderBackend provides renderBackend,
        content = { GlobalContentScope.content(content) }
    )
}
// endregion

internal val uiContentSizeCategoryToFontScaleMap = mapOf(
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
