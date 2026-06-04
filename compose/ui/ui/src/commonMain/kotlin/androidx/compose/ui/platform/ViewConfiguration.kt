/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.internal.JvmDefaultWithCompatibility
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp

// region Tencent Code
interface TapCounter {

    var singleTapCount: Long

    var doubleTapCount: Long
}
// end region

/**
 * Contains methods to standard constants used in the UI for timeouts, sizes, and distances.
 */
@JvmDefaultWithCompatibility
interface ViewConfiguration {
    /**
     * The duration before a press turns into a long press.
     */
    val longPressTimeoutMillis: Long

    /**
     * The duration between the first tap's up event and the second tap's down
     * event for an interaction to be considered a double-tap.
     */
    val doubleTapTimeoutMillis: Long

    /**
     * The minimum duration between the first tap's up event and the second tap's down event for
     * an interaction to be considered a double-tap.
     */
    val doubleTapMinTimeMillis: Long

    /**
     * Distance in pixels a touch can wander before we think the user is scrolling.
     */
    val touchSlop: Float

    /**
     * Factor to determine the direction of the scroll, the larger the value, the stricter the direction of the gesture,
     * for example, 0 means no limit, 1 means equalization.
     */
    // region Tencent Code
    val touchDirectionFactor: Float get() = 0f
    // endregion

    /**
     * await drag on PointerEventPass.Initial or PointerEventPass.Main in draggable.
     */
    // region Tencent Code
    val awaitDragOnPointerEventPassInitial: Boolean get() = true
    // endregion

    /**
     * The minimum touch target size. If layout has reduced the pointer input bounds below this,
     * the touch target will be expanded evenly around the layout to ensure that it is at least
     * this big.
     */
    val minimumTouchTargetSize: DpSize
        get() = if (ComposeTabService.composeForceZeroTouchSizeEnable) DpSize.Zero else DpSize(48.dp, 48.dp)

    /**
     * The maximum velocity a fling have at any given time. This value should be in pixels/second.
     */
    val maximumFlingVelocity: Float get() = Float.MAX_VALUE

    // region Tencent Code
    /* Count click events for reporting  */
    val tapCounter: TapCounter? get() = null
    // end region
}

// region Tencent Code: This API is also published in JS API.
/**
 * Creates a copy of this [ViewConfiguration] with the specified property overrides.
 *
 * All parameters default to the current value of this configuration, so only the
 * properties you want to change need to be specified.
 *
 * Usage:
 * ```
 * val modified = configuration.copy(awaitDragOnPointerEventPassInitial = false)
 * CompositionLocalProvider(LocalViewConfiguration provides modified) { ... }
 * ```
 */
fun ViewConfiguration.copy(
    longPressTimeoutMillis: Long = this.longPressTimeoutMillis,
    doubleTapTimeoutMillis: Long = this.doubleTapTimeoutMillis,
    doubleTapMinTimeMillis: Long = this.doubleTapMinTimeMillis,
    touchSlop: Float = this.touchSlop,
    awaitDragOnPointerEventPassInitial: Boolean = this.awaitDragOnPointerEventPassInitial,
): ViewConfiguration = object : ViewConfiguration {
    override val longPressTimeoutMillis: Long = longPressTimeoutMillis
    override val doubleTapTimeoutMillis: Long = doubleTapTimeoutMillis
    override val doubleTapMinTimeMillis: Long = doubleTapMinTimeMillis
    override val touchSlop: Float = touchSlop
    override val awaitDragOnPointerEventPassInitial: Boolean = awaitDragOnPointerEventPassInitial
}
// endregion
