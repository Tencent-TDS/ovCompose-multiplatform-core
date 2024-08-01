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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.toUIColor
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.height
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toDpOffset
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toOffset
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.width
import androidx.compose.ui.viewinterop.InteropContainer
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewGroup
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.TypedInteropUIView
import androidx.compose.ui.viewinterop.TypedInteropUIViewController
import androidx.compose.ui.viewinterop.TypedInteropViewHolder
import androidx.compose.ui.viewinterop.UIKitInteropContainer
import androidx.compose.ui.viewinterop.interopViewSemantics
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView
import platform.UIKit.UIViewController

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}
private val DefaultViewResize: UIView.(CValue<CGRect>) -> Unit = { rect -> this.setFrame(rect) }
private val DefaultViewControllerResize: UIViewController.(CValue<CGRect>) -> Unit =
    { rect -> this.view.setFrame(rect) }

internal abstract class UIKitInteropElementHolder<T : InteropView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropViewGroup,
    override val isInteractive: Boolean,
    isNativeAccessibilityEnabled: Boolean,
    compositeHashKey: Int,
) : TypedInteropViewHolder<T>(factory, interopContainer, group, compositeHashKey) {
    override val measurePolicy: MeasurePolicy
        get() = MeasurePolicy { _, constraints ->
            layout(constraints.minWidth, constraints.minHeight) {
                // No-op, no children are expected
            }
        }

    override val extraModifier = Modifier
        .drawBehind {
            drawRect(
                color = Color.Transparent,
                blendMode = BlendMode.Clear
            )
        }
        .interopViewSemantics(isNativeAccessibilityEnabled, group)

    private var currentUnclippedRect: IntRect? = null
    private var currentClippedRect: IntRect? = null

    protected fun updateRect(unclippedRect: IntRect, clippedRect: IntRect) {
        if (currentUnclippedRect == unclippedRect && currentClippedRect == clippedRect) {
            return
        }

        val clippedDpRect = clippedRect.toRect().toDpRect(density)
        val unclippedDpRect = unclippedRect.toRect().toDpRect(density)

        // wrapping view itself is always using the clipped rect
        if (clippedRect != currentClippedRect) {
            container.changeInteropViewLayout {
                group.setFrame(clippedDpRect.asCGRect())
            }
        }

        // Only call onResize if the actual size changes.
        if (currentUnclippedRect != unclippedRect || currentClippedRect != clippedRect) {
            // offset to move the component to the correct position inside the wrapping view, so
            // its global unclipped frame stays the same
            val offset = unclippedRect.topLeft - clippedRect.topLeft
            val dpOffset = offset.toOffset().toDpOffset(density)

            container.changeInteropViewLayout {
                // The actual component created by the user is resized here using the provided callback.
                val rect = CGRectMake(
                    x = dpOffset.x.value.toDouble(),
                    y = dpOffset.y.value.toDouble(),
                    width = unclippedDpRect.width.value.toDouble(),
                    height = unclippedDpRect.height.value.toDouble()
                )

                setUserComponentFrame(rect)
            }
        }

        currentUnclippedRect = unclippedRect
        currentClippedRect = clippedRect
    }


    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val rootCoordinates = layoutCoordinates.findRootCoordinates()

        val unclippedBounds = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = false
            )

        val clippedBounds = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = true
            )

        updateRect(
            unclippedRect = unclippedBounds.roundToIntRect(),
            clippedRect = clippedBounds.roundToIntRect()
        )
    }

    abstract fun setUserComponentFrame(rect: CValue<CGRect>)
}

internal class UIKitInteropViewHolder<T : UIView>(
    createView: () -> T,
    interopContainer: InteropContainer,
    group: InteropViewGroup,
    isInteractive: Boolean,
    isNativeAccessibilityEnabled: Boolean,
    compositeHashKey: Int
) : UIKitInteropElementHolder<TypedInteropUIView<T>>(
    factory = {
        TypedInteropUIView(
            group = group,
            view = createView()
        )
    },
    interopContainer = interopContainer,
    group = group,
    isInteractive = isInteractive,
    isNativeAccessibilityEnabled = isNativeAccessibilityEnabled,
    compositeHashKey = compositeHashKey
) {
    init {
        // Group will be placed to hierarchy in [InteropContainer.placeInteropView]
        group.addSubview(interopView.view)
    }

    override fun setUserComponentFrame(rect: CValue<CGRect>) {
        interopView.view.setFrame(rect)
    }
}

