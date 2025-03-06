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

package androidx.compose.mpp.demo.components.text

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.LineHeightStyle.Alignment
import androidx.compose.ui.text.style.LineHeightStyle.Trim
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LineHeightStyleDemo() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(5.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement
            .spacedBy(5.dp)
    ) {
        for (trim in sequenceOf(
            Trim.FirstLineTop,
            Trim.LastLineBottom,
            Trim.Both,
            Trim.None,
        )) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                for (alignment in sequenceOf(
                    Alignment.Top,
                    Alignment.Center,
                    Alignment.Proportional,
                    Alignment.Bottom,
                )) {
                    LineHeightStyleShowcase(
                        modifier = Modifier
                            .weight(1f)
                            .border(1.dp, Color.Black)
                            .padding(5.dp),
                        lineHeightStyle = LineHeightStyle(
                            trim = trim,
                            alignment = alignment
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun LineHeightStyleShowcase(modifier: Modifier, lineHeightStyle: LineHeightStyle) {
    Column(modifier) {
        val labelStyle = TextStyle.Default.copy(
            fontSize = 8.sp,
            fontFamily = FontFamily.Monospace,
        )
        BasicText(lineHeightStyle.alignment.toString(), style = labelStyle)
        BasicText(lineHeightStyle.trim.toString(), style = labelStyle)
        Spacer(modifier = Modifier.height(5.dp))

        val demoTextStyle = TextStyle.Default.copy(
            fontSize = 36.sp,
            lineHeight = 96.sp,
            lineHeightStyle = lineHeightStyle,
        )
        TextWithMetrics("Lorem Ipsum", style = demoTextStyle )
    }
}

