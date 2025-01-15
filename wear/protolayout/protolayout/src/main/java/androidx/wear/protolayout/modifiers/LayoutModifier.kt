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

package androidx.wear.protolayout.modifiers

import java.util.Objects

/**
 * An ordered, immutable collection of [modifier elements][LayoutModifier.Element] that decorate or
 * add behavior to ProtoLayout layout elements. For example, backgrounds, padding and click actions.
 * When a single modifier is applied multiple times, the last one wins.
 *
 * @sample androidx.wear.protolayout.material3.samples.edgeButtonSampleIcon
 */
interface LayoutModifier {
    /**
     * Accumulates a value starting with [initial] and applying [operation] to the current value and
     * each element from outside in.
     *
     * Elements wrap one another in a chain from left to right; an [Element] that appears to the
     * left of another in a `+` expression or in [operation]'s parameter order affects all of the
     * elements that appear after it. [foldIn] may be used to accumulate a value starting from the
     * parent or head of the modifier chain to the final wrapped child.
     */
    fun <R> foldIn(initial: R, operation: (R, Element) -> R): R

    /**
     * Concatenates this modifier with another.
     *
     * Returns a [LayoutModifier] representing this modifier followed by [other] in sequence.
     */
    infix fun then(other: LayoutModifier): LayoutModifier =
        if (other === LayoutModifier) this else CombinedLayoutModifier(this, other)

    /** A single element contained within a [LayoutModifier] chain. */
    interface Element : LayoutModifier {
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R =
            operation(initial, this)
    }

    /**
     * The companion object `LayoutModifier` is the empty, default, or starter [LayoutModifier] that
     * contains no [elements][Element]. Use it to create a new [LayoutModifier] using modifier
     * extension factory functions.
     */
    companion object : LayoutModifier {
        @Suppress("MissingJvmstatic")
        override fun <R> foldIn(initial: R, operation: (R, Element) -> R): R = initial

        @Suppress("MissingJvmstatic")
        override infix fun then(other: LayoutModifier): LayoutModifier = other

        @Suppress("MissingJvmstatic") override fun toString(): String = "LayoutModifier"
    }
}

/**
 * A node in a [LayoutModifier] chain. A CombinedModifier always contains at least two elements; a
 * * Modifier [outer] that wraps around the Modifier [inner].
 */
internal class CombinedLayoutModifier(
    private val outer: LayoutModifier,
    private val inner: LayoutModifier
) : LayoutModifier {
    override fun <R> foldIn(initial: R, operation: (R, LayoutModifier.Element) -> R): R =
        inner.foldIn(outer.foldIn(initial, operation), operation)

    override fun equals(other: Any?): Boolean =
        other is CombinedLayoutModifier && outer == other.outer && inner == other.inner

    override fun hashCode(): Int = Objects.hash(outer, inner)

    override fun toString(): String =
        "[" +
            foldIn("") { acc, element ->
                if (acc.isEmpty()) element.toString() else "$acc, $element"
            } +
            "]"
}
