/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

@file:Suppress("NOTHING_TO_INLINE")

package androidx.compose.ui.unit

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.util.lerp
import androidx.compose.ui.util.packInts
import androidx.compose.ui.util.unpackInt1
import androidx.compose.ui.util.unpackInt2
import kotlin.jvm.JvmInline
import kotlin.math.absoluteValue
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Packs two Short values into one Int value for use in inline classes.
 */
private inline fun packShorts(val1: Int, val2: Int): Int {
    requireShort(val1)
    requireShort(val2)
    return val1.shl(16) or (val2 and 0xFFFF)
}

/**
 * Unpacks the first Short value in [packShorts] from its returned UInt.
 */
private inline fun unpackShort1(value: Int): Int {
    return value.shr(16).toShort().toInt()
}

/**
 * Unpacks the second Short value in [packShorts] from its returned UInt.
 */
private inline fun unpackShort2(value: Int): Int {
    return value.and(0xFFFF).toShort().toInt()
}

private val SHORT_RANGE = Short.MIN_VALUE..Short.MAX_VALUE

private inline fun requireShort(value: Int) = require(value in SHORT_RANGE) {
    "Int $value overflows as Short"
}

fun ShortRect(left: Int, top: Int, right: Int, bottom: Int) =
    ShortRect(packInts(packShorts(left, top), packShorts(right, bottom)))

/**
 * An immutable, 2D, axis-aligned, short bounds rectangle whose coordinates are relative
 * to a given origin.
 */
@JvmInline
@Immutable
value class ShortRect internal constructor(private val packedValue: Long) {

    @Stable
    val left: Int
        get() = unpackShort1(unpackInt1(packedValue))

    /**
     * The vertical aspect of the position in [Int] pixels.
     */
    @Stable
    val top: Int
        get() = unpackShort2(unpackInt1(packedValue))

    @Stable
    val right: Int
        get() = unpackShort1(unpackInt2(packedValue))

    @Stable
    val bottom: Int
        get() = unpackShort2(unpackInt2(packedValue))

    fun copy(
        left: Int = this.left,
        top: Int = this.top,
        right: Int = this.right,
        bottom: Int = this.bottom
    ) = ShortRect(left, top, right, bottom)

    companion object {

        /** A rectangle with left, top, right, and bottom edges all at zero. */
        @Stable
        val Zero: ShortRect = ShortRect(0, 0, 0, 0)
    }

    /** The distance between the left and right edges of this rectangle. */
    @Stable
    val width: Int
        get() {
            return right - left
        }

    /** The distance between the top and bottom edges of this rectangle. */
    @Stable
    val height: Int
        get() {
            return bottom - top
        }

    /**
     * The distance between the upper-left corner and the lower-right corner of
     * this rectangle.
     */
    @Stable
    val size: IntSize
        get() = IntSize(width, height)

    /**
     * Whether this rectangle encloses a non-zero area. Negative areas are
     * considered empty.
     */
    @Stable
    val isEmpty: Boolean
        get() = left >= right || top >= bottom

    /**
     * Returns a new rectangle translated by the given offset.
     *
     * To translate a rectangle by separate x and y components rather than by an
     * [Offset], consider [translate].
     */
    @Stable
    fun translate(offset: IntOffset): ShortRect {
        return ShortRect(left + offset.x, top + offset.y, right + offset.x, bottom + offset.y)
    }

    /**
     * Returns a new rectangle with translateX added to the x components and
     * translateY added to the y components.
     */
    @Stable
    fun translate(translateX: Int, translateY: Int): ShortRect {
        return ShortRect(
            left + translateX,
            top + translateY,
            right + translateX,
            bottom + translateY
        )
    }

    /** Returns a new rectangle with edges moved outwards by the given delta. */
    @Stable
    fun inflate(delta: Int): ShortRect {
        return ShortRect(left - delta, top - delta, right + delta, bottom + delta)
    }

    /** Returns a new rectangle with edges moved inwards by the given delta. */
    @Stable
    fun deflate(delta: Int): ShortRect = inflate(-delta)

    /**
     * Returns a new rectangle that is the intersection of the given
     * rectangle and this rectangle. The two rectangles must overlap
     * for this to be meaningful. If the two rectangles do not overlap,
     * then the resulting ShortRect will have a negative width or height.
     */
    @Stable
    fun intersect(other: ShortRect): ShortRect {
        return ShortRect(
            max(left, other.left),
            max(top, other.top),
            min(right, other.right),
            min(bottom, other.bottom)
        )
    }

    /** Whether `other` has a nonzero area of overlap with this rectangle. */
    fun overlaps(other: ShortRect): Boolean {
        if (right <= other.left || other.right <= left)
            return false
        if (bottom <= other.top || other.bottom <= top)
            return false
        return true
    }

    /**
     * The lesser of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val minDimension: Int
        get() = min(width.absoluteValue, height.absoluteValue)

    /**
     * The greater of the magnitudes of the [width] and the [height] of this
     * rectangle.
     */
    val maxDimension: Int
        get() = max(width.absoluteValue, height.absoluteValue)

    /**
     * The offset to the intersection of the top and left edges of this rectangle.
     */
    val topLeft: IntOffset
        get() = IntOffset(left, top)

    /**
     * The offset to the center of the top edge of this rectangle.
     */
    val topCenter: IntOffset
        get() = IntOffset(left + width / 2, top)

    /**
     * The offset to the intersection of the top and right edges of this rectangle.
     */
    val topRight: IntOffset
        get() = IntOffset(right, top)

    /**
     * The offset to the center of the left edge of this rectangle.
     */
    val centerLeft: IntOffset
        get() = IntOffset(left, top + height / 2)

    /**
     * The offset to the point halfway between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * See also [androidx.compose.ui.unit.center].
     */
    val center: IntOffset
        get() = IntOffset(left + width / 2, top + height / 2)

    /**
     * The offset to the center of the right edge of this rectangle.
     */
    val centerRight: IntOffset
        get() = IntOffset(right, top + height / 2)

    /**
     * The offset to the intersection of the bottom and left edges of this rectangle.
     */
    val bottomLeft: IntOffset
        get() = IntOffset(left, bottom)

    /**
     * The offset to the center of the bottom edge of this rectangle.
     */
    val bottomCenter: IntOffset
        get() {
            return IntOffset(left + width / 2, bottom)
        }

    /**
     * The offset to the intersection of the bottom and right edges of this rectangle.
     */
    val bottomRight: IntOffset
        get() {
            return IntOffset(right, bottom)
        }

    /**
     * Whether the point specified by the given offset (which is assumed to be
     * relative to the origin) lies between the left and right and the top and
     * bottom edges of this rectangle.
     *
     * Rectangles include their top and left edges but exclude their bottom and
     * right edges.
     */
    operator fun contains(offset: IntOffset): Boolean {
        return offset.x >= left && offset.x < right && offset.y >= top && offset.y < bottom
    }

    override fun toString() = "ShortRect.fromLTRB(" +
            "$left, " +
            "$top, " +
            "$right, " +
            "$bottom)"
}

