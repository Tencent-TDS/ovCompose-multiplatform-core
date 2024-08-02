/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.AccessibilityKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.uikit.utils.CMPInteropWrappingView
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * On iOS, [interopView] is encapsulating the hierarchy consisting of a wrapping
 * [InteropViewGroup] and underlying [UIView] or a [UIViewController]. This immediate
 * class is only used for API surface where the top-level view is needed for general-purpose
 * operations, such as view hierarchy hit-testing and modification.
 *
 * Subclasses of [interopView] are used to represent specific types of interop views to forward
 * user-settable callbacks.
 *
 * @property group The [InteropViewGroup] (aka [UIView]) that contains the underlying
 * interop element.
 */
actual open class InteropView internal constructor(
    val group: InteropViewGroup
)

/**
 * An [InteropView] that contains underlying type-erased [UIViewController].
 */
internal open class InteropUIViewController(
    group: InteropViewGroup,
    open val viewController: UIViewController
)  : InteropView(group)

/**
 * An [InteropView] that contains underlying [UIViewController] of certain type [T].
 */
internal class TypedInteropUIViewController<T : UIViewController>(
    group: InteropViewGroup,
    override val viewController: T // Narrowed type
) : InteropUIViewController(group, viewController)
/**
 * An [InteropView] that contains underlying [UIView] of certain type [T].
 */
internal class TypedInteropUIView<T : UIView>(
    group: InteropViewGroup,
    val view: T
) : InteropView(group)


@Suppress("ACTUAL_WITHOUT_EXPECT") // https://youtrack.jetbrains.com/issue/KT-37316
internal actual typealias InteropViewGroup = UIView

/**
 * A [UIView] that contains underlying interop element, such as an independent [UIView]
 * or [UIViewController]'s root [UIView].
 *
 * @param areTouchesDelayed indicates whether the touches are allowed to be delayed by Compose
 * in attempt to intercept touches, or should get delivered to the interop view immediately without
 * Compose being aware of them.
 */
internal class InteropWrappingView(
    val areTouchesDelayed: Boolean
) : CMPInteropWrappingView(frame = CGRectZero.readValue()) {
    var actualAccessibilityContainer: Any? = null

    init {
        // required to properly clip the content of the wrapping view in case interop unclipped
        // bounds are larger than clipped bounds
        clipsToBounds = true
    }

    override fun accessibilityContainer(): Any? {
        return actualAccessibilityContainer
    }
}

internal val InteropViewSemanticsKey = AccessibilityKey<InteropWrappingView>(
    name = "InteropView",
    mergePolicy = { parentValue, childValue ->
        if (parentValue == null) {
            childValue
        } else {
            println(
                "Warning: Merging accessibility for multiple interop views is not supported. " +
                    "Multiple [UIKitView] are grouped under one node that should be represented as a single accessibility element." +
                    "It isn't recommended because the accessibility system can only recognize the first one. " +
                    "If you need multiple native views for accessibility, make sure to place them inside a single [UIKitView]."
            )

            parentValue
        }
    }
)

private var SemanticsPropertyReceiver.interopView by InteropViewSemanticsKey

/**
 * Chain [this] with [Modifier.semantics] that sets the [trackInteropPlacement] of the node
 * if [isNativeAccessibilityEnabled] is true. If [isNativeAccessibilityEnabled] is false, [this] is returned as is.
 */
internal fun Modifier.interopViewSemantics(isNativeAccessibilityEnabled: Boolean, interopViewGroup: InteropViewGroup) =
    if (isNativeAccessibilityEnabled) {
        this.semantics { interopView = interopViewGroup as InteropWrappingView }
    } else {
        this
    }