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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBelow
import kotlinx.cinterop.CValue
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSLock
import platform.QuartzCore.CATransaction
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/**
 * Providing interop container as composition local, so [UIKitView]/[UIKitViewController] can use it
 * to add native views to the hierarchy.
 */
internal val LocalUIKitInteropContainer = staticCompositionLocalOf<UIKitInteropContainer> {
    error("UIKitInteropContainer not provided")
}


internal enum class UIKitInteropState {
    BEGAN, UNCHANGED, ENDED
}

internal enum class UIKitInteropViewHierarchyChange {
    VIEW_ADDED,
    VIEW_REMOVED
}

/**
 * Lambda containing changes to UIKit objects, which can be synchronized within [CATransaction]
 */
internal typealias UIKitInteropAction = () -> Unit

internal interface UIKitInteropTransaction {
    val actions: List<UIKitInteropAction>
    val state: UIKitInteropState
}

internal fun UIKitInteropTransaction.isEmpty() = actions.isEmpty() && state == UIKitInteropState.UNCHANGED
internal fun UIKitInteropTransaction.isNotEmpty() = !isEmpty()

private class UIKitInteropMutableTransaction: UIKitInteropTransaction {
    override val actions = mutableListOf<UIKitInteropAction>()
    override var state = UIKitInteropState.UNCHANGED
        set(value) {
            field = when (value) {
                UIKitInteropState.UNCHANGED -> error("Can't assign UNCHANGED value explicitly")
                UIKitInteropState.BEGAN -> {
                    when (field) {
                        UIKitInteropState.BEGAN -> error("Can't assign BEGAN twice in the same transaction")
                        UIKitInteropState.UNCHANGED -> value
                        UIKitInteropState.ENDED -> UIKitInteropState.UNCHANGED
                    }
                }
                UIKitInteropState.ENDED -> {
                    when (field) {
                        UIKitInteropState.BEGAN -> UIKitInteropState.UNCHANGED
                        UIKitInteropState.UNCHANGED -> value
                        UIKitInteropState.ENDED -> error("Can't assign ENDED twice in the same transaction")
                    }
                }
            }
        }
}

/**
 * A container that controls interop views/components.
 */
internal class UIKitInteropContainer(
    val requestRedraw: () -> Unit
): InteropContainer<UIView> {
    val containerView: UIView = UIKitInteropContainerView()
    override var rootModifier: TrackInteropModifierNode<UIView>? = null
    override var interopViews = mutableSetOf<UIView>()
        private set

    private val lock: NSLock = NSLock()
    private var transaction = UIKitInteropMutableTransaction()

    /**
     * Number of views, created by interop API and present in current view hierarchy
     */
    private var viewsCount = 0
        set(value) {
            require(value >= 0)

            field = value
        }

    /**
     * Dispose by immediately executing all UIKit interop actions that can't be deferred to be
     * synchronized with rendering because scene will never be rendered past that moment.
     */
    fun dispose() {
        val lastTransaction = retrieveTransaction()

        for (action in lastTransaction.actions) {
            action.invoke()
        }
    }

    /**
     * Add lambda to a list of commands which will be executed later in the same CATransaction, when the next rendered Compose frame is presented
     */
    fun deferAction(hierarchyChange: UIKitInteropViewHierarchyChange? = null, action: () -> Unit) {
        requestRedraw()

        lock.doLocked {
            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_ADDED) {
                if (viewsCount == 0) {
                    transaction.state = UIKitInteropState.BEGAN
                }
                viewsCount += 1
            }

            transaction.actions.add(action)

            if (hierarchyChange == UIKitInteropViewHierarchyChange.VIEW_REMOVED) {
                viewsCount -= 1
                if (viewsCount == 0) {
                    transaction.state = UIKitInteropState.ENDED
                }
            }
        }
    }

    /**
     * Return an object containing pending changes and reset internal storage
     */
    fun retrieveTransaction(): UIKitInteropTransaction =
        lock.doLocked {
            val result = transaction
            transaction = UIKitInteropMutableTransaction()
            result
        }

    override fun placeInteropView(nativeView: UIView) = deferAction {
        val index = countInteropComponentsBelow(nativeView)
        if (nativeView in interopViews) {
            // Place might be called multiple times
            nativeView.removeFromSuperview()
        } else {
            interopViews.add(nativeView)
        }
        containerView.insertSubview(nativeView, index.toLong())
    }

    override fun removeInteropView(nativeView: UIView) {
        nativeView.removeFromSuperview()
        interopViews.remove(nativeView)
    }
}

private class UIKitInteropContainerView: UIView(CGRectZero.readValue()) {
    /**
     * We used a simple solution to make only this view not touchable.
     * Another view added to this container will be touchable.
     */
    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? =
        super.hitTest(point, withEvent).takeIf {
            it != this
        }
}

/**
 * Modifier to track interop view inside [LayoutNode] hierarchy. Used to properly
 * sort interop views in the tree.
 *
 * @param view The [UIView] that matches the current node.
 */
internal fun Modifier.trackUIKitInterop(
    container: UIKitInteropContainer,
    view: UIView
): Modifier = this then TrackInteropModifierElement(
    container = container,
    nativeView = view
)

internal inline fun <T> NSLock.doLocked(block: () -> T): T {
    lock()

    try {
        return block()
    } finally {
        unlock()
    }
}