/**
 * Construct a rectangle from its left and top edges as well as its width and height.
 * @param offset Offset to represent the top and left parameters of the Rect
 * @param size Size to determine the width and height of this [ShortRect].
 * @return Rect with [ShortRect.left] and [ShortRect.top] configured to [IntOffset.x] and
 * [IntOffset.y] as [ShortRect.right] and [ShortRect.bottom] to [IntOffset.x] + [IntSize.width] and
 * [IntOffset.y] + [IntSize.height] respectively
 */
@Stable
fun ShortRect(offset: IntOffset, size: IntSize) =
    ShortRect(
        left = offset.x,
        top = offset.y,
        right = offset.x + size.width,
        bottom = offset.y + size.height
    )

/**
 * Construct the smallest rectangle that encloses the given offsets, treating
 * them as vectors from the origin.
 * @param topLeft Offset representing the left and top edges of the rectangle
 * @param bottomRight Offset representing the bottom and right edges of the rectangle
 */
@Stable
fun ShortRect(topLeft: IntOffset, bottomRight: IntOffset): ShortRect =
    ShortRect(
        topLeft.x,
        topLeft.y,
        bottomRight.x,
        bottomRight.y
    )

/**
 * Construct a rectangle that bounds the given circle
 * @param center Offset that represents the center of the circle
 * @param radius Radius of the circle to enclose
 */
@Stable
fun ShortRect(center: IntOffset, radius: Int): ShortRect =
    ShortRect(
        center.x - radius,
        center.y - radius,
        center.x + radius,
        center.y + radius
    )

/**
 * Linearly interpolate between two rectangles.
 *
 * The [fraction] argument represents position on the timeline, with 0.0 meaning
 * that the interpolation has not started, returning [start] (or something
 * equivalent to [start]), 1.0 meaning that the interpolation has finished,
 * returning [stop] (or something equivalent to [stop]), and values in between
 * meaning that the interpolation is at the relevant point on the timeline
 * between [start] and [stop]. The interpolation can be extrapolated beyond 0.0 and
 * 1.0, so negative values and values greater than 1.0 are valid (and can
 * easily be generated by curves).
 *
 * Values for [fraction] are usually obtained from an [Animation<Float>], such as
 * an `AnimationController`.
 */
@Stable
fun lerp(start: ShortRect, stop: ShortRect, fraction: Float): ShortRect {
    return ShortRect(
        lerp(start.left, stop.left, fraction),
        lerp(start.top, stop.top, fraction),
        lerp(start.right, stop.right, fraction),
        lerp(start.bottom, stop.bottom, fraction)
    )
}

/**
 * Converts an [ShortRect] to a [Rect]
 */
@Stable
fun ShortRect.toRect(): Rect = Rect(
    left = left.toFloat(),
    top = top.toFloat(),
    right = right.toFloat(),
    bottom = bottom.toFloat()
)

/**
 * Rounds a [Rect] to an [ShortRect]
 */
@Stable
fun Rect.roundToShortRect(): ShortRect = ShortRect(
    left = left.roundToInt(),
    top = top.roundToInt(),
    right = right.roundToInt(),
    bottom = bottom.roundToInt()
)
