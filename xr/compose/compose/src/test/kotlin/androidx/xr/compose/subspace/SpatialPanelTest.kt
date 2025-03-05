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

package androidx.xr.compose.subspace

import android.view.View
import android.widget.TextView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.xr.compose.subspace.layout.CorePanelEntity
import androidx.xr.compose.subspace.layout.SpatialRoundedCornerShape
import androidx.xr.compose.subspace.layout.SubspaceModifier
import androidx.xr.compose.subspace.layout.height
import androidx.xr.compose.subspace.layout.testTag
import androidx.xr.compose.subspace.layout.width
import androidx.xr.compose.testing.SubspaceTestingActivity
import androidx.xr.compose.testing.onSubspaceNodeWithTag
import androidx.xr.compose.testing.setSubspaceContent
import com.google.common.truth.Truth.assertThat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Tests for [SpatialPanel]. */
@RunWith(AndroidJUnit4::class)
class SpatialPanelTest {
    @get:Rule val composeTestRule = createAndroidComposeRule<SubspaceTestingActivity>()

    @Test
    fun spatialPanel_internalElementsAreLaidOutProperly() {
        composeTestRule.setSubspaceContent {
            SpatialPanel(SubspaceModifier.width(100.dp).testTag("panel")) {
                // Row with 2 elements, one is 3x as large as the other
                Row {
                    Spacer(Modifier.testTag("spacer1").weight(1f))
                    Spacer(Modifier.testTag("spacer2").weight(3f))
                }
            }
        }
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithTag("spacer1").assertWidthIsEqualTo(25.dp)
        composeTestRule.onNodeWithTag("spacer2").assertWidthIsEqualTo(75.dp)
    }

    @Test
    fun spatialPanel_textTooLong_panelDoesNotGrowBeyondSpecifiedWidth() {
        composeTestRule.setSubspaceContent {
            // Panel with 10dp width, way too small for the text we're putting into it
            SpatialPanel(SubspaceModifier.width(10.dp).testTag("panel")) {
                // Panel contains a column.
                Column {
                    Text("Hello World long text", style = MaterialTheme.typography.headlineLarge)
                }
            }
        }
        // Text element stays 10dp long, even though it needs more space, as the Panel will not grow
        // for the text.
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText("Hello World long text").assertWidthIsEqualTo(10.dp)
    }

    @Test
    fun spatialPanel_viewBasedPanelComposes() {
        composeTestRule.setSubspaceContent {
            val context = LocalContext.current
            val textView = remember { TextView(context).apply { text = "Hello World" } }
            SpatialPanel(view = textView, SubspaceModifier.testTag("panel"))
            // The View is not inserted in the compose tree, we need to test it differentlly
            assertEquals(View.VISIBLE, textView.visibility)
        }
        // TODO: verify that the TextView is add to the Panel
        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
    }

    @Test
    fun mainPanel_renders() {
        val text = "Main Window Text"
        composeTestRule.setSubspaceContent({ Text(text) }) {
            MainPanel(SubspaceModifier.testTag("panel"))
        }

        composeTestRule.onSubspaceNodeWithTag("panel").assertExists()
        composeTestRule.onNodeWithText(text).assertExists()
    }

    @Test
    fun mainPanel_addedTwice_asserts() {
        val text = "Main Window Text"

        assertThrows(IllegalStateException::class.java) {
            composeTestRule.setSubspaceContent({ Text(text) }) {
                MainPanel(SubspaceModifier.testTag("panel"))
                MainPanel(SubspaceModifier.testTag("panel2"))
            }
        }
    }

    @Test
    fun mainPanel_addedTwiceInDifferentSubtrees_asserts() {
        val text = "Main Window Text"

        assertThrows(IllegalStateException::class.java) {
            composeTestRule.setSubspaceContent({ Text(text) }) {
                SpatialColumn {
                    SpatialRow { MainPanel(SubspaceModifier.testTag("panel")) }
                    SpatialRow { MainPanel(SubspaceModifier.testTag("panel2")) }
                }
            }
        }
    }

    @Test
    fun spatialPanel_cornerRadius_dp() {
        val density = Density(1.0f)
        composeTestRule.setSubspaceContent {
            SpatialPanel(
                modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                shape = SpatialRoundedCornerShape(CornerSize(32.dp)),
            ) {}
        }

        assertThat(getCorePanelEntity("panel")?.getCornerRadius(density)).isEqualTo(32f)
    }

    @Test
    fun spatialPanel_cornerRadius_percent() {
        val density = Density(1.0f)
        composeTestRule.setSubspaceContent {
            SpatialPanel(
                modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                shape = SpatialRoundedCornerShape(CornerSize(50)),
            ) {}
        }

        // 50 percent of the shorter side (200.dp) at 1.0 Density is 100 pixels.
        assertThat(getCorePanelEntity("panel")?.getCornerRadius(density)).isEqualTo(100f)
    }

    @Test
    fun spatialPanel_cornerRadius_increasedDensity() {
        val density = Density(3.0f)
        composeTestRule.setSubspaceContent {
            SpatialPanel(
                modifier = SubspaceModifier.width(200.dp).height(300.dp).testTag("panel"),
                shape = SpatialRoundedCornerShape(CornerSize(50)),
            ) {}
        }

        // 50 percent of the shorter side (200.dp) at 3.0 Density is 300 pixels.
        assertThat(getCorePanelEntity("panel")?.getCornerRadius(density)).isEqualTo(300f)
    }

    private fun getCorePanelEntity(tag: String): CorePanelEntity? {
        return composeTestRule.onSubspaceNodeWithTag(tag).fetchSemanticsNode().coreEntity
            as? CorePanelEntity
    }
}
