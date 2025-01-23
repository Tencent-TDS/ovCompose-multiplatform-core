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

package androidx.xr.compose.subspace.layout

import androidx.annotation.RestrictTo
import androidx.xr.compose.subspace.node.SubspaceModifierElement
import androidx.xr.runtime.math.Pose

/**
 * Moves a subspace element (i.e. currently only affects Jetpack XR Entity Panels/Volumes) in space.
 * The order of the [SubspaceModifier]s is important. Please take note of this when using movable.
 * If you have the following modifier chain: SubspaceModifier.offset().size().movable(), the
 * modifiers will work as expected. If instead you have this modifier chain:
 * SubspaceModifier.size().offset().movable(), you will experience unexpected placement behavior
 * when using the movable modifier. In general, the offset modifier should be specified before the
 * size modifier, and the movable modifier should be specified last.
 *
 * @param enabled - true if this composable should be movable.
 * @param stickyPose - if enabled, the user specified position will be retained when the modifier is
 *   disabled or removed.
 * @param onPoseChange - a callback to process the pose change during movement, with translation in
 *   pixels. This will only be called if [enabled] is true. If the callback returns false the
 *   default behavior of moving this composable's subspace hierarchy will be executed. If it returns
 *   true, it is the responsibility of the callback to process the event.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public fun SubspaceModifier.movable(
    enabled: Boolean = true,
    stickyPose: Boolean = false,
    onPoseChange: (Pose) -> Boolean = { false },
): SubspaceModifier = this.then(MovableElement(enabled, onPoseChange, stickyPose))

private class MovableElement(
    private val enabled: Boolean,
    private val onPoseChange: (Pose) -> Boolean,
    private val stickyPose: Boolean,
) : SubspaceModifierElement<MovableNode>() {

    override fun create(): MovableNode =
        MovableNode(enabled = enabled, stickyPose = stickyPose, onPoseChange = onPoseChange)

    override fun update(node: MovableNode) {
        node.enabled = enabled
        node.onPoseChange = onPoseChange
        node.stickyPose = stickyPose
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MovableElement) return false

        if (enabled != other.enabled) return false
        if (onPoseChange !== other.onPoseChange) return false
        if (stickyPose != other.stickyPose) return false

        return true
    }

    override fun hashCode(): Int {
        var result = enabled.hashCode()
        result = 31 * result + onPoseChange.hashCode()
        result = 31 * result + stickyPose.hashCode()
        return result
    }
}

internal class MovableNode(
    public var enabled: Boolean,
    public var stickyPose: Boolean,
    public var onPoseChange: (Pose) -> Boolean,
) : SubspaceModifier.Node()
