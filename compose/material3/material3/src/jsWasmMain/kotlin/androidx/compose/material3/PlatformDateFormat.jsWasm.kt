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

    //TODO: replace formatting with kotlinx datetime when supported
    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {
        val date = Date(utcTimeMillis)
        return pattern
            .replace("%Y".toRegex(), date.getFullYear().toString())
            .replace("%y".toRegex(), date.getFullYear().toString())
            .replace("%m".toRegex(), date.getMonth().toString())
            .replace("%M".toRegex(), date.getMonth().toString())
            .replace("%d".toRegex(), date.getDay().toString())
            .replace("%H".toRegex(), date.getHours().toString())
            .replace("%h".toRegex(), date.getHours().toString())
            .replace("%i".toRegex(), date.getMinutes().toString())
            .replace("%s".toRegex(), date.getSeconds().toString())
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        locale: CalendarLocale
    ): String {
        // TODO: support date skeletons on js
        return formatWithPattern(utcTimeMillis, skeleton, locale)
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        val year = parseSegment(date, pattern, "YYYY") ?: return null
        val month = parseSegment(date, pattern, "MM") ?: return null
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