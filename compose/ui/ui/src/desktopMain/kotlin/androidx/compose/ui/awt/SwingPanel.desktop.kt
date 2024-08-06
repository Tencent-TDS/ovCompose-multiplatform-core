/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.compose.ui.awt

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.ComposeFeatureFlags
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.EmptyLayout
import androidx.compose.ui.layout.OverlayLayout
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.viewinterop.InteropView
import androidx.compose.ui.viewinterop.InteropViewGroup
import androidx.compose.ui.viewinterop.LocalInteropContainer
import androidx.compose.ui.viewinterop.SwingInteropViewHolder
import androidx.compose.ui.viewinterop.pointerInteropFilter
import androidx.compose.ui.viewinterop.trackInteropPlacement
import java.awt.Component
import java.awt.Container
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import javax.swing.JPanel
import javax.swing.LayoutFocusTraversalPolicy

val NoOpUpdate: Component.() -> Unit = {}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory] block will be called
 * to obtain the [Component] to be composed.
 *
 * By default, the Swing component is placed on top of the Compose layer (that means that Compose
 * content can't overlap or clip it). It might be changed by `compose.interop.blending` system
 * property. See [ComposeFeatureFlags.useInteropBlending].
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be re-executed to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
public fun <T : Component> SwingPanel(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {
    val interopContainer = LocalInteropContainer.current
    val compositeKeyHash = currentCompositeKeyHash

    val group = remember {
        SwingInteropViewGroup(
            key = compositeKeyHash,
            focusComponent = interopContainer.root
        )
    }

    val interopViewHolder = remember {
        SwingInteropViewHolder(
            factory = factory,
            container = interopContainer,
            group = group,
            compositeKeyHash = compositeKeyHash
        )
    }

    val focusManager = LocalFocusManager.current
    val focusSwitcher = remember { FocusSwitcher(group, focusManager) }

    InteropView(
        factory = {
            interopViewHolder
        },
        modifier = modifier,
        update = {
            it.background = background.toAwtColor()
            update(it)
        }
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(Unit) {
        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (e.isFocusGainedHandledBySwingPanel(interopViewHolder.group)) {
                    when (e.cause) {
                        FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
                        FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
                        else -> Unit
                    }
                }
            }

            override fun focusLost(e: FocusEvent) = Unit
        }
        interopContainer.root.addFocusListener(focusListener)
        onDispose {
            interopContainer.root.removeFocusListener(focusListener)
        }
    }
}

/**
 * Composes an AWT/Swing component obtained from [factory]. The [factory] block will be called
 * to obtain the [Component] to be composed.
 *
 * By default, the Swing component is placed on top of the Compose layer (that means that Compose
 * content can't overlap or clip it). It might be changed by `compose.interop.blending` system
 * property. See [ComposeFeatureFlags.useInteropBlending].
 *
 * The [update] block runs due to recomposition, this is the place to set [Component] properties
 * depending on state. When state changes, the block will be re-executed to set the new properties.
 *
 * @param background Background color of SwingPanel
 * @param factory The block creating the [Component] to be composed.
 * @param modifier The modifier to be applied to the layout.
 * @param update The callback to be invoked after the layout is inflated.
 */
@Composable
public fun <T : Component> SwingPanel2(
    background: Color = Color.White,
    factory: () -> T,
    modifier: Modifier = Modifier,
    update: (T) -> Unit = NoOpUpdate,
) {
    val interopContainer = LocalInteropContainer.current
    val focusManager = LocalFocusManager.current
    val compositeKeyHash = currentCompositeKeyHash

    val group = remember {
        SwingInteropViewGroup(
            key = compositeKeyHash,
            focusComponent = interopContainer.root
        )
    }

    val focusSwitcher = remember { FocusSwitcher(group, focusManager) }

    val backgroundColor by remember(background) {
        mutableStateOf(background.toAwtColor())
    }

    InteropView(
        factory = {
            SwingInteropViewHolder(
                factory = factory,
                container = interopContainer,
                group = group,
                compositeKeyHash = it
            )
        },
        modifier = modifier,
        onReset = null,
        onRelease = NoOpUpdate,
        update = {
            it.background = backgroundColor
            update(it)
        }
    ) {
        focusSwitcher.Content()
    }

    DisposableEffect(Unit) {
        val focusListener = object : FocusListener {
            override fun focusGained(e: FocusEvent) {
                if (e.isFocusGainedHandledBySwingPanel(group)) {
                    when (e.cause) {
                        FocusEvent.Cause.TRAVERSAL_FORWARD -> focusSwitcher.moveForward()
                        FocusEvent.Cause.TRAVERSAL_BACKWARD -> focusSwitcher.moveBackward()
                        else -> Unit
                    }
                }
            }

            override fun focusLost(e: FocusEvent) = Unit
        }
        interopContainer.root.addFocusListener(focusListener)
        onDispose {
            interopContainer.root.removeFocusListener(focusListener)
        }
    }
}

/**
 * Returns whether the event is handled by SwingPanel.
 *
 * The focus can be switched from the child component inside SwingPanel.
 * In that case, SwingPanel will take care of it.
 *
 * The alternative that needs more investigation is to
 * not use ComposePanel as next/previous focus element for SwingPanel children
 * (see [SwingInteropViewGroup.focusComponent])
 */
internal fun FocusEvent.isFocusGainedHandledBySwingPanel(container: Container) =
    container.isParentOf(oppositeComponent)

/**
 * A container for [SwingPanel]'s component. Takes care about focus and clipping.
 *
 * @param key The unique identifier for the panel container.
 * @param focusComponent The component that should receive focus.
 */
private class SwingInteropViewGroup(
    key: Int,
    private val focusComponent: Component
) : JPanel() {
    init {
        name = "SwingPanel #${key.toString(MaxSupportedRadix)}"
        layout = null
        focusTraversalPolicy = object : LayoutFocusTraversalPolicy() {
            override fun getComponentAfter(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                return if (aComponent == getLastComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentAfter(aContainer, aComponent)
                }
            }

            override fun getComponentBefore(
                aContainer: Container?,
                aComponent: Component?
            ): Component? {
                return if (aComponent == getFirstComponent(aContainer)) {
                    focusComponent
                } else {
                    super.getComponentBefore(aContainer, aComponent)
                }
            }
        }
        isFocusCycleRoot = true
    }
}

private class FocusSwitcher(
    private val group: InteropViewGroup,
    private val focusManager: FocusManager,
) {
    private val backwardTracker = FocusTracker {
        val component = group.focusTraversalPolicy.getFirstComponent(group)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_FORWARD)
        } else {
            moveForward()
        }
    }

    private val forwardTracker = FocusTracker {
        val component = group.focusTraversalPolicy.getLastComponent(group)
        if (component != null) {
            component.requestFocus(FocusEvent.Cause.TRAVERSAL_BACKWARD)
        } else {
            moveBackward()
        }
    }

    fun moveBackward() {
        backwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Previous)
    }

    fun moveForward() {
        forwardTracker.requestFocusWithoutEvent()
        focusManager.moveFocus(FocusDirection.Next)
    }

    @Composable
    fun Content() {
        EmptyLayout(backwardTracker.modifier)
        EmptyLayout(forwardTracker.modifier)
    }

    /**
     * A helper class that can help:
     * - to prevent recursive focus events
     *   (a case when we focus the same element inside `onFocusEvent`)
     * - to prevent triggering `onFocusEvent` while requesting focus somewhere else
     */
    private class FocusTracker(
        private val onNonRecursiveFocused: () -> Unit
    ) {
        private val requester = FocusRequester()

        private var isRequestingFocus = false
        private var isHandlingFocus = false

        fun requestFocusWithoutEvent() {
            try {
                isRequestingFocus = true
                requester.requestFocus()
            } finally {
                isRequestingFocus = false
            }
        }

        val modifier = Modifier
            .focusRequester(requester)
            .onFocusEvent {
                if (!isRequestingFocus && !isHandlingFocus && it.isFocused) {
                    try {
                        isHandlingFocus = true
                        onNonRecursiveFocused()
                    } finally {
                        isHandlingFocus = false
                    }
                }
            }
            .focusTarget()
    }
}

/**
 * The maximum radix available for conversion to and from strings.
 */
private val MaxSupportedRadix = 36
