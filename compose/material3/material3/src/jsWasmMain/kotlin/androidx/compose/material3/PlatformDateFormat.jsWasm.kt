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
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

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
        val date = Instant
            .fromEpochMilliseconds(utcTimeMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())

        val jsDate = Date(utcTimeMillis)

        val monthShort = jsDate.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                month = SHORT
            })

        val monthLong = jsDate.toLocaleDateString(
            locales = locale.toLanguageTag(),
            options = dateLocaleOptions {
                month = LONG
            })

        return pattern
            .replace("yyyy", date.year.toString(), ignoreCase = true)
            .replace("yy", date.year.toString().takeLast(2), ignoreCase = true)
            .replace("MMMM", monthLong)
            .replace("MMM", monthShort)
            .replace("MM", date.monthNumber.toStringWithLeadingZero())
            .replace("dd", date.dayOfMonth.toStringWithLeadingZero(), ignoreCase = true)
            .replace("hh", date.hour.toStringWithLeadingZero(), ignoreCase = true)
            .replace("ii", date.minute.toStringWithLeadingZero(), ignoreCase = true)
            .replace("ss", date.second.toStringWithLeadingZero(), ignoreCase = true)
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
                    else -> undefined
                }
                month = when {
                    skeleton.contains("MMMM", true) -> LONG
                    skeleton.contains("MMM", true) -> SHORT
                    skeleton.contains("MM", true) -> NUMERIC
                    else -> undefined
                }
                day = when {
                    skeleton.contains("d", true) -> NUMERIC
                    else -> undefined
                }
                hour = when {
                    skeleton.contains("h", true) -> NUMERIC
                    else -> undefined
                }
                minute = when {
                    skeleton.contains("i", true) -> NUMERIC
                    else -> undefined
                }
                second = when {
                    skeleton.contains("s", true) -> NUMERIC
                    else -> undefined
                }
            }
        )
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {
        val year = parseSegment(date, pattern, "yyyy") ?: return null
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

    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        val date = Date(year = 1234, month = 10, day = 23)

        val shortDate = date.toLocaleDateString(locale.toLanguageTag())

        val delimiter = shortDate.first { !it.isDigit() }

        println(shortDate)

        val pattern = shortDate
            .replace("1234","yyyy")
            .replace("11", "MM") //10 -> 11 not an error. month is index
            .replace("23","dd")

        return DateInputFormat(pattern,delimiter)
    }
}

private fun Int.toStringWithLeadingZero(): String{
    return if (this >= 10) toString() else "0$this"
}