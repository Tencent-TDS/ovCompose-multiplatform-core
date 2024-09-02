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

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Defines the options for window decoration.
 */
interface WindowDecoration {

    /**
     * Specifies that the default system decoration should be used.
     */
    object System : WindowDecoration

    /**
     * Specifies that the window should be undecorated.
     *
     * If it is resizable, the given thickness will be used for the edge resizers.
     */
    @Immutable
    class Undecorated(val resizerThickness: Dp = DefaultWindowResizerThickness) : WindowDecoration {
        override fun equals(other: Any?): Boolean {
            if (other !is Undecorated) return false
            return other.resizerThickness == resizerThickness
        }

        override fun hashCode(): Int {
            return resizerThickness.hashCode()
        }
    }

    companion object {
        /**
         * Returns [WindowDecoration.System] if [undecorated] is `false`, or [Undecorated] with
         * default resizer thickness, if `true`.
         */
        fun fromFlag(undecorated: Boolean): WindowDecoration =
            if (undecorated) System else Undecorated()
    }

}

/**
 * The default thickness of the resizers in an undecorated window.
 */
val DefaultWindowResizerThickness: Dp = 8.dp

/**
 * Returns the resizer thickness of the given [WindowDecoration].
 */
internal val WindowDecoration.resizerThickness: Dp
    get() = if (this is WindowDecoration.Undecorated) resizerThickness else DefaultWindowResizerThickness