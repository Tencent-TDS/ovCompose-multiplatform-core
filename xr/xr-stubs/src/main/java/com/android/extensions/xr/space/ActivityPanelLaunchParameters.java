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


/**
 * Defines the launch parameters when creating an {@link
 * com.android.extensions.xr.space.ActivityPanel ActivityPanel}.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public final class ActivityPanelLaunchParameters {

    /**
     * Constructs an {@link com.android.extensions.xr.space.ActivityPanelLaunchParameters
     * ActivityPanelLaunchParameters} with the given initial window bounds.
     *
     * @param windowBounds the initial 2D window bounds of the panel, which will be the bounds of
     *     the Activity launched into the {@link com.android.extensions.xr.space.ActivityPanel
     *     ActivityPanel}.
     */
    public ActivityPanelLaunchParameters(android.graphics.Rect windowBounds) {
        throw new RuntimeException("Stub!");
    }

    /**
     * @return the initial 2D window bounds.
     */
    public android.graphics.Rect getWindowBounds() {
        throw new RuntimeException("Stub!");
    }
}
