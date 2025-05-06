/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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
import androidx.compose.runtime.CrashReporter
import androidx.compose.runtime.EnableIosRenderLayerV2
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.EPSILON
import androidx.compose.ui.SystemTheme
import androidx.compose.ui.equalsRelaxed
import androidx.compose.ui.graphics.traceAction
import androidx.compose.ui.node.LargeDimension
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
import androidx.compose.ui.uikit.ComposeUIViewConfiguration
import androidx.compose.ui.uikit.InterfaceOrientation
import androidx.compose.ui.uikit.RenderBackend
import androidx.compose.ui.uikit.systemDensity
import androidx.compose.ui.uikit.utils.TMMComposeMarkViewForVideoReport
import androidx.compose.ui.uikit.utils.TMMNativeCreateComposeSceneReusePool
import androidx.compose.ui.uikit.utils.TMMNativeReleaseComposeSceneReusePool
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import kotlinx.coroutines.Dispatchers
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSize
import platform.CoreGraphics.CGSizeMake
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.UIApplication
import platform.UIKit.UIColor
import platform.UIKit.UIResponder
import platform.UIKit.UITraitCollection
import platform.UIKit.UIUserInterfaceLayoutDirection
import platform.UIKit.UIUserInterfaceStyle
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt

private val coroutineDispatcher = Dispatchers.Main

internal class ComposeUIViewContainer(private val composeUIView: ComposeUIView) : ComposeContainer {
    override val view: UIView
        get() = composeUIView
    override val viewController: UIViewController
        get() = composeUIView.viewController
    override val renderBackend: RenderBackend
        get() = composeUIView.renderBackend
    @OptIn(InternalComposeApi::class)
    override val interfaceOrientationState: MutableState<InterfaceOrientation>
        get() = composeUIView.interfaceOrientationState
    override val systemThemeState: MutableState<SystemTheme>
        get() = composeUIView.systemThemeState

    override fun attachLayer(layer: UIViewComposeSceneLayer) {
        composeUIView.attachLayer(layer)
    }

    override fun detachLayer(layer: UIViewComposeSceneLayer) {
        composeUIView.detachLayer(layer)
    }

    override fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext {
        return composeUIView.createComposeSceneContext(platformContext)
    }
}

