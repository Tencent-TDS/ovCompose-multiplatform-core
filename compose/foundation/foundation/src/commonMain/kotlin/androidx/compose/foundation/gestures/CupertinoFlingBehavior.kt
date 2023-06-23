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
import androidx.compose.ui.geometry.Offset
import kotlin.math.abs
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.coroutines.withContext

internal class CupertinoFlingBehavior(
    density: Float,
    private val motionDurationScale: MotionDurationScale = DefaultScrollMotionDurationScale,
    val threshold: Float = 0.5f
) : FlingBehavior {
    var overscrollEffect: CupertinoOverscrollEffect? = null
    var getOffsetFromDelta: ((Float) -> Offset)? = null

    private val animationSpec = CupertinoScrollDecayAnimationSpec(threshold)
    fun Float.toOffset(): Offset {
        getOffsetFromDelta?.let {
            return it.invoke(this)
        } ?: throw Exception("CupertinoFlingBehavior.getOffsetFromDelta is null, should be set by owning entity")
    }

    override suspend fun ScrollScope.performFling(initialVelocity: Float): Float {
        // come up with the better threshold, but we need it since spline curve gives us NaNs

        return withContext(motionDurationScale) {
            if (abs(initialVelocity) > 1f) {
                var velocityLeft = initialVelocity
                var lastValue = 0f
                var unconsumedDeltaAfterDecay: Float? = null

                AnimationState(
                    initialValue = 0f,
                    initialVelocity = initialVelocity,
                ).animateDecay(animationSpec.generateDecayAnimationSpec()) {
                    val delta = value - lastValue
                    val consumed = scrollBy(delta)
                    lastValue = value
                    velocityLeft = this.velocity
                    // avoid rounding errors and stop if anything is unconsumed, remember it to
                    // start rubber band spring animation after scroll decay animation
//                    if (abs(delta - consumed) > 0.5f) this.cancelAnimation()
                    val unconsumedDelta = delta - consumed
                    if (abs(unconsumedDelta) > threshold) {
                        unconsumedDeltaAfterDecay = unconsumedDelta
                        this.cancelAnimation()
                    }
                }

                val constOverscrollEffect = overscrollEffect
                val constUnconsumedDeltaAfterDecay = unconsumedDeltaAfterDecay

                if (constUnconsumedDeltaAfterDecay != null && constOverscrollEffect != null) {
                    constOverscrollEffect.playSpringAnimation(constUnconsumedDeltaAfterDecay.toOffset(), velocityLeft.toOffset())

                    0f
                } else {
                    velocityLeft
                }
            } else {
                overscrollEffect?.let {
                    it.playSpringAnimation(Offset.Zero, Offset.Zero)
                    0f
                } ?: initialVelocity
            }
        }
    }
}

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