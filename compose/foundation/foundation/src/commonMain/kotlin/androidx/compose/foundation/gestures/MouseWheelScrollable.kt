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

package androidx.compose.foundation.gestures

import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateTo
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.SuspendingPointerInputModifierNode
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.ObserverModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.util.fastForEach
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withTimeoutOrNull

internal class MouseWheelScrollNode(
    private val scrollingLogic: ScrollingLogic,
    private var _enabled: Boolean,
) : DelegatingNode(), CompositionLocalConsumerModifierNode, ObserverModifierNode {
    private lateinit var mouseWheelScrollConfig: ScrollConfig
    private lateinit var physics: ScrollPhysics

    override fun onAttach() {
        mouseWheelScrollConfig = platformScrollConfig()
        physics = if (mouseWheelScrollConfig.isSmoothScrollingEnabled) {
            AnimatedMouseWheelScrollPhysics(
                mouseWheelScrollConfig = mouseWheelScrollConfig,
                scrollingLogic = scrollingLogic,
                density = { currentValueOf(LocalDensity) }
            ).apply {
                coroutineScope.launch {
                    receiveMouseWheelEvents()
                }
            }
        } else {
            RawMouseWheelScrollPhysics(mouseWheelScrollConfig, scrollingLogic)
        }
    }

    // TODO(https://youtrack.jetbrains.com/issue/COMPOSE-731/Scrollable-doesnt-react-on-density-changes)
    //  it isn't called, because LocalDensity is staticCompositionLocalOf
    override fun onObservedReadsChanged() {
        physics.mouseWheelScrollConfig = mouseWheelScrollConfig
        physics.scrollingLogic = scrollingLogic
    }

    private val pointerInputNode = delegate(SuspendingPointerInputModifierNode {
        if (_enabled) {
            mouseWheelInput()
        }
    })

    var enabled
        get() = _enabled
        set(value) {
            if (_enabled != value) {
                _enabled = value
                pointerInputNode.resetPointerInputHandler()
            }
        }

    private suspend fun PointerInputScope.mouseWheelInput() = awaitPointerEventScope {
        while (true) {
            val event = awaitScrollEvent()
            if (!event.isConsumed) {
                val consumed = with(physics) { onMouseWheel(event) }
                if (consumed) {
                    event.consume()
                }
            }
        }
    }

    private suspend fun AwaitPointerEventScope.awaitScrollEvent(): PointerEvent {
        var event: PointerEvent
        do {
            event = awaitPointerEvent()
        } while (event.type != PointerEventType.Scroll)
        return event
    }

    private inline val PointerEvent.isConsumed: Boolean get() = changes.fastAny { it.isConsumed }
    private inline fun PointerEvent.consume() = changes.fastForEach { it.consume() }
}

private abstract class ScrollPhysics(
    var mouseWheelScrollConfig: ScrollConfig,
    var scrollingLogic: ScrollingLogic,
) {
    abstract fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean
}

private class RawMouseWheelScrollPhysics(
    mouseWheelScrollConfig: ScrollConfig,
    scrollingLogic: ScrollingLogic,
) : ScrollPhysics(mouseWheelScrollConfig, scrollingLogic) {
    override fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean {
        val delta = with(mouseWheelScrollConfig) {
            calculateMouseWheelScroll(pointerEvent, size)
        }
        return scrollingLogic.dispatchRawDelta(delta) != Offset.Zero
    }
}

