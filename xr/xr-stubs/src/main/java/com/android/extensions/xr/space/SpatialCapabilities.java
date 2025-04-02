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

package com.android.extensions.xr.space;


/** Represents a set of capabilities an activity has. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class SpatialCapabilities {

    public SpatialCapabilities() {
        throw new RuntimeException("Stub!");
    }

    /** Returns true if the capability is available. */
    public boolean get(int capability) {
        throw new RuntimeException("Stub!");
    }

    /** equals() */
    public boolean equals(java.lang.Object other) {
        throw new RuntimeException("Stub!");
    }

    /** hashCode() */
    public int hashCode() {
        throw new RuntimeException("Stub!");
    }

    /** toString() */
    public java.lang.String toString() {
        throw new RuntimeException("Stub!");
    }

    /** The activity can set its own environment. */
    public static final int APP_ENVIRONMENTS_CAPABLE = 3; // 0x3

    /** The activity can enable or disable passthrough. */
    public static final int PASSTHROUGH_CONTROL_CAPABLE = 2; // 0x2

    /**
     * The activity can create 3D contents.
     *
     * <p>This capability allows neither spatial panel creation nor spatial activity embedding.
     */
    public static final int SPATIAL_3D_CONTENTS_CAPABLE = 1; // 0x1

    /**
     * The activity can launch another activity on a spatial panel to spatially embed it.
     *
     * <p>This capability allows neither spatial panel creation nor 3D content creation.
     */
    public static final int SPATIAL_ACTIVITY_EMBEDDING_CAPABLE = 5; // 0x5

    /** The activity can use spatial audio. */
    public static final int SPATIAL_AUDIO_CAPABLE = 4; // 0x4

    /**
     * The activity can spatialize itself by adding a spatial panel.
     *
     * <p>This capability allows neither 3D content creation nor spatial activity embedding.
     */
    public static final int SPATIAL_UI_CAPABLE = 0; // 0x0
}
