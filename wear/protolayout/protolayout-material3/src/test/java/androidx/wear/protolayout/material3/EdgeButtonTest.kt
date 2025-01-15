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

package androidx.wear.protolayout.material3

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.wear.protolayout.ActionBuilders.launchAction
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Image
import androidx.wear.protolayout.expression.AppDataKey
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicInt32
import androidx.wear.protolayout.material3.EdgeButtonDefaults.BOTTOM_MARGIN_DP
import androidx.wear.protolayout.material3.EdgeButtonDefaults.EDGE_BUTTON_HEIGHT_DP
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.testing.LayoutElementAssertionsProvider
import androidx.wear.protolayout.testing.LayoutElementMatcher
import androidx.wear.protolayout.testing.hasColor
import androidx.wear.protolayout.testing.hasContentDescription
import androidx.wear.protolayout.testing.hasHeight
import androidx.wear.protolayout.testing.hasImage
import androidx.wear.protolayout.testing.hasText
import androidx.wear.protolayout.testing.hasWidth
import androidx.wear.protolayout.testing.isClickable
import androidx.wear.protolayout.types.LayoutString
import androidx.wear.protolayout.types.asLayoutConstraint
import androidx.wear.protolayout.types.layoutString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.internal.DoNotInstrument

@RunWith(AndroidJUnit4::class)
@DoNotInstrument
class EdgeButtonTest {
    @Test
    fun containerSize() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onRoot()
            .assert(hasWidth(DEVICE_CONFIGURATION.screenWidthDp.toDp()))
            .assert(hasHeight((EDGE_BUTTON_HEIGHT_DP + BOTTOM_MARGIN_DP).toDp()))
    }

    @Test
    fun visibleHeight() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasHeight(EDGE_BUTTON_HEIGHT_DP.toDp()))
    }

    @Test
    fun contentDescription() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasContentDescription(CONTENT_DESCRIPTION))
    }

    @Test
    fun defaultBackgroundColor() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(isClickable())
            .assert(hasColor(COLOR_SCHEME.primary.staticArgb))
    }

    @Test
    fun icon() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON).onElement(hasImage(RES_ID)).assertExists()
    }

    @Test
    fun iconColor() {
        LayoutElementAssertionsProvider(ICON_EDGE_BUTTON)
            .onElement(hasImage(RES_ID))
            .assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))
    }

    @Test
    fun customColors() {
        val queryProvider =
            LayoutElementAssertionsProvider(
                materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                    iconEdgeButton(
                        onClick = CLICKABLE,
                        modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION),
                        colors =
                            ButtonColors(
                                container = COLOR_SCHEME.tertiaryContainer,
                                icon = COLOR_SCHEME.onTertiary
                            ),
                    ) {
                        icon(RES_ID)
                    }
                }
            )

        queryProvider
            .onElement(isClickable())
            .assert(hasColor(COLOR_SCHEME.tertiaryContainer.staticArgb))
        queryProvider
            .onElement(LayoutElementMatcher("Element type is Image") { it is Image })
            .assert(hasColor(COLOR_SCHEME.onTertiary.staticArgb))
    }

    @Test
    fun staticText() {
        val label = "static text"
        val textEdgeButton =
            materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                textEdgeButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                ) {
                    text(label.layoutString)
                }
            }

        LayoutElementAssertionsProvider(textEdgeButton)
            .onElement(hasText(label))
            .assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))
    }

    @Test
    fun dynamicText() {
        val label = "test text"
        val stateKey = AppDataKey<DynamicInt32>("testKey")
        val dynamicLabel =
            LayoutString(
                label,
                DynamicInt32.from(stateKey).times(2).format(),
                label.asLayoutConstraint()
            )

        val queryProvider =
            LayoutElementAssertionsProvider(
                materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                    textEdgeButton(
                        onClick = CLICKABLE,
                        modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                    ) {
                        this.text(dynamicLabel)
                    }
                }
            )

        queryProvider
            .onElement(hasText(dynamicLabel))
            .assert(hasColor(COLOR_SCHEME.onPrimary.staticArgb))
    }

    companion object {
        private val CONTEXT = getApplicationContext() as Context
        private val COLOR_SCHEME = ColorScheme()

        private val DEVICE_CONFIGURATION =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(192)
                .setScreenHeightDp(192)
                .build()

        private val CLICKABLE =
            clickable(action = launchAction(ComponentName("pkg", "cls")), id = "action_id")

        private const val CONTENT_DESCRIPTION = "it is an edge button"

        private const val RES_ID = "resId"
        private val ICON_EDGE_BUTTON =
            materialScope(CONTEXT, DEVICE_CONFIGURATION, allowDynamicTheme = false) {
                iconEdgeButton(
                    onClick = CLICKABLE,
                    modifier = LayoutModifier.contentDescription(CONTENT_DESCRIPTION)
                ) {
                    icon(RES_ID)
                }
            }
    }
}
