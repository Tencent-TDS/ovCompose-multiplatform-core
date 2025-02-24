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

package androidx.xr.runtime.internal

import androidx.annotation.RestrictTo
import androidx.xr.runtime.math.Pose
import androidx.xr.runtime.math.Vector3
import java.util.concurrent.Executor

/** Interface for an XR Runtime Entity. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface Entity : ActivityPose {

    /** Updates the pose (position and rotation) of the Entity relative to its parent. */
    public var pose: Pose

    /**
     * Sets the scale of this entity relative to its parent. This value will affect the rendering of
     * this Entity's children. As the scale increases, this will stretch the content of the Entity.
     *
     * @param scale The [Vector3] scale factor from the parent.
     */
    public var scale: Vector3

    /**
     * Add given Entity as child. The child Entity's pose will be relative to the pose of its parent
     *
     * @param child The child entity.
     */
    public fun addChild(child: Entity)

    /** Sets the provided Entities to be children of the Entity. */
    public var children: List<Entity>

    /**
     * Sets the parent Entity for this Entity. The child Entity's pose will be relative to the pose
     * of its parent.
     *
     * @param parent The parent entity.
     */
    public var parent: Entity?

    /** Sets context-text for this entity to be consumed by Accessibility systems. */
    public var contentDescription: String

    /**
     * Sets the size for the given Entity.
     *
     * @param dimensions Dimensions for the Entity in meters.
     */
    public var size: Dimensions

    /**
     * Sets the alpha transparency for the given Entity.
     *
     * @param alpha Alpha transparency level for the Entity.
     */
    public var alpha: Float

    /** Returns the total alpha transparency level for this Entity. */
    public val activitySpaceAlpha: Float

    /**
     * Sets the local hidden state of this Entity. When true, this Entity and all descendants will
     * not be rendered in the scene. When the hidden state is false, an entity will be rendered if
     * its ancestors are not hidden.
     *
     * @param hidden The new local hidden state of this Entity.
     */
    public var isHidden: Boolean

    /**
     * Returns the hidden status of this Entity.
     *
     * @param includeParents Whether to include the hidden status of parents in the returned value.
     * @return If includeParents is true, the returned value will be true if this Entity or any of
     *   its ancestors is hidden. If includeParents is false, the local hidden state is returned.
     *   Regardless of the local hidden state, an entity will not be rendered if any of its
     *   ancestors are hidden.
     */
    public fun isHidden(includeParents: Boolean): Boolean

    /**
     * Adds the listener to the set of active input listeners, for input events targeted to this
     * entity or its child entities.
     *
     * @param executor The executor to run the listener on.
     * @param listener The input event listener to add.
     */
    @Suppress("ExecutorRegistration")
    public fun addInputEventListener(executor: Executor, listener: InputEventListener)

    /** Removes the given listener from the set of active input listeners. */
    public fun removeInputEventListener(listener: InputEventListener)

    /**
     * Dispose any system resources held by this entity, and transitively calls dispose() on all the
     * children. Once disposed, Entity shouldn't be used again.
     */
    public fun dispose()

    /**
     * Add these components to entity.
     *
     * @param component Component to add to the Entity.
     * @return True if the given component is added to the Entity.
     */
    public fun addComponent(component: Component)

    /**
     * Remove the given component from the entity.
     *
     * @param component Component to remove from the entity.
     */
    public fun removeComponent(component: Component)

    /** Remove all components from this entity. */
    public fun removeAllComponents()
}
