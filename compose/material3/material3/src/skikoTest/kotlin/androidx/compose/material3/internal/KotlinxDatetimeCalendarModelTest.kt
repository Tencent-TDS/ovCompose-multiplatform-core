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

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.assertThat
import androidx.compose.material3.isEqualTo
import kotlin.test.Test


//TODO: currently ICU skeleton is not supported on JVM.
// Remove this condition or set it to true in desktopTest when supported
expect val supportsDateSkeleton : Boolean

expect fun getTimeZone() : String
expect fun setTimeZone(id : String)

// Tests were copied from Android CalendarModelTest and adopted to multiplatform
@OptIn(ExperimentalMaterial3Api::class)
internal class KotlinxDatetimeCalendarModelTest {

    @Test
    fun dateCreation() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val date = model.getCanonicalDate(January2022Millis) // 1/1/2022
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateCreation_differentTZ() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val defaultTz = getTimeZone()

        setTimeZone("GMT-5")

        var date = model.getCanonicalDate(January2022Millis) // 1/1/2022
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)

        setTimeZone("GMT+5")

        date = model.getCanonicalDate(January2022Millis) // 1/1/2022
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)

        setTimeZone(defaultTz)
    }

    @Test
    fun dateCreation_withRounding() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val date = model.getCanonicalDate(January2022Millis + 30000) // 1/1/2022 + 30000 millis
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        // Check that the milliseconds represent the start of the day.
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateCreation_withRounding_differentTz() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val defaultTz = getTimeZone()

        setTimeZone("GMT-5")

        var date = model.getCanonicalDate(January2022Millis + 30000) // 1/1/2022 + 30000 millis
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        // Check that the milliseconds represent the start of the day.
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)

        setTimeZone("GMT+5")

        date = model.getCanonicalDate(January2022Millis + 30000) // 1/1/2022 + 30000 millis
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        // Check that the milliseconds represent the start of the day.
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)

        setTimeZone(defaultTz)
    }

    @Test
    fun dateRestore() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )
        assertThat(model.getCanonicalDate(date.utcTimeMillis)).isEqualTo(date)
    }

    @Test
    fun monthCreation() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )
        val monthFromDate = model.getMonth(date)
        val monthFromMilli = model.getMonth(January2022Millis)
        val monthFromYearMonth = model.getMonth(year = 2022, month = 1)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
        assertThat(monthFromDate).isEqualTo(monthFromYearMonth)
    }

    @Test
    fun monthCreation_differentTz() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val defaultTz = getTimeZone()

        setTimeZone("GMT-5")

        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )

        var monthFromDate = model.getMonth(date)
        var monthFromMilli = model.getMonth(January2022Millis)
        var monthFromYearMonth = model.getMonth(year = 2022, month = 1)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
        assertThat(monthFromDate).isEqualTo(monthFromYearMonth)

        setTimeZone("GMT+5")

        monthFromDate = model.getMonth(date)
        monthFromMilli = model.getMonth(January2022Millis)
        monthFromYearMonth = model.getMonth(year = 2022, month = 1)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
        assertThat(monthFromDate).isEqualTo(monthFromYearMonth)

        setTimeZone(defaultTz)
    }

    @Test
    fun monthCreation_withRounding() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )
        val monthFromDate = model.getMonth(date)
        val monthFromMilli = model.getMonth(January2022Millis + 10000)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)
    }

    @Test
    fun monthCreation_withRounding_differentTZ() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val defaultTz = getTimeZone()

        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )

        setTimeZone("GMT-5")


        var monthFromDate = model.getMonth(date)
        var monthFromMilli = model.getMonth(January2022Millis + 10000)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)

        setTimeZone("GMT+5")

        monthFromDate = model.getMonth(date)
        monthFromMilli = model.getMonth(January2022Millis + 10000)
        assertThat(monthFromDate).isEqualTo(monthFromMilli)

        setTimeZone(defaultTz)
    }


    @Test
    fun monthRestore() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val month = model.getMonth(year = 1999, month = 12)
        assertThat(model.getMonth(month.startUtcTimeMillis)).isEqualTo(month)
    }

    @Test
    fun plusMinusMonth() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val month = model.getMonth(January2022Millis) // 1/1/2022
        val expectedNextMonth = model.getMonth(month.endUtcTimeMillis + 1) // 2/1/2022
        val plusMonth = model.plusMonths(from = month, addedMonthsCount = 1)
        assertThat(plusMonth).isEqualTo(expectedNextMonth)
        assertThat(model.minusMonths(from = plusMonth, subtractedMonthsCount = 1)).isEqualTo(month)
    }

    @Test
    fun parseDate() {
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val expectedDate =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )
        val parsedDate = model.parse("1/1/2022", "M/d/yyyy")
        assertThat(parsedDate).isEqualTo(expectedDate)
    }

    @Test
    fun formatDate() {

        if (!supportsDateSkeleton)
            return

        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)

        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )
        assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo("Jan 1, 2022")
        assertThat(model.formatWithSkeleton(date, "dMMMy")).isEqualTo("Jan 1, 2022")
        assertThat(model.formatWithSkeleton(date, "yMMMMEEEEd"))
            .isEqualTo("Saturday, January 1, 2022")
        // Check that the direct formatting is equal to the one the model does.
        assertThat(model.formatWithSkeleton(date, "yMMMd"))
            .isEqualTo(date.format(model, "yMMMd"))
    }

    @Test
    fun formatDate_differentTZ() {

        if (!supportsDateSkeleton)
            return

        val defaultTz = getTimeZone()

        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)

        val date =
            CalendarDate(
                year = 2022,
                month = 1,
                dayOfMonth = 1,
                utcTimeMillis = January2022Millis
            )

        val test = {
            assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo("Jan 1, 2022")
            assertThat(model.formatWithSkeleton(date, "dMMMy",)).isEqualTo("Jan 1, 2022")
            assertThat(model.formatWithSkeleton(date, "yMMMMEEEEd"))
                .isEqualTo("Saturday, January 1, 2022")
            // Check that the direct formatting is equal to the one the model does.
            assertThat(model.formatWithSkeleton(date, "yMMMd"))
                .isEqualTo(date.format(model, "yMMMd"))
        }

        setTimeZone("GMT-5")

        test()

        setTimeZone("GMT+5")

        test()

        setTimeZone(defaultTz)
    }

    @Test
    fun formatMonth() {

        if (!supportsDateSkeleton)
            return

        val locale = calendarLocale("en", "US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val month = model.getMonth(year = 2022, month = 3)
        assertThat(model.formatWithSkeleton(month, "yMMMM")).isEqualTo("March 2022")
        assertThat(model.formatWithSkeleton(month, "MMMMy")).isEqualTo("March 2022")
        // Check that the direct formatting is equal to the one the model does.
        assertThat(model.formatWithSkeleton(month, "yMMMM"))
            .isEqualTo(month.format(model, "yMMMM"))
    }

    @Test
    fun formatMonth_differentTz() {

        if (!supportsDateSkeleton)
            return
        val defaultTz = getTimeZone()


        val locale = calendarLocale("en", "US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val month = model.getMonth(year = 2022, month = 3)

        val test = {
            assertThat(model.formatWithSkeleton(month, "yMMMM")).isEqualTo("March 2022")
            assertThat(model.formatWithSkeleton(month, "MMMMy")).isEqualTo("March 2022")
            // Check that the direct formatting is equal to the one the model does.
            assertThat(model.formatWithSkeleton(month, "yMMMM"))
                .isEqualTo(month.format(model, "yMMMM"))
        }

        setTimeZone("GMT-5")

        test()

        setTimeZone("GMT+5")

        test()

        setTimeZone(defaultTz)
    }

    @Test
    fun weekdayNames() {
        // Ensure we are running on a US locale for this test.
        val locale = calendarLocale("en","US")
        val model = KotlinxDatetimeCalendarModel(locale)
        val weekDays = model.weekdayNames
        assertThat(weekDays.size).isEqualTo(DaysInWeek)

        // Check that the first day is always "Monday", per ISO-8601 standard.
        assertThat(weekDays.first().first.lowercase()).isEqualTo("monday")
        weekDays.forEach {
            assertThat(it.second.first().lowercaseChar()).isEqualTo(
                it.first.first().lowercaseChar()
            )
        }
    }

    @Test
    fun dateInputFormat() {

        var locale = calendarLocale("en","US")
        var model = KotlinxDatetimeCalendarModel(locale)
        assertThat(model.getDateInputFormat(locale).patternWithDelimiters,).isEqualTo("MM/dd/yyyy")
        assertThat(model.getDateInputFormat(locale).patternWithoutDelimiters).isEqualTo("MMddyyyy")
        assertThat(model.getDateInputFormat(locale).delimiter).isEqualTo('/')

        locale = calendarLocale("zh","CN")
        model = KotlinxDatetimeCalendarModel(locale)
        assertThat(model.getDateInputFormat(locale).patternWithDelimiters).isEqualTo("yyyy/MM/dd")
        assertThat(model.getDateInputFormat(locale).patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(model.getDateInputFormat(locale).delimiter).isEqualTo('/')

        locale = calendarLocale("en","GB")
        model = KotlinxDatetimeCalendarModel(locale)
        assertThat(model.getDateInputFormat(locale).patternWithDelimiters).isEqualTo("dd/MM/yyyy")
        assertThat(model.getDateInputFormat(locale).patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(model.getDateInputFormat(locale).delimiter).isEqualTo('/')

        locale = calendarLocale("ko","KR")
        model = KotlinxDatetimeCalendarModel(locale)
        assertThat(model.getDateInputFormat(locale).patternWithDelimiters).isEqualTo("yyyy.MM.dd")
        assertThat(model.getDateInputFormat(locale).patternWithoutDelimiters).isEqualTo("yyyyMMdd")
        assertThat(model.getDateInputFormat(locale).delimiter).isEqualTo('.')

        locale = calendarLocale("es","CL")
        model = KotlinxDatetimeCalendarModel(locale)
        assertThat(model.getDateInputFormat(locale).patternWithDelimiters).isEqualTo("dd-MM-yyyy")
        assertThat(model.getDateInputFormat(locale).patternWithoutDelimiters).isEqualTo("ddMMyyyy")
        assertThat(model.getDateInputFormat(locale).delimiter).isEqualTo('-')
    }

    @Test
    fun illegalDateParsingDoesNotThrowException(){
        val model = KotlinxDatetimeCalendarModel(calendarLocale("en","US"))

        assertThat(model.parse("50-50-2000","MM-dd-yyyy")).isEqualTo(null)
        assertThat(model.parse("50-50-2000","")).isEqualTo(null)
        assertThat(model.parse("","MM-dd-yyyy")).isEqualTo(null)
    }
}

internal const val January2022Millis = 1640995200000
