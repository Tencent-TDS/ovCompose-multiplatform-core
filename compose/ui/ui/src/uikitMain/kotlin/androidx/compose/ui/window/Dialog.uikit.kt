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

package androidx.compose.ui.window

import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.MeasureScope
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal actual fun MeasureScope.platformDefaultConstrains(
    measurable: Measurable,
    constraints: Constraints
): Constraints = constraints.copy(
    maxWidth = min(preferredDialogWidth(constraints), constraints.maxWidth)
)

// Ported from Android. See https://cs.android.com/search?q=abc_config_prefDialogWidth
private fun MeasureScope.preferredDialogWidth(constraints: Constraints): Int {
    val smallestWidth = min(constraints.maxWidth, constraints.maxHeight).toDp()
    return when {
        smallestWidth >= 600.dp -> 580.dp
        smallestWidth >= 480.dp -> 440.dp
        else -> 320.dp
    }.roundToPx()
}
