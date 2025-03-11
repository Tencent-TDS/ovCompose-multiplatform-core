/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.wear.compose.material3.samples

import androidx.annotation.Sampled
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.paint
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.TransformingLazyColumn
import androidx.wear.compose.material3.Button
import androidx.wear.compose.material3.SurfaceTransformation
import androidx.wear.compose.material3.Text
import androidx.wear.compose.material3.TitleCard
import androidx.wear.compose.material3.lazy.rememberResponsiveTransformationSpec

@Sampled
@Preview
@Composable
fun SurfaceTransformationOnCustomComponent() {
    @Composable
    fun MyCardComponent(
        title: String,
        body: String,
        transformation: SurfaceTransformation,
        modifier: Modifier = Modifier
    ) {
        Column(
            modifier =
                modifier
                    .fillMaxWidth()
                    .paint(
                        transformation.createBackgroundPainter(
                            ColorPainter(color = Color.Gray),
                            shape = RoundedCornerShape(16.dp)
                        )
                    )
                    .graphicsLayer { with(transformation) { applyTransformation() } }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(title)
            Text(body)
        }
    }

    val transformationSpec = rememberResponsiveTransformationSpec()

    TransformingLazyColumn {
        items(count = 100) {
            MyCardComponent(
                "Message #$it",
                "This is a body",
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(transformationSpec::getTransformedHeight),
            )
        }
    }
}

@Sampled
@Preview
@Composable
fun SurfaceTransformationButtonSample() {
    val transformationSpec = rememberResponsiveTransformationSpec()

    TransformingLazyColumn {
        items(count = 100) {
            Button(
                onClick = {},
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(transformationSpec::getTransformedHeight),
            ) {
                Text("Button #$it")
            }
        }
    }
}

@Sampled
@Preview
@Composable
fun SurfaceTransformationCardSample() {
    val transformationSpec = rememberResponsiveTransformationSpec()
    var expandedIndex by remember { mutableIntStateOf(-1) }

    TransformingLazyColumn {
        items(count = 100) {
            TitleCard(
                onClick = { expandedIndex = if (expandedIndex == it) -1 else it },
                title = { Text("Card #$it") },
                subtitle = { Text("Subtitle #$it") },
                transformation = SurfaceTransformation(transformationSpec),
                modifier = Modifier.transformedHeight(transformationSpec::getTransformedHeight),
            ) {
                if (it == expandedIndex) {
                    Text("Expanded content #$it")
                }
            }
        }
    }
}
