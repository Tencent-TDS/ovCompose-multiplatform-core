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

package androidx.compose.animation.core

import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow

/*
 * Remark: all calculations inside are linear relative to initialValue and initialVelocity
 * so there is no need to include density
 */
class CupertinoScrollDecayAnimationSpec(
    private val decelerationRate: Float = 0.998f
) : FloatDecayAnimationSpec {

    private val coefficient: Float = 1000f * ln(decelerationRate)

    override val absVelocityThreshold: Float = 0.5f

    override fun getTargetValue(initialValue: Float, initialVelocity: Float): Float =
        initialValue - initialVelocity / coefficient

    override fun getValueFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos)
        val initialVelocityOverTimeIntegral =
            (decelerationRate.pow(1000f * playTimeSeconds) - 1f) / coefficient * initialVelocity
        return initialValue + initialVelocityOverTimeIntegral
    }

    override fun getDurationNanos(initialValue: Float, initialVelocity: Float): Long {
        val absVelocity = abs(initialVelocity)

        if (absVelocity < absVelocityThreshold) {
            return 0
        }

        val seconds = ln(-coefficient * absVelocityThreshold / absVelocity) / coefficient

        return convertSecondsToNanos(seconds)
    }

    override fun getVelocityFromNanos(
        playTimeNanos: Long,
        initialValue: Float,
        initialVelocity: Float
    ): Float {
        val playTimeSeconds = convertNanosToSeconds(playTimeNanos)

        return initialVelocity * decelerationRate.pow(1000f * playTimeSeconds)
    }
}