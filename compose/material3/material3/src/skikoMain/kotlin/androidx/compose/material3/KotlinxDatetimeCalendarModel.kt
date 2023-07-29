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

import kotlin.math.abs
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime


internal class KotlinxDatetimeCalendarModel : CalendarModel {

    override val today: CalendarDate
        get() = Clock.System.now().toCalendarDate()

    override val firstDayOfWeek: Int
        get() = PlatformDateFormat.firstDayOfWeek

    //TODO: localize weekdays names
    override val weekdayNames: List<Pair<String, String>>
        get() = PlatformDateFormat.weekdayNames ?: listOf(
            "Monday" to "Mon",
            "Tuesday" to "Tue",
            "Wednesday" to "Wed",
            "Thursday" to "Thu",
            "Friday" to "Fri",
            "Saturday" to "Sat",
            "Sunday" to "Sun",
        )


    private val systemTZ
        get() = TimeZone.currentSystemDefault()
    override fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        return PlatformDateFormat.getDateInputFormat(locale)
    }

    override fun getCanonicalDate(timeInMillis: Long): CalendarDate {
        return Instant
            .fromEpochMilliseconds(timeInMillis)
            .toLocalDateTime(TimeZone.UTC)
            .date
            .atStartOfDayIn(TimeZone.UTC)
            .toCalendarDate()
    }

    override fun getMonth(timeInMillis: Long): CalendarMonth {
        return Instant
            .fromEpochMilliseconds(timeInMillis)
            .toCalendarMonth()
    }

    override fun getMonth(date: CalendarDate): CalendarMonth {
        return getMonth(date.utcTimeMillis)
    }

    override fun getMonth(year: Int, month: Int): CalendarMonth {
        val instant = LocalDate(
            year = year,
            monthNumber = month,
            dayOfMonth = 1,
        ).atStartOfDayIn(systemTZ)

        return getMonth(instant.toEpochMilliseconds())
    }

    override fun getDayOfWeek(date: CalendarDate): Int {
        return Instant
            .fromEpochMilliseconds(date.utcTimeMillis)
            .toLocalDateTime(systemTZ)
            .dayOfWeek.isoDayNumber
    }

    override fun plusMonths(from: CalendarMonth, addedMonthsCount: Int): CalendarMonth {
        val dateTime = Instant
            .fromEpochMilliseconds(from.startUtcTimeMillis)
            .toLocalDateTime(systemTZ)

        var month = dateTime.monthNumber + addedMonthsCount
        val year = dateTime.year + (month-1)/12

        month = (month % 12).takeIf { it != 0 } ?: 12

        return LocalDate(
            year = year,
            monthNumber = month,
            dayOfMonth = 1,
        ).atStartOfDayIn(systemTZ)
            .toCalendarMonth()
    }

    override fun minusMonths(from: CalendarMonth, subtractedMonthsCount: Int): CalendarMonth {
        val dateTime = Instant
            .fromEpochMilliseconds(from.startUtcTimeMillis)
            .toLocalDateTime(systemTZ)

        var month = dateTime.monthNumber - subtractedMonthsCount
        val year = dateTime.year - (abs(month)-1)/12

        if (month < 0)
            month = (12 + month % 12).takeIf { it != 0 } ?: 12

        return LocalDate(
            year = year,
            monthNumber = month,
            dayOfMonth = 1,
        ).atStartOfDayIn(systemTZ)
            .toCalendarMonth()
    }

    override fun formatWithPattern(
        utcTimeMillis: Long,
        pattern: String,
        locale: CalendarLocale
    ): String {
        return PlatformDateFormat.formatWithPattern(utcTimeMillis, pattern, locale)
    }

    override fun parse(date: String, pattern: String): CalendarDate? {
        return PlatformDateFormat.parse(date, pattern)
    }
}


@ExperimentalMaterial3Api
internal actual fun formatWithSkeleton(
    utcTimeMillis: Long,
    skeleton: String,
    locale: CalendarLocale
): String {
    return PlatformDateFormat.formatWithSkeleton(
        utcTimeMillis = utcTimeMillis,
        skeleton = skeleton,
        locale = locale
    )
}

internal fun Instant.toCalendarDate(
    timeZone : TimeZone = TimeZone.currentSystemDefault()
) : CalendarDate {

    val dateTime = toLocalDateTime(timeZone)

    return CalendarDate(
        year = dateTime.year,
        month = dateTime.monthNumber,
        dayOfMonth = dateTime.dayOfMonth,
        utcTimeMillis = toEpochMilliseconds()
    )
}


private fun Instant.toCalendarMonth(
    timeZone : TimeZone = TimeZone.currentSystemDefault()
) : CalendarMonth {

    val dateTime = toLocalDateTime(timeZone)

    val monthStart = LocalDate(
        year = dateTime.year,
        month = dateTime.month,
        dayOfMonth = 1,
    )

    return CalendarMonth(
        year = dateTime.year,
        month = dateTime.monthNumber,
        numberOfDays = dateTime.month
            .numberOfDays(dateTime.year.isLeapYear()),
        daysFromStartOfWeekToFirstOfMonth = monthStart.dayOfWeek.isoDayNumber-1,
        startUtcTimeMillis = monthStart
            .atStartOfDayIn(TimeZone.UTC)
            .toEpochMilliseconds()
    )
}

private fun Int.isLeapYear() = this%4 == 0 && (this%100 != 0 || this%400 == 0)

private fun Month.numberOfDays(isLeap : Boolean) : Int {
    return when(this){
        Month.FEBRUARY -> if (isLeap) 29 else 28
        Month.JANUARY,
        Month.MARCH,
        Month.MAY,
        Month.JULY,
        Month.AUGUST,
        Month.OCTOBER,
        Month.DECEMBER -> 31

        else -> 30
    }
}