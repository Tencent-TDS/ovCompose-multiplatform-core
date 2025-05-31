/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.runtime.BroadcastFrameClock
import androidx.compose.runtime.MonotonicFrameClock
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.Foundation.NSTimeInterval
import platform.QuartzCore.CADisplayLink
import platform.darwin.NSObject
import platform.darwin.sel_registerName
import kotlin.math.floor
import kotlin.math.roundToLong

internal fun NSTimeInterval.toNanoSeconds(): Long {
    // The calculation is split in two instead of
    // `(targetTimestamp * 1e9).toLong()`
    // to avoid losing precision for fractional part
    val integral = floor(this)
    val fractional = this - integral
    val secondsToNanos = 1_000_000_000L
    val nanos = integral.roundToLong() * secondsToNanos + (fractional * 1e9).roundToLong()
    return nanos
}


internal class DisplayLinkListener(private val trigger: () -> Unit = {}) {
    private var keyboardAnimationListener: CADisplayLink? = null

    private val _frameClock = BroadcastFrameClock()
    val frameClock: MonotonicFrameClock = _frameClock

    fun start() {
        keyboardAnimationListener = CADisplayLink.displayLinkWithTarget(
            target = object : NSObject() {
                @OptIn(BetaInteropApi::class)
                @Suppress("unused")
                @ObjCAction
                fun animationDidUpdate() {
                    _frameClock.sendFrame(
                        timeNanos = keyboardAnimationListener?.targetTimestamp?.toNanoSeconds() ?: 0
                    )
                    trigger()
                }
            },
            selector = sel_registerName("animationDidUpdate")
        )
        keyboardAnimationListener?.addToRunLoop(NSRunLoop.mainRunLoop, NSRunLoopCommonModes)
    }

    fun invalidate() {
        keyboardAnimationListener?.invalidate()
        keyboardAnimationListener = null
    }
}