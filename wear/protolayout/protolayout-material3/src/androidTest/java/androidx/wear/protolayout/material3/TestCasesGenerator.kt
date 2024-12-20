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

import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.Modifiers
import androidx.wear.protolayout.material3.ButtonDefaults.filledButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledTonalButtonColors
import androidx.wear.protolayout.material3.ButtonDefaults.filledVariantButtonColors
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.MaterialGoldenTest.Companion.pxToDp
import androidx.wear.protolayout.modifiers.LayoutModifier
import androidx.wear.protolayout.modifiers.contentDescription
import androidx.wear.protolayout.types.LayoutColor
import androidx.wear.protolayout.types.layoutString
import com.google.common.collect.ImmutableMap

private const val CONTENT_DESCRIPTION_PLACEHOLDER = "Description"

object TestCasesGenerator {
    private const val ICON_ID: String = "icon"
    private const val IMAGE_ID: String = "avatar_image"
    private const val NORMAL_SCALE_SUFFIX: String = ""

    /**
     * This function will append goldenSuffix on the name of the golden images that should be
     * different for different user font sizes. Note that some of the golden will have the same name
     * as it should point on the same size independent image. These test cases are meant to be
     * tested in RTL and LTR modes.
     */
    fun generateTestCases(
        goldenSuffix: String = NORMAL_SCALE_SUFFIX
    ): ImmutableMap<String, LayoutElementBuilders.Layout> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val displayMetrics = context.resources.displayMetrics
        val scale = displayMetrics.density

        val deviceParameters =
            DeviceParametersBuilders.DeviceParameters.Builder()
                .setScreenWidthDp(pxToDp(RunnerUtils.SCREEN_SIZE_SMALL, scale))
                .setScreenHeightDp(pxToDp(RunnerUtils.SCREEN_SIZE_SMALL, scale))
                .setScreenDensity(displayMetrics.density)
                .setFontScale(1f)
                .setScreenShape(DeviceParametersBuilders.SCREEN_SHAPE_RECT)
                .build()
        val clickable: ModifiersBuilders.Clickable =
            ModifiersBuilders.Clickable.Builder()
                .setOnClick(ActionBuilders.LaunchAction.Builder().build())
                .setId("action_id")
                .build()
        val testCases: HashMap<String, LayoutElementBuilders.LayoutElement> = HashMap()

        testCases["primarylayout_edgebuttonfilled_buttongroup_iconoverride_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayoutWithOverrideIcon(
                    mainSlot = {
                        text(
                            text = "Text in the main slot that overflows".layoutString,
                            color = colorScheme.secondary
                        )
                    },
                    bottomSlot = {
                        textEdgeButton(
                            onClick = clickable,
                            labelContent = { text("Action".layoutString) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledButtonColors()
                        )
                    },
                    titleSlot = { text("Title".layoutString) },
                    overrideIcon = true
                )
            }
        testCases["primarylayout_edgebuttonfilledvariant_iconoverride_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayoutWithOverrideIcon(
                    mainSlot = {
                        buttonGroup {
                            buttonGroupItem {
                                coloredBox(color = colorScheme.secondary, shape = shapes.full)
                            }
                            buttonGroupItem {
                                coloredBox(color = colorScheme.secondaryDim, shape = shapes.small)
                            }
                            buttonGroupItem {
                                coloredBox(
                                    color = colorScheme.secondaryContainer,
                                    shape = shapes.large
                                )
                            }
                        }
                    },
                    bottomSlot = {
                        textEdgeButton(
                            onClick = clickable,
                            labelContent = { text("Action that overflows".layoutString) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledVariantButtonColors()
                        )
                    },
                    overrideIcon = true
                )
            }
        testCases["primarylayout_edgebuttonfilledtonal_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        card(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            width = expand(),
                            height = expand(),
                            background = { backgroundImage(protoLayoutResourceId = IMAGE_ID) }
                        ) {
                            text(
                                "Card with image background".layoutString,
                                color = colorScheme.onBackground
                            )
                        }
                    },
                    bottomSlot = {
                        iconEdgeButton(
                            onClick = clickable,
                            iconContent = { icon(ICON_ID) },
                            modifier =
                                LayoutModifier.contentDescription(CONTENT_DESCRIPTION_PLACEHOLDER),
                            colors = filledTonalButtonColors()
                        )
                    },
                    titleSlot = {
                        text("Title that overflows".layoutString, color = colorScheme.error)
                    }
                )
            }
        testCases["primarylayout_titlecard_bottomslot_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        titleCard(
                            onClick = clickable,
                            modifier = LayoutModifier.contentDescription("Card"),
                            height = expand(),
                            title = {
                                text(
                                    "Title Card text that will overflow after 2 max lines of text"
                                        .layoutString
                                )
                            },
                            time = { text("Now".layoutString) },
                            content = { text("Default title card".layoutString) },
                            colors = filledVariantCardColors()
                        )
                    },
                    bottomSlot = { text("Bottom Slot that overflows".layoutString) },
                    titleSlot = { text("TitleCard".layoutString, color = colorScheme.secondaryDim) }
                )
            }
        testCases["primarylayout_bottomslot_withlabel_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        coloredBox(color = colorScheme.errorContainer, shape = shapes.extraLarge)
                    },
                    bottomSlot = { text("Bottom Slot".layoutString) },
                    labelForBottomSlot = { text("Label in bottom slot overflows".layoutString) },
                    titleSlot = {
                        text("Title".layoutString, color = colorScheme.secondaryContainer)
                    }
                )
            }
        testCases["primarylayout_nobottomslot_golden$goldenSuffix"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        coloredBox(color = colorScheme.tertiaryContainer, shape = shapes.extraLarge)
                    },
                    labelForBottomSlot = { text("Ignored Label in bottom slot".layoutString) },
                    titleSlot = {
                        text("Title".layoutString, color = colorScheme.secondaryContainer)
                    }
                )
            }
        testCases["primarylayout_nobottomslotnotitle_golden$NORMAL_SCALE_SUFFIX"] =
            materialScope(
                ApplicationProvider.getApplicationContext(),
                deviceParameters,
                allowDynamicTheme = false
            ) {
                primaryLayout(
                    mainSlot = {
                        coloredBox(color = colorScheme.tertiaryDim, shape = shapes.extraLarge)
                    },
                )
            }

        return collectTestCases(testCases)
    }

    private fun coloredBox(color: LayoutColor, shape: Corner) =
        Box.Builder()
            .setWidth(expand())
            .setHeight(expand())
            .setModifiers(
                Modifiers.Builder()
                    .setBackground(
                        Background.Builder().setColor(color.prop).setCorner(shape).build()
                    )
                    .build()
            )
            .build()

    private fun collectTestCases(
        testCases: Map<String, LayoutElementBuilders.LayoutElement>
    ): ImmutableMap<String, LayoutElementBuilders.Layout> {
        return testCases.entries
            .stream()
            .collect(
                ImmutableMap.toImmutableMap(
                    { obj: Map.Entry<String, LayoutElementBuilders.LayoutElement> -> obj.key },
                    { entry: Map.Entry<String, LayoutElementBuilders.LayoutElement> ->
                        LayoutElementBuilders.Layout.fromLayoutElement(entry.value)
                    }
                )
            )
    }
}
