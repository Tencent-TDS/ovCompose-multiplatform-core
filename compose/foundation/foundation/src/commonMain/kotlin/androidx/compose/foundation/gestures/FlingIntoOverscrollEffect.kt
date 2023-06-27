/*
 * Copyright 2023 The Android Open Source Project
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

/**
 * Interface for defining an effect to be performed when flinging into overscroll.
 *
 * The [FlingIntoOverscrollEffect] provides two methods to check if the fling inertia is enough to completely negate
 * accumulated overscroll and perform animation based on the provided initial value and initial velocity. The effect
 * implemented by this interface should be used to provide visual feedback to the user when they fling beyond the
 * bounds of a scrollable, and overscroll occurs.
 */
interface FlingIntoOverscrollEffect {
    /**
     * Checks if the fling inertia is not enough to completely negate accumulated overscroll, so
     * default fling animation can be bypassed and [performAnimation] could be invoked immediately
     * to end up in a state, where accumulated overscroll value is zero
     *
     * @param targetValue The total distance to be passed by a fling relative to zero.
     * @return True if the fling inertia is weak to completely negate overscroll, false otherwise
     */
    fun isFlingInertiaWeak(targetValue: Float): Boolean

    /**
     * Performs animation based on the [initialValue] and [initialVelocity] provided.
     *
     * @param initialValue The initial value of the animation.
     * @param initialVelocity The initial velocity of the animation.
     *
     * @return Left velocity after animation is finished
     */
    suspend fun performAnimation(initialValue: Float, initialVelocity: Float): Float
}