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

import android.graphics.Rect;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** Defines the launch parameters when creating an {@link ActivityPanel}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public final class ActivityPanelLaunchParameters {
    /** 2D window bounds in the host container window coordinates. */
    private final @NonNull Rect mWindowBounds;

    /**
     * Constructs an {@link ActivityPanelLaunchParameters} with the given initial window bounds.
     *
     * @param windowBounds the initial 2D window bounds of the panel, which will be the bounds of
     *     the Activity launched into the {@link ActivityPanel}.
     */
    public ActivityPanelLaunchParameters(@NonNull Rect windowBounds) {
        mWindowBounds = windowBounds;
    }

    /**
     * @return the initial 2D window bounds.
     */
    public @NonNull Rect getWindowBounds() {
        return mWindowBounds;
    }
}
