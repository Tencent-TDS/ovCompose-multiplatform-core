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

package androidx.compose.animation

import androidx.compose.ui.geometry.Offset
import kotlin.math.*

sealed interface SpringSolution {
    val duration: Float

    fun valueAtTime(time: Float): Offset

    class CriticallyDamped(
        override val duration: Float,
        private val beta: Float,
        private val c1: Offset,
        private val c2: Offset
    ): SpringSolution {
        override fun valueAtTime(time: Float): Offset {
            val dampingOverTime = exp(-beta * time)
            return (c1 + c2 * time) * dampingOverTime
        }

        companion object {
            fun create(
                spring: Spring,
                displacement: Offset,
                initialVelocity: Offset,
                threshold: Float
            ): CriticallyDamped {
                val beta = spring.beta
                val c2 = initialVelocity + displacement * beta
                val duration = if (displacement.getDistanceSquared() == 0f && initialVelocity.getDistanceSquared() == 0f) {
                    0f
                } else {
                    val t1 = 1f / beta * ln(2f * displacement.getDistance() / threshold)
                    val t2 = 2f / beta * ln(4f * c2.getDistance() / (E.toFloat() * beta * threshold))
                    max(t1, t2)
                }

                return CriticallyDamped(duration, beta, displacement, c2)
            }
        }
    }
    class Underdamped(
        override val duration: Float,
        private val dampedNaturalFrequency: Float,
        private val beta: Float,
        private val c1: Offset,
        private val c2: Offset
    ): SpringSolution {
        override fun valueAtTime(time: Float): Offset {
            val dampingOverTime = exp(-beta * time)
            val phase = (dampedNaturalFrequency * time)

            return (c1 * cos(phase) + c2 * sin(phase)) * dampingOverTime
        }
        companion object {
            fun create(
                spring: Spring,
                displacement: Offset,
                initialVelocity: Offset,
                threshold: Float
            ): Underdamped {
                val c2 =
                    (initialVelocity + displacement * spring.beta) / spring.dampedNaturalFrequency

                val duration =
                    if (displacement.getDistanceSquared() == 0f && initialVelocity.getDistanceSquared() == 0f) {
                        0f
                    } else {
                        ln((displacement.getDistance() + c2.getDistance()) / threshold) / spring.beta
                    }

                return Underdamped(
                    spring.dampedNaturalFrequency,
                    duration,
                    spring.beta,
                    displacement,
                    c2
                )
            }
        }
    }

    companion object {
        fun create(spring: Spring, displacement: Offset, initialVelocity: Offset, threshold: Float): SpringSolution {
            val dampingRatio = spring.dampingRatio

            require(dampingRatio > 0f && dampingRatio <= 1f)

            return if (dampingRatio == 1f) {
                CriticallyDamped.create(spring, displacement, initialVelocity, threshold)
            } else {
                Underdamped.create(spring, displacement, initialVelocity, threshold)
            }
        }
    }
}

data class Spring(
    val mass: Float,
    val stiffness: Float,
    val dampingRatio: Float
) {
    val damping: Float
        get() = 2f * dampingRatio * sqrt(mass * stiffness)

    val beta: Float
        get() = damping / (2f * mass)

    val dampedNaturalFrequency: Float
        get() = sqrt(stiffness / mass) * sqrt(1f - dampingRatio * dampingRatio)

    companion object {
        val default = Spring(1f, 200f, 1f)
    }
}

data class DecelerationTimingParameters(
    val initialValue: Offset,
    val initialVelocity: Offset,
    val decelerationRate: Float,
    val threshold: Float
) {
    val destination: Offset
        get() = initialValue - initialVelocity / decelerationCoefficient

    val duration: Float
        get() =
            if (initialVelocity.getDistance() > 0f) {
                0f
            } else {
                ln(-decelerationCoefficient * threshold / initialVelocity.getDistance()) / decelerationCoefficient
            }

    fun valueAt(time: Float): Offset =
        initialValue + initialVelocity * (decelerationRate.pow(1000f * time) - 1f) / decelerationCoefficient

    fun durationTo(value: Offset): Float? =
        if (value.distanceToSegment(initialValue, destination) < threshold) {
            null
        } else {
            ln(1f + decelerationCoefficient * (value - initialValue).getDistance() / initialVelocity.getDistance()) / decelerationCoefficient
        }

    fun velocityAt(time: Float): Offset =
        initialVelocity * decelerationRate.pow(1000f * time)

    private val decelerationCoefficient: Float
        get() = 1000f * ln(decelerationRate)

    init {
        require(decelerationRate > 0f && decelerationRate < 1f)
    }
}