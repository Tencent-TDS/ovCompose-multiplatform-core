/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.material3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.internal.animateElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * This interface defines various elevation values for different interaction states, allowing for
 * visual feedback based on user actions or component status.
 *
 * In case the component is in multiple states at once, only the latest interaction value will
 * apply.
 */
sealed interface ComponentElevation {

    /** The default elevation of the component. */
    val elevation: Dp

    /** The elevation of the component when it is pressed. */
    val pressedElevation: Dp

    /** The elevation of the component when it is focused. */
    val focusedElevation: Dp

    /** The elevation of the component when the pointer is hovering over it. */
    val hoveredElevation: Dp

    /** The elevation of the component when it is being dragged. */
    val draggedElevation: Dp

    /** The elevation of the component when it is disabled. */
    val disabledElevation: Dp

    companion object {
        internal fun equals(componentElevation: ComponentElevation?, other: Any?): Boolean {
            if (componentElevation === other) return true
            if (
                componentElevation == null ||
                    other == null ||
                    other !is ComponentElevation ||
                    componentElevation::class != other::class
            ) {
                return false
            }

            return with(componentElevation) {
                elevation == other.elevation &&
                    pressedElevation == other.pressedElevation &&
                    focusedElevation == other.focusedElevation &&
                    hoveredElevation == other.hoveredElevation &&
                    draggedElevation == other.draggedElevation &&
                    disabledElevation == other.disabledElevation
            }
        }

        internal fun hashCode(component: ComponentElevation): Int {
            return with(component) {
                var result = elevation.hashCode()
                result = 31 * result + pressedElevation.hashCode()
                result = 31 * result + focusedElevation.hashCode()
                result = 31 * result + hoveredElevation.hashCode()
                result = 31 * result + disabledElevation.hashCode()
                result = 31 * result + draggedElevation.hashCode()
                result
            }
        }
    }
}

internal val ComponentElevation.hasShadows: Boolean
    get() {
        return elevation != 0.dp ||
            pressedElevation != 0.dp ||
            focusedElevation != 0.dp ||
            hoveredElevation != 0.dp ||
            disabledElevation != 0.dp ||
            draggedElevation != 0.dp
    }

/**
 * Represents the shadow elevation used in a component, depending on its [enabled] state and
 * [interactionSource].
 *
 * Shadow elevation is used to apply a shadow around the component to give it higher emphasis.
 *
 * @param enabled whether the card is enabled
 * @param interactionSource the [InteractionSource] for this component
 */
@Composable
internal fun ComponentElevation.shadowElevation(
    enabled: Boolean,
    interactionSource: InteractionSource?
): State<Dp> {
    if (interactionSource == null) {
        return remember { mutableStateOf(elevation) }
    }
    return animateElevation(enabled = enabled, interactionSource = interactionSource)
}

@Composable
internal fun ComponentElevation.animateElevation(
    enabled: Boolean,
    interactionSource: InteractionSource
): State<Dp> {
    val animatable =
        remember(interactionSource) {
            ElevationAnimatable(
                elevation = elevation,
                pressedElevation = pressedElevation,
                hoveredElevation = hoveredElevation,
                focusedElevation = focusedElevation,
                disabledElevation = disabledElevation,
                draggedElevation = draggedElevation,
                enabled = enabled,
            )
        }

    LaunchedEffect(this) {
        animatable.updateElevation(
            elevation = elevation,
            pressedElevation = pressedElevation,
            hoveredElevation = hoveredElevation,
            focusedElevation = focusedElevation,
            disabledElevation = disabledElevation,
            draggedElevation = draggedElevation,
            enabled = enabled,
        )
    }

    LaunchedEffect(interactionSource) {
        val interactions = mutableListOf<Interaction>()
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is HoverInteraction.Enter -> {
                    interactions.add(interaction)
                }
                is HoverInteraction.Exit -> {
                    interactions.remove(interaction.enter)
                }
                is FocusInteraction.Focus -> {
                    interactions.add(interaction)
                }
                is FocusInteraction.Unfocus -> {
                    interactions.remove(interaction.focus)
                }
                is PressInteraction.Press -> {
                    interactions.add(interaction)
                }
                is PressInteraction.Release -> {
                    interactions.remove(interaction.press)
                }
                is PressInteraction.Cancel -> {
                    interactions.remove(interaction.press)
                }
            }
            val targetInteraction = interactions.lastOrNull()
            launch { animatable.animateElevation(to = targetInteraction) }
        }
    }

    return animatable.asState()
}

internal class ElevationAnimatable(
    var elevation: Dp,
    var pressedElevation: Dp,
    var focusedElevation: Dp,
    var hoveredElevation: Dp,
    var draggedElevation: Dp,
    var disabledElevation: Dp,
    var enabled: Boolean
) {
    private val animatable = Animatable(elevation, Dp.VectorConverter)

    private var lastTargetInteraction: Interaction? = null
    private var targetInteraction: Interaction? = null

    private fun Interaction?.calculateTarget(): Dp {
        if (!enabled) {
            return disabledElevation
        }
        return when (this) {
            is PressInteraction.Press -> pressedElevation
            is HoverInteraction.Enter -> hoveredElevation
            is FocusInteraction.Focus -> focusedElevation
            is DragInteraction.Start -> draggedElevation
            else -> elevation
        }
    }

    suspend fun updateElevation(
        elevation: Dp,
        pressedElevation: Dp,
        hoveredElevation: Dp,
        focusedElevation: Dp,
        disabledElevation: Dp,
        draggedElevation: Dp,
        enabled: Boolean,
    ) {
        this.elevation = elevation
        this.pressedElevation = pressedElevation
        this.hoveredElevation = hoveredElevation
        this.focusedElevation = focusedElevation
        this.disabledElevation = disabledElevation
        this.draggedElevation = draggedElevation
        this.enabled = enabled
        snapElevation()
    }

    private suspend fun snapElevation() {
        val target = targetInteraction.calculateTarget()
        if (animatable.targetValue != target) {
            try {
                animatable.snapTo(target)
            } finally {
                lastTargetInteraction = targetInteraction
            }
        }
    }

    suspend fun animateElevation(to: Interaction?) {
        val target = to.calculateTarget()
        // Update the interaction even if the values are the same, for when we change to another
        // interaction later
        targetInteraction = to
        try {
            if (animatable.targetValue != target) {
                animatable.animateElevation(target = target, from = lastTargetInteraction, to = to)
            }
        } finally {
            lastTargetInteraction = to
        }
    }

    fun asState(): State<Dp> = animatable.asState()
}
