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

import androidx.compose.animation.core.*
import androidx.compose.ui.MotionDurationScale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.coroutines.withContext

internal class CupertinoFlingBehavior(
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
    val threshold: Float = 0.5f
) : FlingBehavior {
    private val animationSpec = CupertinoScrollDecayAnimationSpec(threshold)

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float =
        throw UnsupportedOperationException()

    override suspend fun ScrollScope.performFling(initialVelocity: Float, flingIntoOverscrollEffect: FlingIntoOverscrollEffect?): Float {
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f

                // If this value is not null by the end of decayAnimation, it means that not entire provided
                // delta was consumed during last animation frame, so the animation needs to be cancelled
                // and proper
                var unconsumedDeltaAfterDecay: Float? = null

                // There is an edge case when a user overscrolls and slightly flings in direction of content
                // but inertia is not enough to cover overscroll offset
                // In that scenario, don't do decay animation and simply replace it with FlingIntoOverscrollEffect animation immediately
                var needsDecayAnimation = true

                if (flingIntoOverscrollEffect != null) {
                    val targetValue = animationSpec.getTargetValue(0f, initialVelocity)

                    if (flingIntoOverscrollEffect.isFlingInertiaWeak(targetValue)) {
                        needsDecayAnimation = false
                        unconsumedDeltaAfterDecay = 0f
                    }
                }

                if (needsDecayAnimation) {
                    AnimationState(
                        initialValue = 0f,
                        initialVelocity = initialVelocity,
                    ).animateDecay(animationSpec.generateDecayAnimationSpec()) {
                        val delta = value - lastValue
                        val consumed = scrollBy(delta)
                        lastValue = value
                        velocityLeft = this.velocity

                        val unconsumedDelta = delta - consumed

                        // If some delta is not consumed, it means that fling hits into content bounds.
                        // Unconsumed delta and current velocity will be initial values for
                        // [flingIntoOverscrollEffect] animation to play if any, after we cancel decay animation
                        if (abs(unconsumedDelta) > threshold) {
                            unconsumedDeltaAfterDecay = unconsumedDelta
                            this.cancelAnimation()
                        }
                    }
                }

                val immutableUnconsumedDeltaAfterDecay = unconsumedDeltaAfterDecay

                if (immutableUnconsumedDeltaAfterDecay != null && flingIntoOverscrollEffect != null) {
                    flingIntoOverscrollEffect.performAnimation(
                        immutableUnconsumedDeltaAfterDecay,
                        velocityLeft
                    )

                    0f
                } else {
                    velocityLeft
                }
            } else {
                flingIntoOverscrollEffect?.let {
                    it.performAnimation(0f, 0f)
                    0f
                } ?: initialVelocity
            }
        }
    }
}

/*
 * Remark: all calculations inside are linear relative to initialValue and initialVelocity
 * so there is no need to include density
 */
private class CupertinoScrollDecayAnimationSpec(
    threshold: Float,
    private val decelerationRate: Float = 0.998f,
) : FloatDecayAnimationSpec {

    private val coefficient: Float = 1000f * ln(decelerationRate)

    override val absVelocityThreshold: Float = threshold

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