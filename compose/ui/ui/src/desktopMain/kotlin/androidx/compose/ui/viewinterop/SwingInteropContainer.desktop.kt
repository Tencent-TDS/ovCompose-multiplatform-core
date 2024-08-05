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

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.scene.ComposeSceneMediator
import java.awt.Component
import org.jetbrains.skiko.ClipRectangle

/**
 * A container that controls interop views/components.
 *
 * It receives [root] native view to use it as parent for all interop views. It should be
 * the same component that is used in [ComposeSceneMediator] to avoid issues with transparency.
 *
 * @property root The Swing container to add the interop views to.
 * @property placeInteropAbove Whether to place interop components above non-interop components.
 */
internal class SwingInteropContainer(
    override val root: InteropViewGroup,
    private val placeInteropAbove: Boolean,
): InteropContainer {
    /**
     * Map to reverse-lookup of [InteropViewHolder] having an [InteropViewGroup].
     */
    private var interopComponents = mutableMapOf<InteropViewGroup, InteropViewHolder>()

    override var rootModifier: TrackInteropPlacementModifierNode? = null

    override val snapshotObserver: SnapshotStateObserver = SnapshotStateObserver { command ->
        command()
    }

    /**
     * Index of last interop component in [root].
     *
     * [ComposeSceneMediator] might keep extra components in the same container.
     * So based on [placeInteropAbove] they should go below or under all interop views.
     *
     * @see ComposeSceneMediator.contentComponent
     * @see ComposeSceneMediator.invisibleComponent
     */
    private val lastInteropIndex: Int
        get() {
            var lastInteropIndex = interopComponents.size - 1
            if (!placeInteropAbove) {
                val nonInteropComponents = root.componentCount - interopComponents.size
                lastInteropIndex += nonInteropComponents
            }
            return lastInteropIndex
        }

    override fun contains(holder: InteropViewHolder): Boolean =
        interopComponents.contains(holder.group)

    override fun place(holder: InteropViewHolder) {
        val component = holder.group

        if (interopComponents.isEmpty()) {
            snapshotObserver.start()
        }

        // Add this component to [interopComponents] to track count and clip rects
        val alreadyAdded = component in interopComponents
        if (!alreadyAdded) {
            interopComponents[component] = holder
        }

        // Iterate through a Compose layout tree in draw order and count interop view below this one
        val countBelow = countInteropComponentsBelow(holder)

        // AWT/Swing uses the **REVERSE ORDER** for drawing and events
        val awtIndex = lastInteropIndex - countBelow

        // Update AWT/Swing hierarchy
        if (alreadyAdded) {
            root.setComponentZOrder(component, awtIndex)
        } else {
            root.add(component, awtIndex)
        }

        // Sometimes Swing displays the rest of interop views in incorrect order after adding,
        // so we need to force re-validate it.
        root.validate()
        root.repaint()
    }

    override fun unplace(holder: InteropViewHolder) {
        val component = holder.group
        root.remove(component)
        interopComponents.remove(component)

        if (interopComponents.isEmpty()) {
            snapshotObserver.stop()
        }

        // Sometimes Swing displays the rest of interop views in incorrect order after removing,
        // so we need to force re-validate it.
        root.validate()
        root.repaint()
    }

    override fun update(action: () -> Unit) {
        action()
        root.validate()
        root.repaint()
    }

    fun getClipRectForComponent(component: Component): ClipRectangle =
        requireNotNull(interopComponents[component] as? ClipRectangle)

    @Composable
    operator fun invoke(content: @Composable () -> Unit) {
        CompositionLocalProvider(
            LocalInteropContainer provides this,
        ) {
            TrackInteropPlacementContainer(
                content = content
            )
        }
    }
}
