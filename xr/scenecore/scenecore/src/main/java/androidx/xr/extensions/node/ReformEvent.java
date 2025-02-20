/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.node;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** A reform (move / resize) event. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ReformEvent {
    // clang-format off
    /** The type of reform action this event is referring to. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                REFORM_TYPE_UNKNOWN,
                REFORM_TYPE_MOVE,
                REFORM_TYPE_RESIZE,
            })
    @Retention(SOURCE)
    @interface ReformType {}

    // clang-format on

    int REFORM_TYPE_UNKNOWN = 0;
    int REFORM_TYPE_MOVE = 1;
    int REFORM_TYPE_RESIZE = 2;

    /** Returns the type of reform action this event is referring to. */
    @ReformType
    int getType();

    // clang-format off
    /** The state of the reform action. */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                REFORM_STATE_UNKNOWN,
                REFORM_STATE_START,
                REFORM_STATE_ONGOING,
                REFORM_STATE_END,
            })
    @Retention(SOURCE)
    @interface ReformState {}

    // clang-format on

    int REFORM_STATE_UNKNOWN = 0;
    int REFORM_STATE_START = 1;
    int REFORM_STATE_ONGOING = 2;
    int REFORM_STATE_END = 3;

    /** Returns the state of the reform action. */
    @ReformState
    int getState();

    /** An identifier for this reform action. */
    int getId();

    /** The initial ray origin and direction, in task space. */
    @NonNull
    Vec3 getInitialRayOrigin();

    /** The initial ray direction, in task space. */
    @NonNull
    Vec3 getInitialRayDirection();

    /** The current ray origin and direction, in task space. */
    @NonNull
    Vec3 getCurrentRayOrigin();

    /** The current ray direction, in task space. */
    @NonNull
    Vec3 getCurrentRayDirection();

    /**
     * For a move event, the proposed pose of the node, in task space (or relative to the parent
     * node, if FLAG_POSE_RELATIVE_TO_PARENT was specified in the ReformOptions).
     */
    @NonNull
    Vec3 getProposedPosition();

    /**
     * For a move event, the proposed orientation of the node, in task space (or relative to the
     * parent node, if FLAG_POSE_RELATIVE_TO_PARENT was specified in the ReformOptions).
     */
    @NonNull
    Quatf getProposedOrientation();

    /** Scale will change with distance if ReformOptions.FLAG_SCALE_WITH_DISTANCE is set. */
    @NonNull
    Vec3 getProposedScale();

    /**
     * For a resize event, the proposed new size in meters. Note that in the initial implementation,
     * the Z size may not be modified.
     */
    @NonNull
    Vec3 getProposedSize();
}
