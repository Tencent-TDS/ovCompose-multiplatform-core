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

package androidx.compose.foundation.gestures

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.OverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable

/**
 * Interface to specify fling behavior.
 *
 * When drag has ended with velocity in [scrollable], [performFling] is invoked to perform fling
 * animation and update state via [ScrollScope.scrollBy]
 */
@Stable
interface FlingBehavior {
    /**
     * Performs a fling with the given initial velocity.
     *
     * @param initialVelocity velocity available for fling in the orientation specified in
     * [androidx.compose.foundation.gestures.scrollable] that invoked this method.
     * @param flingIntoOverscrollEffect The effect containing the logic executed
     * if the fling inertial motion ends up in overscroll.
     * @return remaining velocity after fling operation has ended
     */
    suspend fun ScrollScope.performFling(initialVelocity: Float, flingIntoOverscrollEffect: FlingIntoOverscrollEffect?): Float
}

@Composable
internal expect fun rememberFlingBehavior(): FlingBehavior