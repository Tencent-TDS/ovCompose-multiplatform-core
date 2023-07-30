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

import androidx.compose.ui.text.intl.Locale
import kotlin.js.Date
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

internal actual object PlatformDateFormat {

    actual val weekdayNames: List<Pair<String, String>>?
        get() = weekdayNames()

    actual val firstDayOfWeek: Int
        get() = firstDayOfWeek()

    private const val NUMERIC = "numeric"
    private const val SHORT = "short"
    private const val LONG = "long"

    private val countriesFirstDow: Map<String, Int> by lazy {
        listOf(
            7 to listOf("TH", "ET", "SG", "SG", "JM", "BT", "IN", "US", "US", "MO", "KE", "IN", "DO", "AU", "IL", "IN", "KE", "KE", "AS", "TW", "IN", "MZ", "MM", "CN", "IN", "PR", "IN", "PK", "BD", "NP", "IN", "KE", "HN", "BR", "HK", "KE", "PK", "CN", "IN", "TT", "ZA", "VE", "KE", "US", "IN", "IN", "MT", "BD", "ZA", "ET", "ET", "MO", "PH", "PE", "ZA", "ID", "DM", "IL", "CN", "WS", "ZW", "TH", "MO", "PK", "PK", "UM", "LA", "ZA", "BZ", "IN", "MM", "PK", "SG", "KE", "IN", "JP", "SV", "BR", "IN", "IN", "CN", "KE", "BD", "PK", "CN", "KE", "BR", "CN", "US", "KE", "KE", "IN", "HK", "IN", "KE", "US", "ET", "CN", "IN", "IN", "SA", "PH", "ET", "MT", "IL", "IN", "US", "PH", "PH", "CO", "GT", "ZW", "KE", "MO", "JP", "BZ", "US", "IN", "BW", "IL", "ZW", "KE", "SG", "ET", "LA", "JP", "KE", "KR", "ID", "IN", "HK", "PE", "MZ", "HK", "IN", "PA", "IN", "KE", "MZ", "YE", "BS", "KE", "IN", "KE", "IN", "KR", "MX", "ZA", "IN", "IN", "BD", "ZA", "IN", "MH", "ID", "ZA", "KE", "IN", "KE", "MT", "IN", "MZ", "ID", "CN", "PK", "IN", "PH", "GU", "PY", "IN", "BT", "PH", "MO", "US", "AG", "KE", "PE", "ID", "KE", "US", "CA", "KE", "ZW", "KE", "ZW", "SG", "KH", "PR", "CN", "IN", "KE", "IN", "KE", "HK", "NP", "IN", "ID", "IN", "ID", "TW", "IN", "HK", "KE", "IN", "TH", "PT", "VI", "KH", "IN", "IN", "IN", "MZ", "CA", "US", "IN", "ET", "IN", "NI", "CN", "PK", "IN"),
            6 to listOf("EG", "AF", "SY", "AF", "IR", "OM", "IQ", "DZ", "IR", "DJ", "DJ", "IR", "AE", "IR", "SD", "AF", "IQ", "KW", "JO", "DZ", "AF", "IQ", "SD", "IR", "DZ", "DZ", "AF", "IR", "AE", "BH", "QA", "EG", "IQ", "IR", "LY", "SY", "DJ")
        ).map { (day, tags) -> tags.map { it to day } }.flatten().toMap()
    }

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
            .replace("M", date.monthNumber.toString())
            .replace("dd", date.dayOfMonth.toStringWithLeadingZero(), ignoreCase = true)
            .replace("d", date.dayOfMonth.toString(), ignoreCase = true)
            .replace("hh", date.hour.toStringWithLeadingZero(), ignoreCase = true)
            .replace("h", date.hour.toString(), ignoreCase = true)
            .replace("ii", date.minute.toStringWithLeadingZero(), ignoreCase = true)
            .replace("i", date.minute.toString(), ignoreCase = true)
            .replace("ss", date.second.toStringWithLeadingZero(), ignoreCase = true)
            .replace("s", date.second.toString(), ignoreCase = true)
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

    private fun parseSegment(date: String, pattern: String, segmentPattern: String): Int? {
        val index = pattern
            .indexOf(segmentPattern, ignoreCase = true)
            .takeIf { it >= 0 } ?: return null

        return date.substring(index, index + segmentPattern.length)
            .toIntOrNull()
    }

    actual fun getDateInputFormat(locale: CalendarLocale): DateInputFormat {
        val date = Date(year = 1234, month = 10, day = 23)

        val shortDate = date.toLocaleDateString(locale.toLanguageTag())

        val delimiter = shortDate.first { !it.isDigit() }

        val pattern = shortDate
            .replace("1234", "yyyy")
            .replace("11", "MM") //10 -> 11 not an error. month is index
            .replace("23", "dd")

        return DateInputFormat(pattern, delimiter)
    }

    private fun weekdayNames(): List<Pair<String, String>> {
        val now = Date.now()

        val week = List(DaysInWeek) {
            Date(now + MillisecondsIn24Hours * it)
        }.sortedBy { it.getDay() } // sunday to saturday

        val mondayToSunday = week.drop(1) + week.first()

        val locale = defaultLocale()

        val longAndShortWeekDays = listOf(LONG, SHORT).map { format ->
            mondayToSunday.map {
                it.toLocaleDateString(
                    locales = locale.toLanguageTag(),
                    options = dateLocaleOptions {
                        weekday = format
                    })
            }
        }
        return longAndShortWeekDays[0].zip(longAndShortWeekDays[1])
    }

    private fun firstDayOfWeek() : Int{
        // supported by most of browsers except Firefox since 2022
        // https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Intl
        val isIntlSupported = js("typeof(Intl) != undefined")
            .unsafeCast<Boolean>()

        if (!isIntlSupported)
            return countriesFirstDow[Locale.current.region.uppercase()] ?: 1

        val locale = Locale.current.toLanguageTag()

        return js("new Intl.Locale(locale).weekInfo.firstDay").unsafeCast<Int>()
    }
}

private fun Int.toStringWithLeadingZero(): String{
    return if (this >= 10) toString() else "0$this"
}