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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.FontHinting
import androidx.compose.ui.text.FontRasterizationSettings
import androidx.compose.ui.text.FontSmoothing
import androidx.compose.ui.text.PlatformParagraphStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalTextApi::class)
@Composable
fun FontRasterization() {
    val state = rememberScrollState()
    Column(
        modifier = Modifier.fillMaxSize().padding(10.dp).verticalScroll(state),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val hintingOptions = listOf(FontHinting.None, FontHinting.Slight, FontHinting.Normal, FontHinting.Full)
        val isAutoHintingForcedOptions = listOf(false, true)
        val subpixelOptions = listOf(false, true)
        val smoothingOptions = listOf(FontSmoothing.None, FontSmoothing.AntiAlias, FontSmoothing.SubpixelAntiAlias)
        val text = "Lorem ipsum"

        for (hinting in hintingOptions) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                for (smoothing in smoothingOptions) {
                    Column(
                        modifier = Modifier.weight(1f).border(1.dp, Color.Black).padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (subpixel in subpixelOptions) {
                            for (autoHintingForced in isAutoHintingForcedOptions) {
                                FontRasterizationSample(
                                    text = text,
                                    modifier = Modifier.offset(x = 0.25.dp, y = 0.25.dp),
                                    hinting = hinting,
                                    smoothing = smoothing,
                                    subpixelPositioning = subpixel,
                                    autoHintingForced = autoHintingForced
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun FontRasterizationSample(
    text: String,
    modifier: Modifier,
    hinting: FontHinting,
    smoothing: FontSmoothing,
    subpixelPositioning: Boolean,
    autoHintingForced: Boolean
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = TextStyle(
            platformStyle = PlatformTextStyle(
                spanStyle = null,
                paragraphStyle = PlatformParagraphStyle(
                    fontRasterizationSettings = FontRasterizationSettings(
                        smoothing = smoothing,
                        hinting = hinting,
                        subpixelPositioning = subpixelPositioning,
                        autoHintingForced = autoHintingForced
                    )
                )
            )
        )
    )
}
