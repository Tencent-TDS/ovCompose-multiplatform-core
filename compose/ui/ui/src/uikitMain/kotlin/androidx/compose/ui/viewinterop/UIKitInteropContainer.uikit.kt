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

import androidx.compose.runtime.snapshots.SnapshotStateObserver
import platform.UIKit.UIView
import platform.UIKit.UIViewController
import platform.UIKit.addChildViewController
import platform.UIKit.didMoveToParentViewController
import platform.UIKit.removeFromParentViewController
import platform.UIKit.willMoveToParentViewController

/**
 * A container that controls interop views/components.
 * It's using a modifier of [TrackInteropPlacementModifierNode] to properly sort native interop
 * elements and contains a logic for syncing changes to UIKit objects driven by Compose state
 * changes with Compose rendering.
 */
internal class UIKitInteropContainer(
    override val root: InteropViewGroup,
    val viewController: UIViewController,
    val requestRedraw: () -> Unit
) : InteropContainer {
    override var rootModifier: TrackInteropPlacementModifierNode? = null
    private var interopViews = mutableMapOf<InteropView, InteropViewHolder>()
    private var transaction = UIKitInteropMutableTransaction()

    // TODO: Android reuses `owner.snapshotObserver`. We should probably do the same with RootNodeOwner.
    /**
     * Snapshot observer that is used by underlying [InteropViewHolder] to observe changes in
     * Compose state and trigger changes in UIKit objects.
     * It starts observing when the first interop view is added and stops when the last one is
     * removed.
     */
    override val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    override fun contains(holder: InteropViewHolder): Boolean =
        interopViews.contains(holder.getInteropView())

    fun groupForInteropView(interopView: InteropView): InteropViewGroup? {
        val holder = interopViews[interopView] ?: return null
        return holder.group
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

        // snapshotObserver.stop() is not needed, because unplaceInteropView will be called
        // for all interop views and it will stop observing when the last one is removed.
    }

    /**
     * Return an object containing pending changes and reset internal storage
     */
    fun retrieveTransaction(): UIKitInteropTransaction {
        val result = transaction
        transaction = UIKitInteropMutableTransaction()
        return result
    }

    override fun place(holder: InteropViewHolder) {
        val interopView = checkNotNull(holder.getInteropView())

        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.BEGAN
            snapshotObserver.start()
        }
        interopViews[interopView] = holder

        val countBelow = countInteropComponentsBelow(holder).toLong()

        when (interopView) {
            is UIView -> update {
                root.insertSubview(view = holder.group, atIndex = countBelow)
            }

            is UIViewController -> update {
                val needsContainmentCalls = interopView.parentViewController == null
                if (needsContainmentCalls) {
                    interopView.willMoveToParentViewController(viewController)
                }

                root.insertSubview(view = holder.group, atIndex = countBelow)

                if (needsContainmentCalls) {
                    viewController.addChildViewController(interopView)
                }
            }

            else -> error("Unknown interop view type: $interopView")
        }
    }

    override fun unplace(holder: InteropViewHolder) {
        val interopView = requireNotNull(holder.getInteropView())

        interopViews.remove(interopView)
        if (interopViews.isEmpty()) {
            transaction.state = UIKitInteropState.ENDED
            snapshotObserver.stop()
        }


        when (interopView) {
            is UIView -> update {
                holder.group.removeFromSuperview()
            }

            is UIViewController -> update {
                interopView.willMoveToParentViewController(null)
                holder.group.removeFromSuperview()
                interopView.removeFromParentViewController()
            }

            else -> error("Unknown interop view type: $interopView")
        }
    }

    override fun update(action: () -> Unit) {
        requestRedraw()

        // Add lambda to a list of commands which will be executed later
        // in the same [CATransaction], when the next rendered Compose frame is presented.
        transaction.add(action)
    }
}
