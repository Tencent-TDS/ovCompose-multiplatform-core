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
import platform.Foundation.NSDateIntervalFormatter
import platform.Foundation.NSLocale
import platform.Foundation.currentLocale
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.Foundation.timeIntervalSince1970

internal actual object PlatformDateFormat {

    @Suppress("UNCHECKED_CAST")
    actual val weekdayNames: List<Pair<String, String>>?
        get() {
            val formatter = NSDateFormatter().apply {
                setLocale(NSLocale.currentLocale)
            }

            val fromSundayToSaturday = formatter.weekdaySymbols
                .zip(formatter.shortWeekdaySymbols) as List<Pair<String, String>>

            val sunday = fromSundayToSaturday.first()

            return fromSundayToSaturday.drop(1) + sunday
        }

    actual val firstDayOfWeek: Int
        get() = NSCalendar.currentCalendar.firstWeekday.toInt()

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

    //TODO support localized date input format on darwin
    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return DateInputFormat("yyyy-MM-dd", '-')
    }

}