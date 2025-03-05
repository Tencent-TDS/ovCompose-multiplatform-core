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
package androidx.navigation3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND
import androidx.navigation3.NavDisplay.ENTER_TRANSITION_KEY
import androidx.navigation3.NavDisplay.EXIT_TRANSITION_KEY
import androidx.navigation3.NavDisplay.POP_ENTER_TRANSITION_KEY
import androidx.navigation3.NavDisplay.POP_EXIT_TRANSITION_KEY

/** Object that indicates the features that can be handled by the [NavDisplay] */
public object NavDisplay {
    /**
     * Function to be called on the [NavEntry.featureMap] to notify the [NavDisplay] that the
     * content should be animated using the provided transitions.
     */
    public fun transition(enter: EnterTransition?, exit: ExitTransition?): Map<String, Any> =
        if (enter == null || exit == null) emptyMap()
        else mapOf(ENTER_TRANSITION_KEY to enter, EXIT_TRANSITION_KEY to exit)

    /**
     * Function to be called on the [NavEntry.featureMap] to notify the [NavDisplay] that, when
     * popping from backstack, the content should be animated using the provided transitions.
     */
    public fun popTransition(enter: EnterTransition?, exit: ExitTransition?): Map<String, Any> =
        if (enter == null || exit == null) emptyMap()
        else mapOf(POP_ENTER_TRANSITION_KEY to enter, POP_EXIT_TRANSITION_KEY to exit)

    internal const val ENTER_TRANSITION_KEY = "enterTransition"
    internal const val EXIT_TRANSITION_KEY = "exitTransition"
    internal const val POP_ENTER_TRANSITION_KEY = "popEnterTransition"
    internal const val POP_EXIT_TRANSITION_KEY = "popExitTransition"
    internal const val DEFAULT_TRANSITION_DURATION_MILLISECOND = 700
}

/**
 * Display for Composable content that displays a single pane of content at a time, but can move
 * that content in and out with customized transitions.
 *
 * The NavDisplay displays the content associated with the last key on the back stack.
 *
 * @param backstack the collection of keys that represents the state that needs to be handled
 * @param localProviders list of [NavLocalProvider] to add information to the provided entriess
 * @param modifier the modifier to be applied to the layout.
 * @param contentAlignment The [Alignment] of the [AnimatedContent]
 * @param enterTransition Default [EnterTransition] when navigating to [NavEntry]s. Can be
 *   overridden individually for each [NavEntry] by passing in the entry's transitions through
 *   [NavEntry.featureMap].
 * @param exitTransition Default [ExitTransition] when navigating to [NavEntry]s. Can be overridden
 *   individually for each [NavEntry] by passing in the entry's transitions through
 *   [NavEntry.featureMap].
 * @param popEnterTransition Default [EnterTransition] when popping [NavEntry]s. Can be overridden
 *   individually for each [NavEntry] by passing in the entry's transitions through
 *   [NavEntry.featureMap].
 * @param popExitTransition Default [ExitTransition] when popping [NavEntry]s. Can be overridden
 *   individually for each [NavEntry] by passing in the entry's transitions through
 *   [NavEntry.featureMap].
 * @param onBack a callback for handling system back presses
 * @param entryProvider lambda used to construct each possible [NavEntry]
 * @sample androidx.navigation3.samples.BaseNav
 * @sample androidx.navigation3.samples.CustomBasicDisplay
 */
@Composable
public fun <T : Any> NavDisplay(
    backstack: List<T>,
    modifier: Modifier = Modifier,
    localProviders: List<NavLocalProvider> = emptyList(),
    contentAlignment: Alignment = Alignment.TopStart,
    sizeTransform: SizeTransform? = null,
    enterTransition: EnterTransition =
        fadeIn(
            animationSpec =
                tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                )
        ),
    exitTransition: ExitTransition =
        fadeOut(
            animationSpec =
                tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                )
        ),
    popEnterTransition: EnterTransition =
        fadeIn(
            animationSpec =
                tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                )
        ),
    popExitTransition: ExitTransition =
        fadeOut(
            animationSpec =
                tween(
                    DEFAULT_TRANSITION_DURATION_MILLISECOND,
                )
        ),
    onBack: () -> Unit = { if (backstack is MutableList) backstack.removeAt(backstack.size - 1) },
    entryProvider: (key: T) -> NavEntry<out T>
) {
    require(backstack.isNotEmpty()) { "NavDisplay backstack cannot be empty" }

    BackHandler(backstack.size > 1, onBack)
    NavBackStackProvider(backstack, localProviders, entryProvider) { entries ->
        // Make a copy shallow copy so that transition.currentState and transition.targetState are
        // different backstack instances. This ensures currentState reflects the old backstack when
        // the backstack (targetState) is updated.
        val newStack = backstack.toList()
        val entry = entries.last()

        val transition = updateTransition(targetState = newStack, label = newStack.toString())
        val isPop = isPop(transition.currentState, newStack)
        // Incoming entry defines transitions, otherwise it uses default transitions from
        // NavDisplay
        val finalEnterTransition =
            if (isPop) {
                entry.featureMap[POP_ENTER_TRANSITION_KEY] as? EnterTransition ?: popEnterTransition
            } else {
                entry.featureMap[ENTER_TRANSITION_KEY] as? EnterTransition ?: enterTransition
            }
        val finalExitTransition =
            if (isPop) {
                entry.featureMap[POP_EXIT_TRANSITION_KEY] as? ExitTransition ?: popExitTransition
            } else {
                entry.featureMap[EXIT_TRANSITION_KEY] as? ExitTransition ?: exitTransition
            }
        transition.AnimatedContent(
            modifier = modifier,
            transitionSpec = {
                ContentTransform(
                    targetContentEnter = finalEnterTransition,
                    initialContentExit = finalExitTransition,
                    sizeTransform = sizeTransform
                )
            },
            contentAlignment = contentAlignment,
            contentKey = { it.last() }
        ) { innerStack ->
            val lastKey = innerStack.last()
            entries.findLast { entry -> entry.key == lastKey }?.content?.invoke(lastKey)
        }
    }
}

private fun <T : Any> isPop(oldBackStack: List<T>, newBackStack: List<T>): Boolean {
    // entire stack replaced
    if (oldBackStack.first() != newBackStack.first()) return false
    // navigated
    if (newBackStack.size > oldBackStack.size) return false

    val divergingIndex =
        newBackStack.indices.firstOrNull { index -> newBackStack[index] != oldBackStack[index] }
    // if newBackStack never diverged from oldBackStack, then it is a clean subset of the oldStack
    // and is a pop
    return divergingIndex == null
}
