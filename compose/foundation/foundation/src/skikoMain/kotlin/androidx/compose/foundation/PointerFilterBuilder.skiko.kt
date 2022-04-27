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
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.util.fastAll

/**
 * Provides a builder DSL for filtering [PointerEvent]s based on [PointerType].
 *
 * There such methods:
 * [mouse], [touch], [stylus], [eraser], [pointer] - for possible pointer types not supported out of the box.
 * If none of those configured, then all events will be skipped (filtered out).
 * Those methods configure [FilterBuilder],
 * which lets setting one required [PointerButton] and required [PointerKeyboardModifiers].
 *
 * The order of filters application is following:
 * mouse, touch, stylus, eraser, pointer(s) regardless of the order (arrangement) of methods calls.
 *
 * Consider using [Default] which covers most common cases for click handlers (e.g.: on primary click).
 */
@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalFoundationApi
class PointerFilter internal constructor() {

    companion object {
        @ExperimentalFoundationApi
        val Default: PointerFilter.() -> Unit = {
            mouse { button = PointerButton.Primary }
            touch {
                // no button or keyboardModifiers required
            }
            stylus {
                // no button or keyboardModifiers required
            }
            eraser {
                // no button or keyboardModifiers required
            }
        }

        private val DefaultFilterByKeyboardModifiers: PointerKeyboardModifiers.() -> Boolean = { true }
    }

    inner class FilterBuilder {
        var button: PointerButton? = null
        var keyboardModifiers: PointerKeyboardModifiers.() -> Boolean = DefaultFilterByKeyboardModifiers
    }

    private var mouse: FilterVariant? = null
    private var touch: FilterVariant? = null
    private var stylus: FilterVariant? = null
    private var eraser: FilterVariant? = null
    private val customPointers = mutableListOf<FilterVariant>()

    private fun buildFilterData(pointerType: PointerType, builder: FilterBuilder): FilterVariant {
        return FilterVariant(
            pointerType = pointerType,
            button = builder.button,
            keyboardModifiers = builder.keyboardModifiers
        )
    }

    @ExperimentalFoundationApi
    fun mouse(builder: FilterBuilder.() -> Unit) {
        mouse = buildFilterData(PointerType.Mouse, FilterBuilder().also(builder))
    }

    @ExperimentalFoundationApi
    fun touch(builder: FilterBuilder.() -> Unit) {
        touch = buildFilterData(PointerType.Touch, FilterBuilder().also(builder))
    }

    @ExperimentalFoundationApi
    fun stylus(builder: FilterBuilder.() -> Unit) {
        stylus = buildFilterData(PointerType.Stylus, FilterBuilder().also(builder))
    }

    @ExperimentalFoundationApi
    fun eraser(builder: FilterBuilder.() -> Unit) {
        eraser = buildFilterData(PointerType.Eraser, FilterBuilder().also(builder))
    }

    @ExperimentalFoundationApi
    fun pointer(type: PointerType, builder: FilterBuilder.() -> Unit) {
        customPointers.add(buildFilterData(type, FilterBuilder().also(builder)))
    }

    @ExperimentalFoundationApi
    internal fun combinedFilter(): (PointerEvent) -> Boolean {
        return { event ->
            mouse?.filter(event) == true ||
                touch?.filter(event) == true ||
                stylus?.filter(event) == true ||
                eraser?.filter(event) == true ||
                customPointers.any { it.filter(event) }
        }
    }
}


@OptIn(ExperimentalComposeUiApi::class)
private data class FilterVariant(
    val pointerType: PointerType,
    val button: PointerButton?,
    val keyboardModifiers: PointerKeyboardModifiers.() -> Boolean,
) {
    fun filter(e: PointerEvent): Boolean {
        return e.changes.fastAll { it.type == pointerType } &&
            keyboardModifiers(e.keyboardModifiers) &&
            if (button != null) e.button == button else true

    }
}
