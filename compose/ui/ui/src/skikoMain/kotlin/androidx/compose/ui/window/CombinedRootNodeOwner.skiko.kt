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

import androidx.compose.ui.ComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusOwner
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.platform.Platform
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toIntRect
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.util.fastForEachReversed
import kotlin.coroutines.CoroutineContext

internal class CombinedRootNodeOwner(
    scene: ComposeScene,
    platform: Platform,
    initDensity: Density,
    coroutineContext: CoroutineContext,
    initLayoutDirection: LayoutDirection,
    constraints: Constraints,
    onPointerUpdate: () -> Unit,
    modifier: Modifier = Modifier,
) : RootNodeOwner(
    scene = scene,
    platform = platform,
    initDensity = initDensity,
    coroutineContext = coroutineContext,
    initLayoutDirection = initLayoutDirection,
    constraints = constraints,
    focusable = true,
    onOutsidePointerEvent = null,
    onPointerUpdate = onPointerUpdate,
    modifier = modifier
) {
    /**
     * Contains attached registered [RootNodeOwner] in order of attachment.
     * This logic is used by accessibility.
     */
    internal val attachedOwners = mutableListOf<RootNodeOwner>()

    override var constraints = constraints
        set(value) {
            field = value
            forEachAttachedOwner { it.constraints = value }
            bounds = constraints.maxSize.toIntRect()
        }

    override val accessibilityControllers
        get() = attachedOwners.asReversed()
            .map { it.accessibilityController } + super.accessibilityControllers

    private var focusedOwner: RootNodeOwner = this
    private var gestureOwner: RootNodeOwner? = null
    private var lastHoverOwner: RootNodeOwner? = null

    private val _focusOwner = createFocusOwner(platform.focusManager)
    override val focusOwner = object : FocusOwner by _focusOwner {
        override fun takeFocus() {
            val focusOwner = attachedOwners.findLast { it.focusable }?.focusOwner ?: _focusOwner
            focusOwner.takeFocus()
        }

        override fun releaseFocus() {
            _focusOwner.releaseFocus()
            forEachAttachedOwner { it.focusOwner.releaseFocus() }
        }

        override fun moveFocus(focusDirection: FocusDirection): Boolean {
            val focusOwner = attachedOwners.lastOrNull()?.focusOwner ?: _focusOwner
            return focusOwner.moveFocus(focusDirection) ?: false
        }
    }

    /**
     * Find hovered owner for position of first pointer.
     */
    private fun hoveredOwner(event: PointerInputEvent): RootNodeOwner {
        val position = event.pointers.first().position
        return attachedOwners.lastOrNull { it.isInBounds(position) } ?: this
    }

    /**
     * Check if [focusedOwner] blocks input for this owner.
     */
    private fun isInteractive(owner: RootNodeOwner?) =
        when (owner) {
            null -> true
            this -> focusedOwner == this
            else -> attachedOwners.indexOf(focusedOwner) <= attachedOwners.indexOf(owner)
        }

    // Cache to reduce allocations
    private val listCopy = mutableListOf<RootNodeOwner>()
    private inline fun withOwnersCopy(
        includeSelf: Boolean = false,
        block: (List<RootNodeOwner>) -> Unit
    ) {
        // In case of recursive calls, allocate new list
        val owners = if (listCopy.isEmpty()) listCopy else mutableListOf()
        if (includeSelf) {
            owners.add(this)
        }
        owners.addAll(attachedOwners)
        try {
            block(owners)
        } finally {
            owners.clear()
        }
    }

    private inline fun forEachAttachedOwner(action: (RootNodeOwner) -> Unit) =
        withOwnersCopy {
            it.fastForEach(action)
        }

    private inline fun forEachOwnerReversed(action: (RootNodeOwner) -> Unit) =
        withOwnersCopy(includeSelf = true) {
            it.fastForEachReversed(action)
        }

    internal fun attach(owner: RootNodeOwner) {
        attachedOwners.add(owner)
        owner.requestLayout = this.requestLayout
        owner.requestDraw = this.requestDraw
        owner.dispatchSnapshotChanges = this.dispatchSnapshotChanges
        owner.constraints = this.constraints
        if (owner.focusable) {
            focusedOwner = owner

            // Exit event to lastHoverOwner will be sent via synthetic event on next frame
        }
    }

    internal fun detach(owner: RootNodeOwner) {
        attachedOwners.remove(owner)
        owner.dispatchSnapshotChanges = null
        owner.requestDraw = null
        owner.requestLayout = null
        if (owner == focusedOwner) {
            focusedOwner = attachedOwners.lastOrNull { it.focusable } ?: this

            // Enter event to new focusedOwner will be sent via synthetic event on next frame
        }
        if (owner == lastHoverOwner) {
            lastHoverOwner = null
        }
        if (owner == gestureOwner) {
            gestureOwner = null
        }
    }

    override fun hitTestInteropView(position: Offset): Boolean {
        // TODO:
        //  Temporary solution copying control flow from [processPress].
        //  A proper solution is to send touches to scene as black box
        //  and handle only that ones that were received in interop view
        //  instead of using [pointInside].
        attachedOwners.fastForEachReversed { owner ->
            if (owner.isInBounds(position)) {
                return owner.hitTestInteropView(position)
            } else if (owner == focusedOwner) {
                return false
            }
        }
        return super.hitTestInteropView(position)
    }

    override fun sendKeyEvent(keyEvent: KeyEvent): Boolean {
        return if (focusedOwner == this) {
            super.sendKeyEvent(keyEvent)
        } else {
            focusedOwner.sendKeyEvent(keyEvent)
        }
    }

    private fun forwardPointerInput(owner: RootNodeOwner?, event: PointerInputEvent) {
        if (owner == this) {
            super.processPointerInput(event)
        } else {
            owner?.processPointerInput(event)
        }
    }

    override fun processPointerInput(event: PointerInputEvent) {
        when (event.eventType) {
            PointerEventType.Press -> processPress(event)
            PointerEventType.Release -> processRelease(event)
            PointerEventType.Move -> processMove(event)
            PointerEventType.Enter -> processMove(event)
            PointerEventType.Exit -> processMove(event)
            PointerEventType.Scroll -> processScroll(event)
        }

        // Clean gestureOwner when there is no pressed pointers/buttons
        if (!event.isGestureInProgress) {
            gestureOwner = null
        }
    }

    private fun processPress(event: PointerInputEvent) {
        val currentGestureOwner = gestureOwner
        if (currentGestureOwner != null) {
            forwardPointerInput(currentGestureOwner, event)
            return
        }
        val position = event.pointers.first().position
        forEachOwnerReversed { owner ->

            // If the position of in bounds of the owner - send event to it and stop processing
            if (owner.isInBounds(position)) {
                forwardPointerInput(owner, event)
                gestureOwner = owner
                return
            }

            // Input event is out of bounds - send click outside notification
            owner.onOutsidePointerEvent?.invoke(event)

            // if the owner is in focus, do not pass the event to underlying owners
            if (owner == focusedOwner) {
                return
            }
        }
    }

    private fun processRelease(event: PointerInputEvent) {
        // Send Release to gestureOwner even if is not hovered or under focusedOwner
        forwardPointerInput(gestureOwner, event)
        if (!event.isGestureInProgress) {
            val owner = hoveredOwner(event)
            if (isInteractive(owner)) {
                processHover(event, owner)
            } else if (gestureOwner == null) {
                // If hovered owner is not interactive, then it means that
                // - It's not focusedOwner
                // - It placed under focusedOwner or not exist at all
                // In all these cases the even happened outside focused owner bounds
                focusedOwner.onOutsidePointerEvent?.invoke(event)
            }
        }
    }

    private fun processMove(event: PointerInputEvent) {
        var owner = when {
            // All touch events or mouse with pressed button(s)
            event.isGestureInProgress -> gestureOwner

            // Do not generate Enter and Move
            event.eventType == PointerEventType.Exit -> null

            // Find owner under mouse position
            else -> hoveredOwner(event)
        }

        // Even if the owner is not interactive, hover state still need to be updated
        if (!isInteractive(owner)) {
            owner = null
        }
        if (processHover(event, owner)) {
            return
        }
        forwardPointerInput(owner, event.copy(eventType = PointerEventType.Move))
    }

    /**
     * Updates hover state and generates [PointerEventType.Enter] and [PointerEventType.Exit]
     * events. Returns true if [event] is consumed.
     */
    private fun processHover(event: PointerInputEvent, owner: RootNodeOwner?): Boolean {
        if (event.pointers.fastAny { it.type != PointerType.Mouse }) {
            // Track hover only for mouse
            return false
        }
        // Cases:
        // - move from outside to the window (owner != null, lastMoveOwner == null): Enter
        // - move from the window to outside (owner == null, lastMoveOwner != null): Exit
        // - move from one point of the window to another (owner == lastMoveOwner): Move
        // - move from one popup to another (owner != lastMoveOwner): [Popup 1] Exit, [Popup 2] Enter
        if (owner == lastHoverOwner) {
            // Owner wasn't changed
            return false
        }
        forwardPointerInput(lastHoverOwner, event.copy(eventType = PointerEventType.Exit))
        forwardPointerInput(owner, event.copy(eventType = PointerEventType.Enter))
        lastHoverOwner = owner

        // Changing hovering state replaces Move event, so treat it as consumed
        return true
    }

    private fun processScroll(event: PointerInputEvent) {
        val owner = hoveredOwner(event)
        if (isInteractive(owner)) {
            forwardPointerInput(owner, event)
        }
    }

    override fun measureAndLayout(sendPointerUpdate: Boolean) {
        super.measureAndLayout(sendPointerUpdate)
        forEachAttachedOwner { it.measureAndLayout(sendPointerUpdate) }
    }

    override fun draw(canvas: org.jetbrains.skia.Canvas) {
        super.draw(canvas)
        forEachAttachedOwner { it.draw(canvas) }
    }

    override fun clearInvalidObservations() {
        super.clearInvalidObservations()
        forEachAttachedOwner { it.clearInvalidObservations() }
    }
}

private val PointerInputEvent.isGestureInProgress get() = pointers.fastAny { it.down }
