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

import com.google.common.truth.Truth.assertThat
import java.util.Locale
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalMaterial3Api::class)
internal class CalendarModelTest {

    private val model = KotlinxDatetimeCalendarModel()

//    private lateinit var defaultLocale: Locale
//
//    @Before
//    fun before() {
//        defaultLocale = Locale.getDefault()
//    }
//
//    @After
//    fun after() {
//        Locale.setDefault(defaultLocale)
//    }

    @Test
    fun dateCreation() {
        val date = model.getCanonicalDate(January2022Millis) // 1/1/2022
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateCreation_withRounding() {
        val date = model.getCanonicalDate(January2022Millis + 30000) // 1/1/2022 + 30000 millis
        assertThat(date.year).isEqualTo(2022)
        assertThat(date.month).isEqualTo(1)
        assertThat(date.dayOfMonth).isEqualTo(1)
        // Check that the milliseconds represent the start of the day.
        assertThat(date.utcTimeMillis).isEqualTo(January2022Millis)
    }

    @Test
    fun dateRestore() {
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
    fun monthCreation_withRounding() {
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
    fun monthRestore() {
        val month = model.getMonth(year = 1999, month = 12)
        assertThat(model.getMonth(month.startUtcTimeMillis)).isEqualTo(month)
    }

    @Test
    fun plusMinusMonth() {
        val month = model.getMonth(January2022Millis) // 1/1/2022
        val expectedNextMonth = model.getMonth(month.endUtcTimeMillis + 1) // 2/1/2022
        val plusMonth = model.plusMonths(from = month, addedMonthsCount = 1)
        assertThat(plusMonth).isEqualTo(expectedNextMonth)
        assertThat(model.minusMonths(from = plusMonth, subtractedMonthsCount = 1)).isEqualTo(month)
    }

    @Test
    fun parseDate() {
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

//    @Test
//    fun formatDate() {
//        val date =
//            CalendarDate(
//                year = 2022,
//                month = 1,
//                dayOfMonth = 1,
//                utcTimeMillis = androidx.compose.material3.January2022Millis
//            )
//        assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo("Jan 1, 2022")
//        assertThat(model.formatWithSkeleton(date, "dMMMy")).isEqualTo("Jan 1, 2022")
//        assertThat(model.formatWithSkeleton(date, "yMMMMEEEEd"))
//            .isEqualTo("Saturday, January 1, 2022")
//        // Check that the direct formatting is equal to the one the model does.
//        assertThat(model.formatWithSkeleton(date, "yMMMd")).isEqualTo(date.format(model, "yMMMd"))
//    }
//
//    @Test
//    fun formatMonth() {
//        val month = model.getMonth(year = 2022, month = 3)
//        assertThat(model.formatWithSkeleton(month, "yMMMM")).isEqualTo("March 2022")
//        assertThat(model.formatWithSkeleton(month, "MMMMy")).isEqualTo("March 2022")
//        // Check that the direct formatting is equal to the one the model does.
//        assertThat(model.formatWithSkeleton(month, "yMMMM"))
//            .isEqualTo(month.format(model, "yMMMM"))
//    }

//    @Test
//    fun weekdayNames() {
//        // Ensure we are running on a US locale for this test.
//        Locale.setDefault(Locale.US)
//        val weekDays = model.weekdayNames
//        assertThat(weekDays).hasSize(DaysInWeek)
//        // Check that the first day is always "Monday", per ISO-8601 standard.
//        assertThat(weekDays.first().first).ignoringCase().contains("Monday")
//        weekDays.forEach {
//            assertThat(it.second.first().lowercaseChar()).isEqualTo(
//                it.first.first().lowercaseChar()
//            )
//        }
//    }

//    @Test
//    fun dateInputFormat() {
//        Locale.setDefault(Locale.US)
//        assertThat(model.getDateInputFormat().patternWithDelimiters).isEqualTo("MM/dd/yyyy")
//        assertThat(model.getDateInputFormat().patternWithoutDelimiters).isEqualTo("MMddyyyy")
//        assertThat(model.getDateInputFormat().delimiter).isEqualTo('/')
//
//        Locale.setDefault(Locale.CHINA)
//        assertThat(model.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy/MM/dd")
//        assertThat(model.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
//        assertThat(model.getDateInputFormat().delimiter).isEqualTo('/')
//
//        Locale.setDefault(Locale.UK)
//        assertThat(model.getDateInputFormat().patternWithDelimiters).isEqualTo("dd/MM/yyyy")
//        assertThat(model.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
//        assertThat(model.getDateInputFormat().delimiter).isEqualTo('/')
//
//        Locale.setDefault(Locale.KOREA)
//        assertThat(model.getDateInputFormat().patternWithDelimiters).isEqualTo("yyyy.MM.dd")
//        assertThat(model.getDateInputFormat().patternWithoutDelimiters).isEqualTo("yyyyMMdd")
//        assertThat(model.getDateInputFormat().delimiter).isEqualTo('.')
//
//        Locale.setDefault(Locale("es", "CL"))
//        assertThat(model.getDateInputFormat().patternWithDelimiters).isEqualTo("dd-MM-yyyy")
//        assertThat(model.getDateInputFormat().patternWithoutDelimiters).isEqualTo("ddMMyyyy")
//        assertThat(model.getDateInputFormat().delimiter).isEqualTo('-')
//    }
}

private const val January2022Millis = 1640995200000
