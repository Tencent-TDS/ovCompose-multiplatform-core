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

import kotlin.math.*

/**
 * A sealed interface for solutions to the equations of motion for an IOSBasedSpring system.
 *
 * @property duration The duration of motion.
 */
sealed interface IOSBasedSpringSolution {
    val duration: Float

    val durationNanos: Long
        get() = (duration.toDouble() * SecondsToNanos).roundToLong()

    /**
     * Calculates the value of the system at a given time.
     *
     * @param time The time at which to evaluate the system.
     * @return The value of the system at the specified time.
     */
    fun valueAtTime(time: Float): Float

    fun valueAtTime(timeNanos: Long): Float =
        valueAtTime((timeNanos.toDouble() / SecondsToNanos).toFloat())

    /**
     * Calculates the velocity of the critically damped system at a given time.
     *
     * @param time The time at which to evaluate the system's velocity.
     * @return The velocity of the system at the specified time.
     */
    fun velocityAtTime(time: Float): Float

    fun velocityAtTime(timeNanos: Long): Float =
        velocityAtTime((timeNanos.toDouble() / SecondsToNanos).toFloat())

    /**
     * Represents a solution for a critically damped spring system. A system is critically damped when the damping ratio is equal to 1.
     *
     * @property duration The duration of motion.
     * @property beta value of [IOSBasedSpring.beta]
     * @property c1 The initial displacement of the spring.
     * @property c2 The initial velocity of the spring plus the initial displacement times beta.
     */
    class CriticallyDamped(
        override val duration: Float,
        private val beta: Float,
        private val c1: Float,
        private val c2: Float
    ) : IOSBasedSpringSolution {
        override fun valueAtTime(time: Float): Float {
            val dampingOverTime = exp(-beta * time)
            return (c1 + c2 * time) * dampingOverTime
        }

        override fun velocityAtTime(time: Float): Float {
            val dampingOverTime = exp(-beta * time)
            return (c2 - (c1 + c2 * time) * beta) * dampingOverTime
        }

        companion object {
            /**
             * Creates a new instance of a CriticallyDamped spring solution.
             *
             * @param spring The spring for which to solve.
             * @param displacement The initial displacement of the spring.
             * @param initialVelocity The initial velocity of the spring.
             * @param threshold The value that defines when the spring's motion has effectively stopped.
             * @return A CriticallyDamped solution for the given spring and initial conditions.
             */
            fun create(
                spring: IOSBasedSpring,
                displacement: Float,
                initialVelocity: Float,
                threshold: Float
            ): CriticallyDamped {
                val beta = spring.beta
                val c2 = initialVelocity + displacement * beta
                val duration = if (displacement == 0f && initialVelocity == 0f) {
                    0f
                } else {
                    val t1 = 1f / beta * ln(2f * displacement / threshold)
                    val t2 = 2f / beta * ln(4f * c2 / (E.toFloat() * beta * threshold))
                    max(t1, t2)
                }

                return CriticallyDamped(duration, beta, displacement, c2)
            }
        }
    }

    /**
     * Represents a solution for an underdamped spring system. A system is underdamped when the damping ratio is less than 1.
     *
     * @property duration The duration of motion.
     * @property dampedNaturalFrequency value of [IOSBasedSpring.dampedNaturalFrequency]
     * @property beta value of [IOSBasedSpring.beta]
     * @property c1 The initial displacement of the spring.
     * @property c2 The initial velocity of the spring plus the initial displacement times beta, divided by damped natural frequency.
     */
    class Underdamped(
        override val duration: Float,
        private val dampedNaturalFrequency: Float,
        private val beta: Float,
        private val c1: Float,
        private val c2: Float
    ) : IOSBasedSpringSolution {
        override fun valueAtTime(time: Float): Float {
            val dampingOverTime = exp(-beta * time)
            val phase = (dampedNaturalFrequency * time)

            return (c1 * cos(phase) + c2 * sin(phase)) * dampingOverTime
        }

        override fun velocityAtTime(time: Float): Float {
            val dampingOverTime = exp(-beta * time)
            val phase = dampedNaturalFrequency * time
            return (-c1 * phase * sin(phase) - c2 * phase * cos(phase) - (c1 * cos(phase) + c2 * sin(phase)) * beta) * dampingOverTime
        }

        companion object {
            /**
             * Creates a new instance of an Underdamped spring solution.
             *
             * @param spring The spring for which to solve.
             * @param displacement The initial displacement of the spring.
             * @param initialVelocity The initial velocity of the spring.
             * @param threshold The value that defines when the spring's motion has effectively stopped.
             * @return An Underdamped solution for the given spring and initial conditions.
             */
            fun create(
                spring: IOSBasedSpring,
                displacement: Float,
                initialVelocity: Float,
                threshold: Float
            ): Underdamped {
                val c2 =
                    (initialVelocity + displacement * spring.beta) / spring.dampedNaturalFrequency

                val duration =
                    if (displacement == 0f && initialVelocity == 0f) {
                        0f
                    } else {
                        ln((displacement + c2) / threshold) / spring.beta
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
        fun create(
            spring: IOSBasedSpring,
            displacement: Float,
            initialVelocity: Float,
            threshold: Float
        ): IOSBasedSpringSolution {
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

/**
 * A class representing an iOS-based spring. It can be used to model physical systems that can be described by Hooke's law,
 * or, more generally, for animations that require spring-like behavior.
 *
 * @property mass The mass attached to the end of the spring.
 * @property stiffness The spring constant, or the stiffness of the spring. It determines how much force the spring
 *         generates when it is stretched or compressed.
 * @property dampingRatio The damping ratio, which describes how oscillations in a system decay after a disturbance.
 */
data class IOSBasedSpring(
    val mass: Float,
    val stiffness: Float,
    val dampingRatio: Float
) {
    /**
     * The damping coefficient. In physical systems, damping is an influence that causes vibrations to decrease.
     */
    private val damping = 2f * dampingRatio * sqrt(mass * stiffness)

    /**
     * Beta is a term used in the solution of the second order differential equation that describes damped harmonic oscillation.
     * It is the damping coefficient divided by two times the mass.
     */
    val beta = damping / (2f * mass)

    /**
     * The damped natural frequency of the system. It's the frequency at which the system would oscillate if not for the damping.
     */
    val dampedNaturalFrequency = sqrt(stiffness / mass) * sqrt(1f - dampingRatio * dampingRatio)

    companion object {
        /**
         * A pre-configured spring with default properties of mass = 1.0, stiffness = 100.0, and dampingRatio = 1.0.
         */
        val default = IOSBasedSpring(1f, 100f, 1f)
    }
}