@OptIn(InternalComposeApi::class, ExperimentalComposeApi::class)
@ExportObjCClass
class ComposeUIView(
    private val configuration: ComposeUIViewConfiguration,
    private val content: @Composable () -> Unit,
) : UIView(frame = configuration.effectFrame()) {

    init {
        TMMComposeMarkViewForVideoReport(this)
        injectForCompose()
    }

    private var mediator: ComposeSceneMediator? = null
    private val layers: MutableList<UIViewComposeSceneLayer> = mutableListOf()
    private val layoutDirection get() = getLayoutDirection()

    private var isPresent = false

    private var nativeReusePool: Long = 0

    @OptIn(ExperimentalComposeApi::class)
    private val windowContainer: UIView
        get() = if (configuration.platformLayers) { window ?: this } else this

    internal val container: ComposeContainer = ComposeUIViewContainer(this)

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
                window?.windowScene?.interfaceOrientation?.let {
                    InterfaceOrientation.getByRawValue(it)
                }
            } else {
                InterfaceOrientation.getByRawValue(UIApplication.sharedApplication.statusBarOrientation)
            }
        }

    val renderBackend: RenderBackend
        get() = configuration.renderBackend

    val view: UIView
        get() = this

    private var isWidthUnspecified = true
    private var isHeightUnspecified = true
    private var isWidthMatchParent = false
    private var isHeightMatchParent = false

    private var isContentInitialized = false

    private val viewLifecycle = UIKitLifecycle()

    val viewController: UIViewController
        get() {
            var responder: UIResponder? = this.nextResponder
            while (responder != null) {
                if (responder is UIViewController) return responder
                responder = responder.nextResponder
            }
            throw IllegalStateException("$this is not attached to any UIViewController.")
        }

    init {
        // In case that frame is configured on initialization.
        updateFrameSizeSpec()

        setClipsToBounds(true)
        opaque = configuration.opaque
        backgroundColor = if (configuration.opaque) UIColor.whiteColor else UIColor.clearColor
        viewLifecycle.didLoadView()
    }

    override fun setFrame(frame: CValue<CGRect>) {
        super.setFrame(frame)
        updateFrameSizeSpec()
    }

    override fun setBounds(bounds: CValue<CGRect>) {
        super.setBounds(bounds)
        updateFrameSizeSpec()
    }

    private fun updateFrameSizeSpec() {
        frame.useContents {
            isWidthUnspecified = size.width < EPSILON
            isHeightUnspecified = size.height < EPSILON
        }
    }

    override fun safeAreaInsetsDidChange() {
        super.safeAreaInsetsDidChange() // TODO: call super after Kotlin 1.8.20
        mediator?.viewSafeAreaInsetsDidChange()
        layers.fastForEach {
            it.viewSafeAreaInsetsDidChange()
        }

        // Initialize Compose content here to take safe area into account
        // when calculate the content size.
        initContentIfNot()
    }

    override fun traitCollectionDidChange(previousTraitCollection: UITraitCollection?) {
        super.traitCollectionDidChange(previousTraitCollection)
        systemThemeState.value = traitCollection.userInterfaceStyle.asComposeSystemTheme()
    }

    override fun layoutSubviews() {
        // UIKit possesses all required info for layout at this point
        traceAction("ComposeUIView layoutSubviews") {
            currentInterfaceOrientation?.let {
                interfaceOrientationState.value = it
            }

            updateWindowContainer()
            super.layoutSubviews()

            // Size of subviews(InteractionView and RenderingUIView) will be determined
            // in super.layoutSubviews call. Invoke initContentIfNot after super.layoutSubviews
            // will also trigger the resize of subviews.
            // In most cases, Compose content will be initialized in safeAreaInsetsDidChange.
            try {
                initContentIfNot()
            } catch (throwable: Throwable) {
                CrashReporter.reportThrowable(throwable)
                throw throwable
            }
        }
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

        // window size unchanged is not adequate to skip rendering.
        // Size of ComposeUIView may change on its own.
        mediator?.viewWillLayoutSubviews()
        layers.fastForEach {
            it.viewWillLayoutSubviews()
        }
    }

    private fun viewWillAppear() {
        createMediatorIfNeeded()
        configuration.delegate.viewWillAppear(false)
    }

    fun appear() {
        if (isPresent) return
        isPresent = true

        viewDidAppear()
    }

    private fun viewDidAppear() {
        mediator?.viewDidAppear(false)
        layers.fastForEach {
            it.viewDidAppear(false)
        }

        updateWindowContainer()

        configuration.delegate.viewDidAppear(false)
        viewLifecycle.viewDidAppear()
    }

    private fun viewWillDisappear() {
        mediator?.viewWillDisappear(false)
        layers.fastForEach {
            it.viewWillDisappear(false)
        }
        configuration.delegate.viewWillDisappear(false)
    }

    fun disappear() {
        if (!isPresent) return
        isPresent = false

        viewDidDisappear()
    }

    private fun viewDidDisappear() {
        configuration.delegate.viewDidDisappear(false)
        viewLifecycle.viewDidDisappear()
    }

    override fun willMoveToSuperview(newSuperview: UIView?) {
        super.willMoveToSuperview(newSuperview)

        if (newSuperview == null) {
            viewWillDisappear()
        } else {
            viewWillAppear()
        }
    }

    override fun didMoveToSuperview() {
        super.didMoveToSuperview()

        if (superview == null) {
            viewDidDisappear()
        } else {
            viewDidAppear()
        }
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()

        val sizeSpec = adjustViewSizeToMatchParent()
        resizeRootView(sizeSpec)
    }

    fun createComposeSceneContext(platformContext: PlatformContext): ComposeSceneContext =
        ComposeSceneContextImpl(platformContext)

    @OptIn(ExperimentalComposeApi::class)
    private fun createRenderingComponent(renderDelegate: RenderingComponent.Delegate) =
        RenderingComponent(configuration.renderBackend, renderDelegate = renderDelegate).apply {
            view.opaque = configuration.opaque
        }

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

    private fun createMediatorIfNeeded(): ComposeSceneMediator {
        var mediator = this.mediator
        if (mediator == null) {
            mediator = createMediator()
            this.mediator = mediator
        }
        return mediator
    }

    private fun createMediator(): ComposeSceneMediator {
        if (ComposeTabService.viewProxyReuseEnable) {
            releaseReusePoolIfNeeded()
            this.nativeReusePool = TMMNativeCreateComposeSceneReusePool()
        }
        val mediator = ComposeSceneMediator(
            container = view,
            configuration = configuration,
            focusStack = focusStack,
            windowContext = windowContext,
            coroutineContext = coroutineDispatcher,
            renderingComponentFactory = ::createRenderingComponent,
            boundsInWindow = ::getBoundsInWindow,
            onContentSizeChanged = {
                val sizeSpec = adjustViewSizeToContentSize(it)
                resizeRootView(sizeSpec)

                with(systemDensity) {
                    configuration.delegate.viewSizeDidChange(DpSize(it.width.toDp(), it.height.toDp()))
                }
            },
            composeSceneFactory = ::createComposeScene,
            nativeReusePool = nativeReusePool
        )
        return mediator
    }

    private inline fun releaseReusePoolIfNeeded() {
        if (!ComposeTabService.viewProxyReuseEnable) {
            return
        }
        if (this.nativeReusePool != 0L) {
            TMMNativeReleaseComposeSceneReusePool(this.nativeReusePool)
            this.nativeReusePool = 0L
        }
    }

    private fun getBoundsInWindow(): IntRect {
        val offsetInWindow = windowContext.offsetInWindow(view)

        val size = with(systemDensity) {
            val maxWidth = configuration.maxSize.width.takeIf { it != Dp.Unspecified }?.roundToPx() ?: Constraints.Infinity
            val maxHeight = configuration.maxSize.height.takeIf { it != Dp.Unspecified }?.roundToPx() ?: Constraints.Infinity

            if (isWidthUnspecified && isHeightUnspecified) {
                IntSize(maxWidth, maxHeight)
            } else {
                view.bounds.useContents {
                    IntSize(
                        if (isWidthUnspecified) maxWidth else size.width.dp.roundToPx(),
                        if (isHeightUnspecified) maxHeight else size.height.dp.roundToPx()
                    )
                }
            }
        }
        return IntRect(offset = offsetInWindow, size = size)
    }

    private fun initContentIfNot() {
        if (isContentInitialized) return
        isContentInitialized = true

        val mediator = createMediatorIfNeeded()
        EnableIosRenderLayerV2 = configuration.renderBackend == RenderBackend.UIView
        mediator.setContentImmediately {
            CompositionLocalProvider(LocalLifecycleOwner provides viewLifecycle) {
                ProvideContainerCompositionLocals(container, content)
            }
        }
        mediator.setLayout(SceneLayout.UseConstraintsToFillContainer)
        val sizeSpec = adjustViewSizeWithContent()
        sizeSpec?.updateFrom(adjustViewSizeToMatchParent())
        resizeRootView(sizeSpec)
    }

    private fun adjustViewSizeWithContent(): SizeSpec? {
        val mediator = this.mediator ?: return null
        if (!this.isWidthUnspecified && !this.isHeightUnspecified) return null

        val size = mediator.calculateContentSize()
        val density = windowContainer.systemDensity.density

        val sizeSpec = SizeSpec()

        if (isWidthUnspecified) {
            if (size.width == LargeDimension) {
                isWidthMatchParent = true
            } else {
                sizeSpec.width = size.width.toDouble() / density
            }
        }
        if (isHeightUnspecified) {
            if (size.height == LargeDimension) {
                isHeightMatchParent = true
            } else {
                sizeSpec.height = size.height.toDouble() / density
            }
        }
        return sizeSpec
    }

    private fun adjustViewSizeToMatchParent(): SizeSpec? {
        if (!isWidthMatchParent && !isHeightMatchParent) return null
        val sizeSpec = SizeSpec()
        if (isWidthMatchParent) {
            sizeSpec.width = superview?.frame?.useContents { this.size.width }
        }
        if (isHeightMatchParent) {
            sizeSpec.height = superview?.frame?.useContents { this.size.height }
        }
        return sizeSpec
    }

    private fun adjustViewSizeToContentSize(newSize: IntSize): SizeSpec? {
        if (!isWidthUnspecified && !isHeightUnspecified) return null

        val sizeSpec = SizeSpec()
        val density = windowContainer.systemDensity.density

        if (isWidthUnspecified) {
            sizeSpec.width = newSize.width.toDouble() / density
        }
        if (isHeightUnspecified) {
            sizeSpec.height = newSize.height.toDouble() / density
        }
        return sizeSpec
    }

    private fun resizeRootView(sizeSpec: SizeSpec?) {
        if (sizeSpec?.width == null && sizeSpec?.height == null) return

        frame.useContents {
            // Do not call this.setFrame to keep isWidthUnspecified and isHeightUnspecified
            // determined by outside.
            if (!sizeSpec.isSameWith(this.size.width, this.size.height)) {
                super.setFrame(
                    CGRectMake(
                        origin.x,
                        origin.y,
                        sizeSpec.width ?: this.size.width,
                        sizeSpec.height ?: this.size.height
                    )
                )
                setNeedsLayout()
            }
        }
    }

    fun dispose() {
        viewLifecycle.dispose()
        mediator?.dispose()
        mediator = null
        layers.fastForEach {
            it.close()
        }
        releaseReusePoolIfNeeded()
        scheduleGCAsync()
    }

    internal fun attachLayer(layer: UIViewComposeSceneLayer) {
        layers.add(layer)
    }

    internal fun detachLayer(layer: UIViewComposeSceneLayer) {
        layers.remove(layer)
    }

    fun outerContainerOffsetChange(x: Float, y: Float) {
        mediator?.outerContainerOffsetChange(x, y)
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

    override fun sizeThatFits(size: CValue<CGSize>): CValue<CGSize> {
        val mediator = createMediatorIfNeeded()
        initContentIfNot()
        val density = windowContainer.systemDensity.density
        return mediator.calculateContentSize().let { contentSize ->
            size.useContents {
                CGSizeMake(
                    minOf(contentSize.width.toDouble() / density, width),
                    minOf(contentSize.height.toDouble() / density, height)
                )
            }
        }
    }

    internal fun accessibilityMediator(): AccessibilityMediator? {
        return mediator?.accessibilityMediator()
    }
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

private class SizeSpec(var width: Double? = null, var height: Double? = null) {
    fun isSameWith(width: Double, height: Double): Boolean {
        return this.width?.equalsRelaxed(width) != false && this.height?.equalsRelaxed(height) != false
    }

    fun updateFrom(other: SizeSpec?) {
        other ?: return
        width = other.width ?: width
        height = other.height ?: height
    }

    override fun toString(): String {
        return "SizeSpec($width ,$height)"
    }
}