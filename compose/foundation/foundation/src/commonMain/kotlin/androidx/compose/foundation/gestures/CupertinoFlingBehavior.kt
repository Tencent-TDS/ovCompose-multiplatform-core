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
import androidx.compose.foundation.CupertinoOverscrollEffect
import androidx.compose.ui.MotionDurationScale
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.coroutines.withContext

internal class CupertinoFlingBehavior(
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
    val threshold: Float = 0.5f // Half pixel
) : FlingBehavior {
    var overscrollEffect: CupertinoOverscrollEffect? = null

    private val animationSpec = CupertinoScrollDecayAnimationSpec()

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f

                // If this value is not null by the end of decayAnimation, it means that not entire provided
                // delta was consumed during last animation frame, so the animation needs to be cancelled
                // and spring animation should be played
                var unconsumedDeltaAfterDecay: Float? = null

                // There is an edge case when a user overscrolls and slightly flings in direction of content
                // but inertia is not enough to cover overscroll offset
                // In that scenario, don't do decay animation and simply replace it with spring animation immediately
                var needsDecayAnimation = true

                val overscrollEffect = overscrollEffect

                if (overscrollEffect != null) {
                    val targetValue = animationSpec.getTargetValue(0f, initialVelocity)

                    if (overscrollEffect.isFlingInertiaWeak(targetValue)) {
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
                        // spring animation to play if any, after we cancel decay animation
                        if (abs(unconsumedDelta) > threshold) {
                            unconsumedDeltaAfterDecay = unconsumedDelta
                            this.cancelAnimation()
                        }
                    }
                }

                val immutableUnconsumedDeltaAfterDecay = unconsumedDeltaAfterDecay

                if (immutableUnconsumedDeltaAfterDecay != null && overscrollEffect != null) {
                    overscrollEffect.playSpringAnimation(
                        immutableUnconsumedDeltaAfterDecay,
                        velocityLeft
                    )

                    0f
                } else {
                    velocityLeft
                }
            } else {
                overscrollEffect?.let {
                    it.playSpringAnimation(0f, 0f)
                    0f
                } ?: initialVelocity
            }
        }
    }
}