private class AnimatedMouseWheelScrollPhysics(
    mouseWheelScrollConfig: ScrollConfig,
    scrollingLogic: ScrollingLogic,
    val density: () -> Density,
) : ScrollPhysics(mouseWheelScrollConfig, scrollingLogic) {
    private data class MouseWheelScrollDelta(
        val value: Offset,
        val shouldApplyImmediately: Boolean
    ) {
        operator fun plus(other: MouseWheelScrollDelta) = MouseWheelScrollDelta(
            value = value + other.value,
            shouldApplyImmediately = shouldApplyImmediately || other.shouldApplyImmediately
        )
    }
    private val channel = Channel<MouseWheelScrollDelta>(capacity = Channel.UNLIMITED)

    suspend fun receiveMouseWheelEvents() {
        while (coroutineContext.isActive) {
            val scrollDelta = channel.receive()
            val speed = with(density()) { AnimationSpeed.toPx() }
            scrollingLogic.dispatchMouseWheelScroll(scrollDelta, speed)
        }
    }

    private suspend fun ScrollableState.userScroll(
        block: suspend ScrollScope.() -> Unit
    ) = supervisorScope {
        // Run it in supervisorScope to ignore cancellations from scrolls with higher MutatePriority
        scroll(MutatePriority.UserInput, block)
    }

    override fun PointerInputScope.onMouseWheel(pointerEvent: PointerEvent): Boolean {
        val scrollDelta = with(mouseWheelScrollConfig) {
            calculateMouseWheelScroll(pointerEvent, size)
        }
        return if (scrollingLogic.canConsumeDelta(scrollDelta)) {
            channel.trySend(MouseWheelScrollDelta(
                value = scrollDelta,

                // In case of high-resolution wheel, such as a freely rotating wheel with
                // no notches or trackpads, delta should apply immediately, without any delays.
                shouldApplyImmediately = mouseWheelScrollConfig.isPreciseWheelScroll(pointerEvent)
            )).isSuccess
        } else false
    }

    private fun Channel<MouseWheelScrollDelta>.sumOrNull() =
        untilNull { tryReceive().getOrNull() }
            .toList()
            .reduceOrNull { accumulator, it -> accumulator + it }

    private fun <E> untilNull(builderAction: () -> E?) = sequence<E> {
        do {
            val element = builderAction()?.also {
                yield(it)
            }
        } while (element != null)
    }

    private fun ScrollingLogic.canConsumeDelta(scrollDelta: Offset): Boolean {
        val delta = scrollDelta.reverseIfNeeded().toFloat() // Use only current axis
        return if (delta == 0f) {
            false // It means that it's for another axis and cannot be consumed
        } else if (delta > 0f) {
            scrollableState.canScrollForward
        } else {
            scrollableState.canScrollBackward
        }
    }

    private suspend fun ScrollingLogic.dispatchMouseWheelScroll(
        scrollDelta: MouseWheelScrollDelta,
        speed: Float, // px / ms
    ) {
        var targetScrollDelta = scrollDelta
        // Sum delta from all pending events to avoid multiple animation restarts.
        channel.sumOrNull()?.let {
            targetScrollDelta += it
        }
        var targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()
        if (targetValue.isLowScrollingDelta()) {
            return
        }
        var animationState = AnimationState(0f)

        /*
         * TODO Handle real down/up events from touchpad to set isScrollInProgress correctly.
         *  Touchpads emit just multiple mouse wheel events, so detecting start and end of this
         *  "gesture" is not straight forward.
         *  Ideally it should be resolved by catching real touches from input device instead of
         *  waiting the next event with timeout (before resetting progress flag).
         */
        suspend fun waitNextScrollDelta(timeoutMillis: Long): Boolean {
            if (timeoutMillis < 0) return false
            return withTimeoutOrNull(timeoutMillis) {
                channel.receive()
            }?.let {
                targetScrollDelta = it
                targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()
                animationState = AnimationState(0f) // Reset previous animation leftover

                !targetValue.isLowScrollingDelta()
            } ?: false
        }

        scrollableState.userScroll {
            var requiredAnimation = true
            while (requiredAnimation) {
                requiredAnimation = false
                if (targetScrollDelta.shouldApplyImmediately) {
                    dispatchMouseWheelScroll(targetValue)
                    requiredAnimation = waitNextScrollDelta(ProgressTimeout)
                } else {
                    val durationMillis = (abs(targetValue - animationState.value) / speed)
                        .roundToInt()
                        .coerceAtMost(MaxAnimationDuration)
                    animateMouseWheelScroll(animationState, targetValue, durationMillis) { lastValue ->
                        // Sum delta from all pending events to avoid multiple animation restarts.
                        val nextScrollDelta = channel.sumOrNull()
                        if (nextScrollDelta != null) {
                            targetScrollDelta += nextScrollDelta
                            targetValue = targetScrollDelta.value.reverseIfNeeded().toFloat()

                            requiredAnimation = !(targetValue - lastValue).isLowScrollingDelta()
                        }
                        nextScrollDelta != null
                    }
                    requiredAnimation = waitNextScrollDelta(ProgressTimeout - durationMillis)
                }
            }
        }
    }

    private suspend fun ScrollScope.animateMouseWheelScroll(
        animationState: AnimationState<Float, AnimationVector1D>,
        targetValue: Float,
        durationMillis: Int,
        shouldCancelAnimation: (lastValue: Float) -> Boolean
    ) {
        var lastValue = animationState.value
        animationState.animateTo(
            targetValue,
            animationSpec = tween(
                durationMillis = durationMillis,
                easing = LinearEasing
            ),
            sequentialAnimation = true
        ) {
            val delta = value - lastValue
            if (!delta.isLowScrollingDelta()) {
                val consumedDelta = dispatchMouseWheelScroll(delta)
                if (!(delta - consumedDelta).isLowScrollingDelta()) {
                    cancelAnimation()
                    return@animateTo
                }
                lastValue += delta
            }
            if (shouldCancelAnimation(lastValue)) {
                cancelAnimation()
            }
        }
    }

    private fun ScrollScope.dispatchMouseWheelScroll(delta: Float) = with(scrollingLogic) {
        val offset = delta.reverseIfNeeded().toOffset()
        val consumed = dispatchScroll(offset, NestedScrollSource.Wheel)
        consumed.reverseIfNeeded().toFloat()
    }
}

/*
 * Returns true, if the value is too low for visible change in scroll (consumed delta, animation-based change, etc),
 * false otherwise
 */
private inline fun Float.isLowScrollingDelta(): Boolean = abs(this) < 0.5f

private val AnimationSpeed = 1.dp // dp / ms
private const val MaxAnimationDuration = 100 // ms
private const val ProgressTimeout = 100L // ms
