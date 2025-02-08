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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.toFontFamily
import androidx.compose.ui.text.platform.Font
import androidx.compose.ui.unit.sp

@Composable
fun VariableFonts() {
    var robotFlexFontByteArray: ByteArray? by remember { mutableStateOf(null) }
    LaunchedEffect(Unit) {
        robotFlexFontByteArray = loadResource("RobotoFlex-VariableFont.ttf")
    }
    var opsz by remember { mutableStateOf(14f) }
    var slnt by remember { mutableStateOf(0f) }
    var wdth by remember { mutableStateOf(100f) }
    var wght by remember { mutableStateOf(400f) }
    var GRAD by remember { mutableStateOf(0f) }
    var XOPQ by remember { mutableStateOf(96f) }
    var XTRA by remember { mutableStateOf(468f) }
    var YOPQ by remember { mutableStateOf(79f) }
    var YTAS by remember { mutableStateOf(750f) }
    var YTDE by remember { mutableStateOf(-203f) }
    var YTFI by remember { mutableStateOf(738f) }
    var YTLC by remember { mutableStateOf(514f) }
    var YTUC by remember { mutableStateOf(712f) }
    val variationSettings = FontVariation.Settings(
        FontVariation.Setting("opsz", opsz),
        FontVariation.Setting("slnt", slnt),
        FontVariation.Setting("wdth", wdth),
        FontVariation.Setting("wght", wght),
        FontVariation.Setting("GRAD", GRAD),
        FontVariation.Setting("XOPQ", XOPQ),
        FontVariation.Setting("XTRA", XTRA),
        FontVariation.Setting("YOPQ", YOPQ),
        FontVariation.Setting("YTAS", YTAS),
        FontVariation.Setting("YTDE", YTDE),
        FontVariation.Setting("YTFI", YTFI),
        FontVariation.Setting("YTLC", YTLC),
        FontVariation.Setting("YTUC", YTUC),
    )
    val fontFamily =
        robotFlexFontByteArray?.let {
            Font(
                identity = "RobotFlex ${variationSettings.hashCode()}",
                data = it,
                variationSettings = variationSettings
            ).toFontFamily()
        } ?: FontFamily.Default
    Column {
        Text(
            """
                116 Part
                O, no! it is an ever-fix`ed mark,
                That looks on tempests and is never shaken;
            """.trimIndent(),
            fontFamily = fontFamily,
            fontSize = 32.sp
        )
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text("opsz $opsz")
            Slider(
                value = opsz,
                onValueChange = { opsz = it },
                valueRange = 8f..144f,
            )
            Text("slnt $slnt")
            Slider(
                value = slnt,
                onValueChange = { slnt = it },
                valueRange = -10f..0f
            )
            Text("wdth $wdth")
            Slider(
                value = wdth,
                onValueChange = { wdth = it },
                valueRange = 25f..151f
            )
            Text("wght $wght")
            Slider(
                value = wght,
                onValueChange = { wght = it },
                valueRange = 100f..1000f
            )
            Text("GRAD $GRAD")
            Slider(
                value = GRAD,
                onValueChange = { GRAD = it },
                valueRange = -200f..150f
            )
            Text("XOPQ $XOPQ")
            Slider(
                value = XOPQ,
                onValueChange = { XOPQ = it },
                valueRange = 27f..175f
            )
            Text("XTRA $XTRA")
            Slider(
                value = XTRA,
                onValueChange = { XTRA = it },
                valueRange = 323f..603f
            )
            Text("YOPQ $YOPQ")
            Slider(
                value = YOPQ,
                onValueChange = { YOPQ = it },
                valueRange = 25f..135f
            )
            Text("YTAS $YTAS")
            Slider(
                value = YTAS,
                onValueChange = { YTAS = it },
                valueRange = 649f..854f
            )
            Text("YTDE $YTDE")
            Slider(
                value = YTDE,
                onValueChange = { YTDE = it },
                valueRange = -305f..-98f
            )
            Text("YTFI $YTFI")
            Slider(
                value = YTFI,
                onValueChange = { YTFI = it },
                valueRange = 560f..788f
            )
            Text("YTLC $YTLC")
            Slider(
                value = YTLC,
                onValueChange = { YTLC = it },
                valueRange = 416f..570f
            )
            Text("YTUC $YTUC")
            Slider(
                value = YTUC,
                onValueChange = { YTUC = it },
                valueRange = 528f..760f
            )
        }
    }
}

expect suspend fun loadResource(file: String): ByteArray?