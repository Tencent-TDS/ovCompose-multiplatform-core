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

package androidx.compose.material3

import kotlin.js.Date
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn

internal actual object PlatformDateFormat {

    actual val weekdayNames: List<Pair<String, String>>?
        get() = null

    // TODO: support localized first day of week on JS
    actual val firstDayOfWeek: Int
        get() = 1

    private const val NUMERIC = "numeric"
    private const val SHORT = "short"
    private const val LONG = "long"

    //TODO: replace formatting with kotlinx datetime when supported
    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {
        val date = Date(utcTimeMillis)
        return pattern
            .replace("yyyy", date.getFullYear().toString(), ignoreCase = true)
            .replace("yy", date.getFullYear().toString().takeLast(2), ignoreCase = true)
            .replace("mm", date.getMonth().toString(), ignoreCase = true)
            .replace("dd", date.getDay().toString(), ignoreCase = true)
            .replace("hh", date.getHours().toString(), ignoreCase = true)
            .replace("ii", date.getMinutes().toString(), ignoreCase = true)
            .replace("ss", date.getSeconds().toString(), ignoreCase = true)
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        locale: CalendarLocale
    ): String {
        val date = Date(utcTimeMillis)

        return date.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                year = when {
                    skeleton.contains("y", true) -> NUMERIC
                    else -> null
                }
                month = when {
                    skeleton.contains("MMMM", true) -> LONG
                    skeleton.contains("MMM", true) -> SHORT
                    skeleton.contains("MM", true) -> NUMERIC
                    else -> null
                }
                day = when {
                    skeleton.contains("dd", true) -> NUMERIC
                    else -> null
                }
                hour = when {
                    skeleton.contains("h", true) -> NUMERIC
                    else -> null
                }
                minute = when {
                    skeleton.contains("i", true) -> NUMERIC
                    else -> null
                }
                second = when {
                    skeleton.contains("s", true) -> NUMERIC
                    else -> null
                }
            }
        )
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        val year = parseSegment(date, pattern, "yyy") ?: return null
        val month = parseSegment(date, pattern, "mm") ?: return null
        val day = parseSegment(date, pattern, "dd")

        return LocalDate(
            year, month, day ?: 1
        ).atStartOfDayIn(TimeZone.currentSystemDefault())
            .toCalendarDate()
    }

    private fun parseSegment(date: String, pattern: String, segmentPattern : String) : Int? {
        val index = pattern
            .indexOf(segmentPattern, ignoreCase = true)
            .takeIf { it >=0 } ?: return null

        return date.substring(index, index + segmentPattern.length)
            .toIntOrNull()
    }

    //TODO: support localized date input format on js
    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return DateInputFormat("yyyy-MM-dd",'-')
    }
}