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

package androidx.wear.protolayout.material3.samples

import android.content.Context
import androidx.annotation.Sampled
import androidx.wear.protolayout.DeviceParametersBuilders.DeviceParameters
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.LayoutElement
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.TypeBuilders.StringLayoutConstraint
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.expression.DynamicBuilders.DynamicString
import androidx.wear.protolayout.material3.CardDefaults.filledVariantCardColors
import androidx.wear.protolayout.material3.TitleCardStyle.Companion.largeTitleCardStyle
import androidx.wear.protolayout.material3.Typography
import androidx.wear.protolayout.material3.backgroundImage
import androidx.wear.protolayout.material3.buttonGroup
import androidx.wear.protolayout.material3.card
import androidx.wear.protolayout.material3.icon
import androidx.wear.protolayout.material3.iconEdgeButton
import androidx.wear.protolayout.material3.materialScope
import androidx.wear.protolayout.material3.primaryLayout
import androidx.wear.protolayout.material3.prop
import androidx.wear.protolayout.material3.text
import androidx.wear.protolayout.material3.textEdgeButton
import androidx.wear.protolayout.material3.titleCard

/** Builds Material3 text element with default options. */
@Sampled
fun helloWorldTextDefault(context: Context, deviceConfiguration: DeviceParameters): LayoutElement =
    materialScope(context, deviceConfiguration) {
        text(text = "Hello Material3".prop(), typography = Typography.DISPLAY_LARGE)
    }

/** Builds Material3 text element with some of the overridden defaults. */
@Sampled
fun helloWorldTextDynamicCustom(
    context: Context,
    deviceConfiguration: DeviceParameters
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        text(
            text =
                StringProp.Builder("Static")
                    .setDynamicValue(DynamicString.constant("Dynamic"))
                    .build(),
            stringLayoutConstraint = StringLayoutConstraint.Builder("Constraint").build(),
            typography = Typography.DISPLAY_LARGE,
            color = colorScheme.tertiary,
            underline = true,
            maxLines = 5
        )
    }

@Sampled
fun edgeButtonSampleIcon(
    context: Context,
    deviceConfiguration: DeviceParameters,
    clickable: Clickable
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        iconEdgeButton(onClick = clickable, contentDescription = "Description of a button".prop()) {
            icon(protoLayoutResourceId = "id")
        }
    }

@Sampled
fun edgeButtonSampleText(
    context: Context,
    deviceConfiguration: DeviceParameters,
    clickable: Clickable
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        textEdgeButton(onClick = clickable, contentDescription = "Description of a button".prop()) {
            text("Hello".prop())
        }
    }

@Sampled
fun topLeveLayout(
    context: Context,
    deviceConfiguration: DeviceParameters,
    clickable: Clickable
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        primaryLayout(
            titleSlot = { text("App title".prop()) },
            mainSlot = {
                buttonGroup {
                    // To be populated with proper components
                    buttonGroupItem {
                        LayoutElementBuilders.Box.Builder()
                            .setModifiers(
                                ModifiersBuilders.Modifiers.Builder()
                                    .setBackground(
                                        ModifiersBuilders.Background.Builder()
                                            .setCorner(shapes.full)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    }
                }
            },
            bottomSlot = { iconEdgeButton(clickable, "Description".prop()) { icon("id") } }
        )
    }

@Sampled
fun cardSample(
    context: Context,
    deviceConfiguration: DeviceParameters,
    clickable: Clickable
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        primaryLayout(
            mainSlot = {
                card(
                    onClick = clickable,
                    contentDescription = "Card with image background".prop(),
                    width = expand(),
                    height = expand(),
                    background = { backgroundImage(protoLayoutResourceId = "id") }
                ) {
                    text("Content of the Card!".prop())
                }
            }
        )
    }

@Sampled
fun titleCardSample(
    context: Context,
    deviceConfiguration: DeviceParameters,
    clickable: Clickable
): LayoutElement =
    materialScope(context, deviceConfiguration) {
        primaryLayout(
            mainSlot = {
                titleCard(
                    onClick = clickable,
                    contentDescription = "Card with image background".prop(),
                    height = expand(),
                    colors = filledVariantCardColors(),
                    style = largeTitleCardStyle(),
                    title = { text("This is title of the title card".prop()) },
                    time = { text("NOW".prop()) },
                    content = { text("Content of the Card!".prop()) }
                )
            }
        )
    }
