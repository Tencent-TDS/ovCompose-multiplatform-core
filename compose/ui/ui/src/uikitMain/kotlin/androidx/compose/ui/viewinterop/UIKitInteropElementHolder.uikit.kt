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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asCGSize
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toDpSize
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.unit.toSize
import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectIntersection
import platform.CoreGraphics.CGRectIsEmpty

internal abstract class UIKitInteropElementHolder<T : InteropView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropWrappingView,
    compositeKeyHash: Int
) : TypedInteropViewHolder<T>(
    factory = factory,
    interopContainer = interopContainer,
    group = group,
    compositeKeyHash = compositeKeyHash,
    measurePolicy = MeasurePolicy { _, constraints ->
        layout(constraints.minWidth, constraints.minHeight) {
            // No-op, no children are expected
            // TODO: attempt to calculate the size of the wrapped view using constraints
            //  and autolayout system if possible
            //  https://youtrack.jetbrains.com/issue/CMP-5873/iOS-investigate-intrinsic-sizing-of-interop-elements
        }
    }
) {
    constructor(
        factory: () -> T,
        interopContainer: InteropContainer,
        compositeKeyHash: Int,
    ) : this(
        factory = factory,
        interopContainer = interopContainer,
        group = InteropWrappingView(
            interactionMode = null
        ),
        compositeKeyHash = compositeKeyHash
    )

    private enum class VisibilityState {
        DETACHED,
        NOT_VISIBLE,
        APPEARING,
        VISIBLE
    }

    private var currentUnclippedRect: IntRect? = null
    private var currentClippedRect: IntRect? = null
    private var currentUserComponentRect: IntRect? = null

    /**
     * Immediate frame of underlying user component. Can be different from
     * [currentUserComponentRect] due to scheduling.
     */
    protected abstract var userComponentCGRect: CValue<CGRect>
    private var visibilityState = VisibilityState.NOT_VISIBLE

    /**
     * Will be down-cast from [platformDetails] to [UIKitInteropPlatformDetails] when the
     * platform details are set.
     */
    private var typedPlatformDetails: UIKitInteropPlatformDetails<T>? = null

    private val listener: UIKitInteropListener<T>?
        get() = typedPlatformDetails?.listener

    override fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        val rootCoordinates = layoutCoordinates.findRootCoordinates()

        val unclippedRect = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = false
            ).roundToIntRect()

        val clippedRect = rootCoordinates
            .localBoundingBoxOf(
                sourceCoordinates = layoutCoordinates,
                clipBounds = true
            ).roundToIntRect()

        if (currentUnclippedRect == unclippedRect && currentClippedRect == clippedRect) {
            return
        }

        // wrapping view itself is always using the clipped rect
        // don't issue a redundant update, if the clipped rect is the same
        if (clippedRect != currentClippedRect) {
            val groupFrame = clippedRect
                .toRect()
                .toDpRect(density)
                .asCGRect()

            container.scheduleUpdate {
                group.setFrame(groupFrame)
            }
        }

        // user component is always updated if the unclipped or clipped rect changes,
        // because it needs to be moved inside the clipping view to keep the frame
        // in window coordinates the same
        if (currentUnclippedRect != unclippedRect || currentClippedRect != clippedRect) {
            // offset to move the component to the correct position inside the wrapping view, so
            // its root space frame stays the same if the wrapping view is clipped

            val userComponentRect = IntRect(
                offset = unclippedRect.topLeft - clippedRect.topLeft,
                size = unclippedRect.size
            )

            // update the user component frame only if it changes
            if (userComponentRect != currentUserComponentRect) {
                // Schedule frame update
                val newUserComponentCGRect =
                    userComponentRect
                        .toRect()
                        .toDpRect(density)
                        .asCGRect()

                container.scheduleUpdate {
                    userComponentCGRect = newUserComponentCGRect
                }

                // Check if component size changed
                val newUserComponentSize =
                    if (userComponentRect.size != currentUserComponentRect?.size) {
                        userComponentRect.size
                    } else {
                        null
                    }

                val listener = listener
                // Schedule invoking callbacks with updated data
                if (listener != null && newUserComponentSize != null) {
                    val cgSize = newUserComponentSize
                        .toSize()
                        .toDpSize(density)
                        .asCGSize()

                    container.scheduleUpdate {
                        listener.onResize(typedInteropView, cgSize)
                    }
                }

                currentUserComponentRect = userComponentRect
            }
        }

        currentUnclippedRect = unclippedRect
        currentClippedRect = clippedRect

    }

    override fun dispatchToView(pointerEvent: PointerEvent) {
        // No-op, we can't dispatch events to UIView or UIViewController directly, see
        // [InteractionUIView] logic
    }

    /**
     * This logic is similar for both interop view and view controller holders.
     */
    override fun changeInteropViewIndex(root: InteropViewGroup, index: Int) {
        root.insertSubview(view = group, atIndex = index.toLong())
    }

    /**
     * Check that [group] doesn't entirely clip a child view with a [cgRect]
     */
    private fun isVisible(cgRect: CValue<CGRect>): Boolean =
        CGRectIsEmpty(
            CGRectIntersection(cgRect, group.bounds)
        ).not()

    private fun onWillAppear() {
        listener?.onWillAppear(typedInteropView)
    }

    private fun onDidAppear() {
        listener?.onDidAppear(typedInteropView)
    }

    private fun onWillDisappear() {
        listener?.onWillDisappear(typedInteropView)
    }

    private fun onDidDisappear() {
        listener?.onDidDisappear(typedInteropView)
    }

    protected fun insertInvokingVisibilityCallbacks(block: () -> Unit) {
        val isVisibleAfterBlockExecution = isVisible(userComponentCGRect)

        onWillAppear()
        block()

        if (isVisibleAfterBlockExecution) {
            onDidAppear()
            visibilityState = VisibilityState.VISIBLE
        } else {
            visibilityState = VisibilityState.APPEARING
        }
    }

    protected fun changeFrameInvokingVisibilityCallbacks(newFrame: CValue<CGRect>, block: () -> Unit) {
        val isVisibleAfterBlockExecution = isVisible(cgRect = newFrame)

        if (isVisibleAfterBlockExecution) {
            when (visibilityState) {
                VisibilityState.NOT_VISIBLE -> {
                    onWillAppear()
                }

                VisibilityState.APPEARING, VisibilityState.VISIBLE, VisibilityState.DETACHED -> {
                    // no-op
                }
            }
        } else {
            when (visibilityState) {
                VisibilityState.NOT_VISIBLE, VisibilityState.DETACHED -> {
                    // no-op
                }

                VisibilityState.VISIBLE, VisibilityState.APPEARING -> {
                    // In case of APPEARING the chain of event is this:
                    // onWillAppear -> onWillDisappear -> onDidDisappear
                    // which is by design and aligns with how UIViewControllers behave.
                    onWillDisappear()
                }
            }
        }

        block()

        if (isVisibleAfterBlockExecution) {
            when (visibilityState) {
                VisibilityState.NOT_VISIBLE, VisibilityState.APPEARING -> {
                    onDidAppear()
                    visibilityState = VisibilityState.VISIBLE
                }

                VisibilityState.VISIBLE, VisibilityState.DETACHED -> {
                    // no-op
                }
            }
        } else {
            when (visibilityState) {
                VisibilityState.NOT_VISIBLE, VisibilityState.DETACHED -> {
                    // no-op
                }

                VisibilityState.APPEARING, VisibilityState.VISIBLE -> {
                    onDidDisappear()
                    visibilityState = VisibilityState.NOT_VISIBLE
                }
            }
        }
    }

    protected fun removeInvokingVisibilityCallbacks(block: () -> Unit) {
        when (visibilityState) {
            VisibilityState.VISIBLE, VisibilityState.APPEARING -> {
                // In case of APPEARING the chain of event is this:
                // onWillAppear -> onWillDisappear -> onDidDisappear
                // which is by design and aligns with how UIViewControllers behave.

                onWillDisappear()
                block()
                onDidDisappear()
            }

            VisibilityState.NOT_VISIBLE -> {
                // View is already not visible, just remove it from the hierarchy
                block()
            }

            VisibilityState.DETACHED -> {
                // TODO: it should be an impossible states but happens because `unplace` can be
                //  called twice. When the changes are down-streamed, this needs to be uncommented
                //  to reinforce an invariant coming from this assumption
                // throw IllegalStateException()
            }
        }

        visibilityState = VisibilityState.DETACHED
    }

    override fun onPlatformDetailsChanged() {
        super.onPlatformDetailsChanged()

        typedPlatformDetails = platformDetails as UIKitInteropPlatformDetails<T>?

        val wrappingView = group as InteropWrappingView
        wrappingView.interactionMode = typedPlatformDetails?.properties?.interactionMode
    }
}