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

package androidx.compose.foundation

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize

@ExperimentalFoundationApi
@Stable
actual class MagnifierStyle internal actual constructor(
    internal actual val useTextDefault: Boolean,
    internal actual val size: DpSize,
    internal actual val cornerRadius: Dp,
    internal actual val elevation: Dp,
    internal actual val clippingEnabled: Boolean,
    internal actual val fishEyeEnabled: Boolean
) {
    @ExperimentalFoundationApi
    actual constructor(
        size: DpSize,
        cornerRadius: Dp,
        elevation: Dp,
        clippingEnabled: Boolean,
        fishEyeEnabled: Boolean
    ) : this(
        useTextDefault = false,
        size = size,
        cornerRadius = cornerRadius,
        elevation = elevation,
        clippingEnabled = clippingEnabled,
        fishEyeEnabled = fishEyeEnabled,
    )

    actual val isSupported: Boolean
        get() = isStyleSupported(this)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is MagnifierStyle) return false

        if (useTextDefault != other.useTextDefault) return false
        if (size != other.size) return false
        if (cornerRadius != other.cornerRadius) return false
        if (elevation != other.elevation) return false
        if (clippingEnabled != other.clippingEnabled) return false
        if (fishEyeEnabled != other.fishEyeEnabled) return false

        return true
    }

    override fun hashCode(): Int {
        var result = useTextDefault.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + cornerRadius.hashCode()
        result = 31 * result + elevation.hashCode()
        result = 31 * result + clippingEnabled.hashCode()
        result = 31 * result + fishEyeEnabled.hashCode()
        return result
    }

    override fun toString(): String {
        return if (useTextDefault) {
            "MagnifierStyle.TextDefault"
        } else {
            "MagnifierStyle(" +
                "size=$size, " +
                "cornerRadius=$cornerRadius, " +
                "elevation=$elevation, " +
                "clippingEnabled=$clippingEnabled, " +
                "fishEyeEnabled=$fishEyeEnabled" +
                ")"
        }
    }

    actual companion object {
        /** A [MagnifierStyle] with all default values. */
        @ExperimentalFoundationApi
        actual val Default : MagnifierStyle = MagnifierStyle()

        /**
         * A [MagnifierStyle] that uses the system defaults for text magnification.
         *
         * Different versions of Android may use different magnifier styles for magnifying text, so
         * using this configuration ensures that the correct style is used to match the system.
         */
        @ExperimentalFoundationApi
        actual val TextDefault = MagnifierStyle(
            useTextDefault = true,
            size = Default.size,
            cornerRadius = Default.cornerRadius,
            elevation = Default.elevation,
            clippingEnabled = Default.clippingEnabled,
            fishEyeEnabled = Default.fishEyeEnabled,
        )

        internal fun isStyleSupported(
            style: MagnifierStyle,
        ): Boolean {
            return if (!isPlatformMagnifierSupported()) {
                false
            } else {
                style == TextDefault || style == Default
            }
        }
    }
}

internal expect fun isPlatformMagnifierSupported() : Boolean
