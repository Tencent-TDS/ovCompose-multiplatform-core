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

package androidx.compose.material3.internal

import androidx.compose.material3.CalendarLocale

internal actual class PlatformDateFormat actual constructor(private val locale: CalendarLocale) {

    actual val firstDayOfWeek: Int
        get() = TODO("not implemented")

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
    ): String {
        TODO("not implemented")
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
    ): String {
        TODO("not implemented")
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        TODO("not implemented")
    }

    actual fun getDateInputFormat(): DateInputFormat {
        TODO("not implemented")
    }

    actual val weekdayNames: List<Pair<String, String>> get() {
        TODO("not implemented")
    }

    // See http://www.unicode.org/reports/tr35/tr35-31/tr35-dates.html#Date_Format_Patterns
    //
    // 'j' template requests the preferred hour format for the locale.
    // 'a' is a pattern for AM\PM symbol. Presence of this symbol means that locale has 12h format.
    actual fun is24HourFormat(): Boolean {
        TODO("not implemented")
    }
}
