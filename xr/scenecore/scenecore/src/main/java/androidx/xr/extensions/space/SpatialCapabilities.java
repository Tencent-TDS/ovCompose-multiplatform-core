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

package androidx.xr.extensions.space;

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;

/** Represents a set of capabilities an activity has. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface SpatialCapabilities {
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            value = {
                SPATIAL_UI_CAPABLE,
                SPATIAL_3D_CONTENTS_CAPABLE,
                PASSTHROUGH_CONTROL_CAPABLE,
                APP_ENVIRONMENTS_CAPABLE,
                SPATIAL_AUDIO_CAPABLE,
                SPATIAL_ACTIVITY_EMBEDDING_CAPABLE
            })
    @Retention(SOURCE)
    @interface CapabilityType {}

    /**
     * The activity can spatialize itself by adding a spatial panel.
     *
     * <p>This capability allows neither 3D content creation nor spatial activity embedding.
     */
    int SPATIAL_UI_CAPABLE = 0;

    /**
     * The activity can create 3D contents.
     *
     * <p>This capability allows neither spatial panel creation nor spatial activity embedding.
     */
    int SPATIAL_3D_CONTENTS_CAPABLE = 1;

    /** The activity can enable or disable passthrough. */
    int PASSTHROUGH_CONTROL_CAPABLE = 2;

    /** The activity can set its own environment. */
    int APP_ENVIRONMENTS_CAPABLE = 3;

    /** The activity can use spatial audio. */
    int SPATIAL_AUDIO_CAPABLE = 4;

    /**
     * The activity can launch another activity on a spatial panel to spatially embed it.
     *
     * <p>This capability allows neither spatial panel creation nor 3D content creation.
     */
    int SPATIAL_ACTIVITY_EMBEDDING_CAPABLE = 5;

    /** Returns true if the capability is available. */
    default boolean get(@CapabilityType int capability) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
