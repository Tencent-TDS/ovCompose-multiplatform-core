/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.compose.ui.text

import org.jetbrains.skia.icu.CharDirection


internal actual fun isRtlCodePoint(codePoint: Int): Boolean? {
    return when (CharDirection.of(codePoint)) {
        CharDirection.RIGHT_TO_LEFT,
        CharDirection.RIGHT_TO_LEFT_ARABIC,
        CharDirection.RIGHT_TO_LEFT_EMBEDDING,
        CharDirection.RIGHT_TO_LEFT_OVERRIDE -> true

        CharDirection.LEFT_TO_RIGHT,
        CharDirection.LEFT_TO_RIGHT_EMBEDDING,
        CharDirection.LEFT_TO_RIGHT_OVERRIDE -> false

        else -> null
    }
}
