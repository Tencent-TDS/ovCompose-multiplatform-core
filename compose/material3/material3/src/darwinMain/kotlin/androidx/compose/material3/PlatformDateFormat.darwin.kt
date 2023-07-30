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

import kotlinx.datetime.Instant
import platform.Foundation.NSCalendar
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSDateFormatterShortStyle
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

internal actual object PlatformDateFormat {

    actual val weekdayNames: List<Pair<String, String>>?
        get() = weekdayNames()

    actual val firstDayOfWeek: Int
        get() = firstDayOfWeek()

    actual fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {

        val nsDate = NSDate.dateWithTimeIntervalSince1970(utcTimeMillis / 1000.0)

        return NSDateFormatter().apply {
            setDateFormat(pattern)
            setLocale(locale)
        }.stringFromDate(nsDate)
    }

    actual fun formatWithSkeleton(
        utcTimeMillis: Long,
        skeleton: String,
        locale: CalendarLocale
    ): String {

        val nsDate = NSDate.dateWithTimeIntervalSince1970(utcTimeMillis / 1000.0)

        return NSDateFormatter().apply {
            setLocalizedDateFormatFromTemplate(skeleton)
            setLocale(locale)
        }.stringFromDate(nsDate)
    }

    actual fun parse(
        date: String,
        pattern: String
    ): CalendarDate? {

        val nsDate = NSDateFormatter().apply {
            setDateFormat(pattern)
        }.dateFromString(date) ?: return null

        return Instant
            .fromEpochMilliseconds((nsDate.timeIntervalSince1970 * 1000).toLong())
            .toCalendarDate()
    }

    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {

        var pattern = NSDateFormatter().apply {
            setDateStyle(NSDateFormatterShortStyle)
        }.dateFormat

        val delimiter = pattern.first { !it.isLetter() }

        // most of time dateFormat returns dd.MM.y -> we need dd.MM.yyyy
        if (!pattern.contains("yyyy", true)) {

            // probably it can also return dd.MM.yy because such formats exist so check for it
            while (pattern.contains("yy", true)) {
                pattern = pattern.replace("yy", "y",true)
            }

            pattern = pattern.replace("y", "yyyy",true)
        }

        return DateInputFormat(pattern, delimiter)
    }

    @Suppress("UNCHECKED_CAST")
    private fun weekdayNames(): List<Pair<String, String>> {
        val formatter = NSDateFormatter().apply {
            setLocale(NSLocale.currentLocale)
        }

        val fromSundayToSaturday = formatter.standaloneWeekdaySymbols
            .zip(formatter.shortStandaloneWeekdaySymbols) as List<Pair<String, String>>

        return fromSundayToSaturday.drop(1) + fromSundayToSaturday.first()
    }

    private fun firstDayOfWeek(): Int {
        return (NSCalendar.currentCalendar.firstWeekday.toInt() - 1).takeIf { it > 0 } ?: 7
    }

}