/*
 * Copyright 2022 The Android Open Source Project
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

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

/**
 * [PointerInputMatcher] represents a single condition or a set of conditions which a [PointerEvent] has to match
 * in order to count as an appropriate event for a gesture.
 *
 *  Supported matchers:
 * - [mouse] - will match an event with [PointerType.Mouse] with a required [PointerButton]
 * - [touch] - will match any event with [PointerType.Touch]
 * - [stylus] - will match an event with [PointerType.Stylus]. And optional [PointerButton] can be specified.
 * - [eraser] - will match any event with [PointerType.Eraser]
 * - [pointer] - takes in [PointerType] and optional [PointerButton]
 *
 * Their combination is supported using plus operator:
 * ```
 * mouse(PointerButton.Primary) + touch + stylus + eraser
 * ```
 * See [DefaultPointerInputMatcher].
 */
@ExperimentalFoundationApi
@OptIn(ExperimentalComposeUiApi::class)
sealed interface PointerInputMatcher {

    @ExperimentalFoundationApi
    val pointerType: PointerType

    @ExperimentalFoundationApi
    fun matches(event: PointerEvent): Boolean {
        return event.changes.fastAll { it.type == pointerType }
    }

    @ExperimentalFoundationApi
    operator fun plus(pointerInputMatcher: PointerInputMatcher): PointerInputMatcher {
        return if (this is CombinedPointerInputMatcher) {
            this.sources.add(pointerInputMatcher)
            this
        } else if (pointerInputMatcher is CombinedPointerInputMatcher) {
            pointerInputMatcher.sources.add(this)
            pointerInputMatcher
        } else {
            CombinedPointerInputMatcher(mutableListOf(this, pointerInputMatcher))
        }
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        @ExperimentalFoundationApi
        fun pointer(
            pointerType: PointerType,
            button: PointerButton? = null
        ): PointerInputMatcher = object : PointerInputMatcherWithButton {
            override val pointerType = pointerType
            override val button = button
        }

        @ExperimentalFoundationApi
        fun mouse(button: PointerButton): PointerInputMatcher = MousePointerInputMatcher(button)
        @ExperimentalFoundationApi
        fun stylus(button: PointerButton? = null): PointerInputMatcher = StylusPointerInputMatcher(button)

        @ExperimentalFoundationApi
        val stylus: PointerInputMatcher = StylusPointerInputMatcher.Companion
        @ExperimentalFoundationApi
        val touch: PointerInputMatcher = TouchPointerInputMatcher
        @ExperimentalFoundationApi
        val eraser: PointerInputMatcher = EraserPointerInputMatcher

        private interface PointerInputMatcherWithButton : PointerInputMatcher {
            val button: PointerButton?

            override fun matches(event: PointerEvent): Boolean {
                return event.changes.fastAll { it.type == pointerType } &&
                    if (button != null) event.button == button else true
            }
        }

        private class MousePointerInputMatcher(
            override val button: PointerButton
        ) : PointerInputMatcherWithButton {
            override val pointerType = PointerType.Mouse
        }

        private class StylusPointerInputMatcher(
            override val button: PointerButton? = null
        ) : PointerInputMatcherWithButton {
            override val pointerType = PointerType.Stylus

            companion object : PointerInputMatcher {
                override val pointerType = PointerType.Stylus
            }
        }

        private object TouchPointerInputMatcher : PointerInputMatcher {
            override val pointerType = PointerType.Touch
        }

        private object EraserPointerInputMatcher : PointerInputMatcher {
            override val pointerType = PointerType.Eraser
        }

        private class CombinedPointerInputMatcher(val sources: MutableList<PointerInputMatcher>) : PointerInputMatcher {
            override val pointerType = PointerType.Unknown

            override fun matches(event: PointerEvent): Boolean {
                return sources.any { it.matches(event) }
            }
        }

        /**
         * The Default [PointerInputMatcher] which covers the most common cases of pointer inputs.
         * [DefaultPointerInputMatcher] will match [PointerEvent]s, which match at least one of the following conditions:
         * - [PointerType] is [PointerType.Mouse] and [PointerEvent.button] is [PointerButton.Primary]
         * - [PointerType] is [PointerType.Touch]
         * - [PointerType] is [PointerType.Stylus], regardless of any buttons pressed
         * - [PointerType] is [PointerType.Eraser]
         */
        @ExperimentalFoundationApi
        val DefaultPointerInputMatcher = mouse(PointerButton.Primary) + touch + stylus + eraser
    }
}