internal class InteropUIViewControllerHolder<T : UIViewController>(
    createViewController: () -> T,
    interopContainer: UIKitInteropContainer,
    group: InteropViewGroup,
    isInteractive: Boolean,
    isNativeAccessibilityEnabled: Boolean,
    compositeHashKey: Int
) : UIKitInteropElementHolder<TypedInteropUIViewController<T>>(
    factory = {
        TypedInteropUIViewController(
            group = group,
            viewController = createViewController()
        )
    },
    interopContainer = interopContainer,
    group = group,
    isInteractive = isInteractive,
    isNativeAccessibilityEnabled = isNativeAccessibilityEnabled,
    compositeHashKey = compositeHashKey
) {
    init {
        // Group will be placed to hierarchy in [InteropContainer.placeInteropView]
        group.addSubview(interopView.viewController.view)
    }

    override fun setUserComponentFrame(rect: CValue<CGRect>) {
        interopView.viewController.view.setFrame(rect)
    }
}

/**
 * @param factory The block creating the [UIView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIView
 * @param accessibilityEnabled If `true`, then the view will be visible to accessibility services.
 *
 * If this Composable is within a modifier chain that merges
 * the semantics of its children (such as `Modifier.clickable`), the merged subtree data will be ignored in favor of
 * the native UIAccessibility resolution for the view constructed by [factory]. For example, `Button` containing [UIKitView]
 * will be invisible for accessibility services, only the [UIView] created by [factory] will be accessible.
 * To avoid this behavior, set [accessibilityEnabled] to `false` and use custom [Modifier.semantics] for `Button` to
 * make the information associated with this view accessible.
 *
 * If there are multiple [UIKitView] or [UIKitViewController] with [accessibilityEnabled] set to `true` in the merged tree, only the first one will be accessible.
 * Consider using a single [UIKitView] or [UIKitViewController] with multiple views inside it if you need multiple accessible views.
 *
 * In general, [accessibilityEnabled] set to `true` is not recommended to use in such cases.
 * Consider using [Modifier.semantics] on Composable that merges its semantics instead.
 *
 * @see Modifier.semantics
 */
@Composable
fun <T : UIView> UIKitView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (view: T, rect: CValue<CGRect>) -> Unit = DefaultViewResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true
) {
    val compositeKeyHash = currentCompositeKeyHash
    val interopContainer = LocalInteropContainer.current
    InteropView(
        factory = {
            UIKitInteropViewHolder(
                createView = factory,
                interopContainer = interopContainer,
                group = InteropWrappingView(areTouchesDelayed = true),
                isInteractive = interactive,
                isNativeAccessibilityEnabled = accessibilityEnabled,
                compositeHashKey = compositeKeyHash
            )
        },
        modifier = modifier,
        onReset = null,
        onRelease = {
            onRelease(it.view)
        },
        update = {
            if (background != Color.Unspecified) {
                it.view.backgroundColor = background.toUIColor()
            }
            update(it.view)
        }
    )
}

/**
 * @param factory The block creating the [UIViewController] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [UIView] background wrapping the view of [UIViewController] created by [factory].
 * @param onRelease A callback invoked as a signal that this view controller instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * view controller should be freed at this time.
 * @param onResize May be used to custom resize logic.
 * @param interactive If true, then user touches will be passed to this UIViewController
 * @param accessibilityEnabled If `true`, then the [UIViewController.view] will be visible to accessibility services.
 *
 * If this Composable is within a modifier chain that merges the semantics of its children (such as `Modifier.clickable`),
 * the merged subtree data will be ignored in favor of
 * the native UIAccessibility resolution for the [UIViewController.view] of [UIViewController] constructed by [factory].
 * For example, `Button` containing [UIKitViewController] will be invisible for accessibility services,
 * only the [UIViewController.view] of [UIViewController] created by [factory] will be accessible.
 * To avoid this behavior, set [accessibilityEnabled] to `false` and use custom [Modifier.semantics] for `Button` to
 * make the information associated with the [UIViewController] accessible.
 *
 * If there are multiple [UIKitView] or [UIKitViewController] with [accessibilityEnabled] set to `true` in the merged tree,
 * only the first one will be accessible.
 * Consider using a single [UIKitView] or [UIKitViewController] with multiple views inside it if you need multiple accessible views.
 *
 * In general, [accessibilityEnabled] set to `true` is not recommended to use in such cases.
 * Consider using [Modifier.semantics] on Composable that merges its semantics instead.
 *
 * @see Modifier.semantics
 */
@Composable
fun <T : UIViewController> UIKitViewController(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    onResize: (viewController: T, rect: CValue<CGRect>) -> Unit = DefaultViewControllerResize,
    interactive: Boolean = true,
    accessibilityEnabled: Boolean = true
) {

}