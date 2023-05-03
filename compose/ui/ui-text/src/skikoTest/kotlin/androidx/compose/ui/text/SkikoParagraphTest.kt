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

package androidx.compose.ui.text

import androidx.compose.ui.text.font.createFontFamilyResolver
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

// Adopted tests from text/text/src/androidTest/java/androidx/compose/ui/text/android/selection/WordBoundaryTest.kt
class SkikoParagraphTest {
    private val fontFamilyResolver = createFontFamilyResolver()
    private val defaultDensity = Density(density = 1f)

    @Test
    fun getWordBoundary_out_of_boundary_too_small() {
        val text = "text"
        val paragraph = simpleParagraph(text)

        assertFailsWith<IllegalArgumentException> {
            paragraph.getWordBoundary(-1)
        }
    }
    @Test
    fun getWordBoundary_out_of_boundary_too_big() {
        val text = "text"
        val paragraph = simpleParagraph(text)

        assertFailsWith<IllegalArgumentException> {
            paragraph.getWordBoundary(text.length + 1)
        }
    }

    @Test
    fun getWordBoundary_empty_string() {
        val paragraph = simpleParagraph("")

        paragraph.getWordBoundary(0).apply {
            assertEquals(0, start)
            assertEquals(0, end)
        }
    }

    @Test
    fun getWordBoundary() {
        val text = "abc def-ghi. jkl"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(text.indexOf('a')).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf(' '), end)
        }
        paragraph.getWordBoundary(text.indexOf('c')).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf(' '), end)
        }
        paragraph.getWordBoundary(text.indexOf(' ')).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf(' '), end)
        }
        paragraph.getWordBoundary(text.indexOf('d')).apply {
            assertEquals(text.indexOf('d'), start)
            assertEquals(text.indexOf('-'), end)
        }
        paragraph.getWordBoundary(text.indexOf('i')).apply {
            assertEquals(text.indexOf('g'), start)
            assertEquals(text.indexOf('.'), end)
        }
        paragraph.getWordBoundary(text.indexOf('k')).apply {
            assertEquals(text.indexOf('j'), start)
            assertEquals(text.indexOf('l') + 1, end)
        }
    }

    @Test
    fun getWordBoundary_spaces() {
        val text = "ab cd  e"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(text.indexOf('b') + 1).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf('b') + 1, end)
        }
        paragraph.getWordBoundary(text.indexOf('c')).apply {
            assertEquals(text.indexOf('c'), start)
            assertEquals(text.indexOf('d') + 1, end)
        }
        paragraph.getWordBoundary(text.indexOf('d') + 2).apply {
            assertEquals(text.indexOf('d') + 2, start)
            assertEquals(text.indexOf('d') + 2, end)
        }
    }

    @Test
    fun getWordBoundary_RTL() { // Hebrew -- "אבג דה-וז. חט"
        val text = "\u05d0\u05d1\u05d2 \u05d3\u05d4-\u05d5\u05d6. \u05d7\u05d8"
        val paragraph = simpleParagraph(text)


        paragraph.getWordBoundary(text.indexOf('\u05d0')).apply {
            assertEquals(text.indexOf('\u05d0'), start)
            assertEquals(text.indexOf(' '), end)
        }

        paragraph.getWordBoundary(text.indexOf('\u05d2')).apply {
            assertEquals(text.indexOf('\u05d0'), start)
            assertEquals(text.indexOf(' '), end)
        }

        paragraph.getWordBoundary(text.indexOf(' ')).apply {
            assertEquals(text.indexOf('\u05d0'), start)
            assertEquals(text.indexOf(' '), end)
        }

        paragraph.getWordBoundary(text.indexOf('\u05d4')).apply {
            assertEquals(text.indexOf('\u05d3'), start)
            assertEquals(text.indexOf('-'), end)
        }

        paragraph.getWordBoundary(text.indexOf('-')).apply {
            assertEquals(text.indexOf('\u05d3'), start)
            assertEquals(text.indexOf('-') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('\u05d5')).apply {
            assertEquals(text.indexOf('-'), start)
            assertEquals(text.indexOf('.'), end)
        }

        paragraph.getWordBoundary(text.indexOf('\u05d6')).apply {
            assertEquals(text.indexOf('\u05d5'), start)
            assertEquals(text.indexOf('.'), end)
        }

        paragraph.getWordBoundary(text.indexOf('\u05d7')).apply {
            assertEquals(text.indexOf('\u05d7'), start)
            assertEquals(text.length, end)
        }
    }

    @Test
    fun getWordBoundary_CJK() { // Japanese HIRAGANA letter + KATAKANA letters
        val text = "\u3042\u30A2\u30A3\u30A4"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(text.indexOf('\u3042')).apply {
            assertEquals(text.indexOf('\u3042'), start)
            assertEquals(text.indexOf('\u3042') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('\u30A2')).apply {
            assertEquals(text.indexOf('\u3042'), start)
            assertEquals(text.indexOf('\u30A4') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('\u30A4')).apply {
            assertEquals(text.indexOf('\u30A2'), start)
            assertEquals(text.indexOf('\u30A4') + 1, end)
        }

        paragraph.getWordBoundary(text.length).apply {
            assertEquals(text.indexOf('\u30A2'), start)
            assertEquals(text.indexOf('\u30A4') + 1, end)
        }
    }

    @Test
    fun getWordBoundary_apostropheMiddleOfWord() {
        // These tests confirm that the word "isn't" is treated like one word.
        val text = "isn't he"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(text.indexOf('i')).apply {
            assertEquals(text.indexOf('i'), start)
            assertEquals(text.indexOf('t') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('n')).apply {
            assertEquals(text.indexOf('i'), start)
            assertEquals(text.indexOf('t') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('\'')).apply {
            assertEquals(text.indexOf('i'), start)
            assertEquals(text.indexOf('t') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('t')).apply {
            assertEquals(text.indexOf('i'), start)
            assertEquals(text.indexOf('t') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('t') + 1).apply {
            assertEquals(text.indexOf('i'), start)
            assertEquals(text.indexOf('t') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('h')).apply {
            assertEquals(text.indexOf('h'), start)
            assertEquals(text.indexOf('e') + 1, end)
        }
    }

    @Test
    fun getWordBoundary_isOnPunctuation() {
        val text = "abc!? (^^;) def"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(text.indexOf('a')).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf('!'), end)
        }

        paragraph.getWordBoundary(text.indexOf('!')).apply {
            assertEquals(text.indexOf('a'), start)
            assertEquals(text.indexOf('?') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('?') + 1).apply {
            assertEquals(text.indexOf('!'), start)
            assertEquals(text.indexOf('?') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('(')).apply {
            assertEquals(text.indexOf('('), start)
            assertEquals(text.indexOf('(') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('(') + 2).apply {
            assertEquals(text.indexOf('(') + 2, start)
            assertEquals(text.indexOf('(') + 2, end)
        }

        paragraph.getWordBoundary(text.indexOf(';')).apply {
            assertEquals(text.indexOf(';'), start)
            assertEquals(text.indexOf(')') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf(')')).apply {
            assertEquals(text.indexOf(';'), start)
            assertEquals(text.indexOf(')') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf(')') + 1).apply {
            assertEquals(text.indexOf(';'), start)
            assertEquals(text.indexOf(')') + 1, end)
        }

        paragraph.getWordBoundary(text.indexOf('d')).apply {
            assertEquals(text.indexOf('d'), start)
            assertEquals(text.length, end)
        }

        paragraph.getWordBoundary(text.length).apply {
            assertEquals(text.indexOf('d'), start)
            assertEquals(text.length, end)
        }
    }

    @Test
    fun getWordBoundary_emoji() {
        // "ab 🧑🏿‍🦰 cd" - example of complex emoji
        //             | (offset=3)      | (offset=6)
        val text = "ab \uD83E\uDDD1\uD83C\uDFFF\u200D\uD83E\uDDB0 cd"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(6).apply {
            assertEquals(3, start)
            assertEquals(10, end)
        }
    }

    @Test
    fun getWordBoundary_multichar() {
        // "ab 𐐔𐐯𐑅𐐨𐑉𐐯𐐻 cd" - example of multi-char code units
        //             | (offset=3)      | (offset=6)
        val text = "ab \uD801\uDC14\uD801\uDC2F\uD801\uDC45\uD801\uDC28\uD801\uDC49\uD801\uDC2F\uD801\uDC3B cd"
        val paragraph = simpleParagraph(text)

        paragraph.getWordBoundary(6).apply {
            assertEquals(3, start)
            assertEquals(17, end)
        }
    }

    private fun simpleParagraph(text: String) = Paragraph(
        text = text,
        style = TextStyle(),
        constraints = Constraints(maxWidth = 1000),
        density = defaultDensity,
        fontFamilyResolver = fontFamilyResolver
    )
}