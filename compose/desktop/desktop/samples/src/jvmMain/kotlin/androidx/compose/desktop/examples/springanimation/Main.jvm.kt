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

package androidx.compose.desktop.examples.springanimation

import androidx.compose.animation.core.IOSBasedSpring
import androidx.compose.animation.core.IOSBasedSpringSolution
import androidx.compose.animation.core.Spring.DampingRatioHighBouncy
import androidx.compose.animation.core.Spring.DampingRatioNoBouncy
import androidx.compose.animation.core.Spring.StiffnessVeryLow
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.singleWindowApplication
import kotlin.math.roundToInt

//fun main() = singleWindowApplication {
//    var offset by remember { mutableStateOf(0f) }
//    var velocity by remember { mutableStateOf(1000f) }
//
//    var boo by remember { mutableStateOf(0) }
//
//    LaunchedEffect(boo) {
//        animate(offset, 0f, 1000f, spring(dampingRatio = DampingRatioNoBouncy, stiffness = StiffnessVeryLow)) { newValue, newVelocity ->
//            offset = newValue
//            velocity = newVelocity
//        }
//    }
//
//    Box(Modifier.offset { Offset(0f, offset).round() }) {
//        Box(Modifier.background(Color.Red).size(100.dp).clickable {
//            boo += 1
//        })
//    }
//}

fun main() = singleWindowApplication(
    state = WindowState(width = 1024.dp, height = 850.dp),
) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(100) {
            Text("Row $it", Modifier.padding(16.dp))
        }
    }
}