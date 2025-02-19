/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.foundation

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.LayoutCoordinates

/**
 * Defines an area of the UI component where gestures that start within this area will be consumed
 * by the component itself, preventing other components or the platform from consuming the gesture.
 *
 * This should be used to define areas where the component should explicitly handle gestures,
 * overriding any potential platform or other component behavior.
 */
public interface GestureInclusion {
    /**
     * Determines whether a gesture starting at the given offset should be consumed by this
     * component.
     *
     * @param offset The offset of the gesture within the component's layout.
     * @param layoutCoordinates The layout coordinates of the component.
     * @return `true` if the gesture should be consumed by this component, `false` otherwise.
     */
    public fun allowGesture(offset: Offset, layoutCoordinates: LayoutCoordinates): Boolean
}
