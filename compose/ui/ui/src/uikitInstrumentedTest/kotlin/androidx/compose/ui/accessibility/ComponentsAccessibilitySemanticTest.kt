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

package androidx.compose.ui.accessibility

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.Checkbox
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.RadioButton
import androidx.compose.material.RangeSlider
import androidx.compose.material.Scaffold
import androidx.compose.material.Slider
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.TriStateCheckbox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.test.assertAccessibilityTree
import androidx.compose.ui.test.findNode
import androidx.compose.ui.test.firstAccessibleNode
import androidx.compose.ui.test.runUIKitInstrumentedTest
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitStaticText
import platform.UIKit.UIAccessibilityTraitToggleButton
import platform.UIKit.UIView
import platform.UIKit.accessibilityActivate
import platform.UIKit.accessibilityDecrement
import platform.UIKit.accessibilityIncrement
import platform.UIKit.setAccessibilityLabel
import platform.UIKit.setIsAccessibilityElement

class ComponentsAccessibilitySemanticTest {

    @Test
    fun testButtonNodeActionAndSemantic() = runUIKitInstrumentedTest {
        var tapped = false
        setContent {
            Button({ tapped = true }) {
                Text("Content")
            }
            Button({ }) {}
        }

        assertAccessibilityTree {
            node {
                node {
                    isAccessibilityElement = true
                    label = "Content"
                    traits(UIAccessibilityTraitButton)
                }
                node {
                    isAccessibilityElement = false
                    label = "Content"
                }
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }

        val node = firstAccessibleNode()
        node.element?.accessibilityActivate()
        assertTrue(tapped)
    }

    @OptIn(ExperimentalMaterialApi::class)
    @Test
    fun testProgressNodesSemantic() = runUIKitInstrumentedTest {
        var sliderValue = 0.4f
        setContent {
            Column {
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it }
                )
                LinearProgressIndicator(progress = 0.7f)
                RangeSlider(
                    value = 30f..70f,
                    onValueChange = {},
                    valueRange = 0f..100f
                )
            }
        }

        assertAccessibilityTree {
            // Slider
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "40%"
            }

            // LinearProgressIndicator
            node {
                isAccessibilityElement = true
                value = "70%"
                traits()
            }

            // Range Slider
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "43%"
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitAdjustable)
                value = "57%"
            }
        }
    }

    @Test
    fun testSliderAction() = runUIKitInstrumentedTest {
        var sliderValue = 0.4f
        setContent {
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                modifier = Modifier.testTag("Slider")
            )
        }

        var oldValue = sliderValue
        val sliderNode = findNode("Slider")
        sliderNode.element?.accessibilityIncrement()
        assertTrue(oldValue < sliderValue)

        oldValue = sliderValue
        sliderNode.element?.accessibilityDecrement()
        assertTrue(oldValue > sliderValue)
    }

    @Test
    fun testToggleAndCheckboxSemantic() = runUIKitInstrumentedTest {
        setContent {
            Column {
                Switch(false, {})
                Checkbox(false, {})
                TriStateCheckbox(ToggleableState.On, {})
                TriStateCheckbox(ToggleableState.Off, {})
                TriStateCheckbox(ToggleableState.Indeterminate, {})
            }
        }

        assertAccessibilityTree {
            // Switch
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
                if (available(OS.Ios to OSVersion(major = 17))) {
                    traits(UIAccessibilityTraitToggleButton)
                }
            }
            // Checkbox
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            // ToggleableState
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testToggleAndCheckboxAction() = runUIKitInstrumentedTest {
        var switch by mutableStateOf(false)
        var checkbox by mutableStateOf(false)
        var triStateCheckbox by mutableStateOf(ToggleableState.Off)

        setContent {
            Column {
                Switch(
                    checked = switch,
                    onCheckedChange = { switch = it },
                    modifier = Modifier.testTag("Switch")
                )
                Checkbox(
                    checked = checkbox,
                    onCheckedChange = { checkbox = it },
                    modifier = Modifier.testTag("Checkbox")
                )
                TriStateCheckbox(
                    state = triStateCheckbox,
                    onClick = { triStateCheckbox = ToggleableState.On },
                    modifier = Modifier.testTag("TriStateCheckbox")
                )
            }
        }

        findNode("Switch").element?.accessibilityActivate()
        assertTrue(switch)
        waitForIdle()
        findNode("Switch").element?.accessibilityActivate()
        assertFalse(switch)

        findNode("Checkbox").element?.accessibilityActivate()
        assertTrue(checkbox)
        waitForIdle()
        findNode("Checkbox").element?.accessibilityActivate()
        assertFalse(checkbox)

        findNode("TriStateCheckbox").element?.accessibilityActivate()
        assertEquals(ToggleableState.On, triStateCheckbox)
    }

    @Test
    fun testRadioButtonSelection() = runUIKitInstrumentedTest {
        var selectedIndex by mutableStateOf(0)

        setContent {
            Column {
                RadioButton(selected = selectedIndex == 0, onClick = { selectedIndex = 0 })
                RadioButton(selected = selectedIndex == 1, onClick = { selectedIndex = 1 })
                RadioButton(
                    selected = selectedIndex == 2,
                    onClick = { selectedIndex = 2 },
                    Modifier.testTag("RadioButton")
                )
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }

        findNode("RadioButton").element?.accessibilityActivate()
        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
        }

        selectedIndex = 0
        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitSelected
                )
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testImageSemantics() = runUIKitInstrumentedTest {
        setContent {
            Column {
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = null,
                    modifier = Modifier.testTag("Image 1")
                )
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = null,
                    modifier = Modifier.testTag("Image 2").semantics { role = Role.Image }
                )
                Image(
                    ImageBitmap(10, 10),
                    contentDescription = "Abstract Picture",
                    modifier = Modifier.testTag("Image 3")
                )
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = false
                identifier = "Image 1"
                traits()
            }
            node {
                isAccessibilityElement = false
                identifier = "Image 2"
                traits(UIAccessibilityTraitImage)
            }
            node {
                isAccessibilityElement = true
                identifier = "Image 3"
                label = "Abstract Picture"
                traits(UIAccessibilityTraitImage)
            }
        }
    }

    @Test
    fun testTextSemantics() = runUIKitInstrumentedTest {
        setContent {
            Column {
                Text("Static Text", modifier = Modifier.testTag("Text 1"))
                Text("Custom Button", modifier = Modifier.testTag("Text 2").clickable { })
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                identifier = "Text 1"
                label = "Static Text"
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                isAccessibilityElement = true
                identifier = "Text 2"
                label = "Custom Button"
                traits(UIAccessibilityTraitButton)
            }
        }
    }

    @Test
    fun testDisabledSemantics() = runUIKitInstrumentedTest {
        setContent {
            Column {
                Button({}, enabled = false) {}
                TextField("", {}, enabled = false)
                Slider(value = 0f, onValueChange = {}, enabled = false)
                Switch(checked = false, onCheckedChange = {}, enabled = false)
                Checkbox(checked = false, onCheckedChange = {}, enabled = false)
                TriStateCheckbox(state = ToggleableState.Off, onClick = {}, enabled = false)
            }
        }

        assertAccessibilityTree {
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitAdjustable,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                if (available(OS.Ios to OSVersion(major = 17))) {
                    traits(
                        UIAccessibilityTraitButton,
                        UIAccessibilityTraitToggleButton,
                        UIAccessibilityTraitNotEnabled
                    )
                } else {
                    traits(
                        UIAccessibilityTraitButton,
                        UIAccessibilityTraitNotEnabled
                    )
                }
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
            node {
                isAccessibilityElement = true
                traits(
                    UIAccessibilityTraitButton,
                    UIAccessibilityTraitNotEnabled
                )
            }
        }
    }

    @Test
    fun testHeadingSemantics() = runUIKitInstrumentedTest {
        setContent {
            Scaffold(topBar = {
                TopAppBar {
                    Text("Header", modifier = Modifier.semantics { heading() })
                }
            }) {
                Column {
                    Text("Content")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Header"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitHeader)
            }
            node {
                label = "Content"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }

    @Test
    fun testSelectionContainer() = runUIKitInstrumentedTest {
        @Composable
        fun LabeledInfo(label: String, data: String) {
            Text(
                buildAnnotatedString {
                    append("$label: ")
                    append(data)
                }
            )
        }

        setContent {
            SelectionContainer {
                Column {
                    Text("Title")
                    LabeledInfo("Subtitle", "subtitle")
                    LabeledInfo("Details", "details")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Title"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Subtitle: subtitle"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Details: details"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }

    @Test
    fun testVisibleNodes() = runUIKitInstrumentedTest {
        var alpha by mutableStateOf(0f)

        setContent {
            Text("Hidden", modifier = Modifier.graphicsLayer {
                this.alpha = alpha
            })
        }

        assertAccessibilityTree {
            label = "Hidden"
            isAccessibilityElement = false
        }

        alpha = 1f
        assertAccessibilityTree {
            label = "Hidden"
            isAccessibilityElement = true
        }
    }

    @Test
    fun testVisibleNodeContainers() = runUIKitInstrumentedTest {
        var alpha by mutableStateOf(0f)

        setContent {
            Column {
                Text("Text 1")
                Row(modifier = Modifier.graphicsLayer {
                    this.alpha = alpha
                }) {
                    Text("Text 2")
                    Text("Text 3")
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = false
            }
            node {
                label = "Text 3"
                isAccessibilityElement = false
            }
        }

        alpha = 1f
        assertAccessibilityTree {
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = true
            }
            node {
                label = "Text 3"
                isAccessibilityElement = true
            }
        }
    }

    @Test
    fun testAccessibilityContainer() = runUIKitInstrumentedTest {
        setContent {
            Column(modifier = Modifier.testTag("Container")) {
                Text("Text 1")
                Text("Text 2")
            }
        }

        assertAccessibilityTree {
            identifier = "Container"
            isAccessibilityElement = false
            node {
                label = "Text 1"
                isAccessibilityElement = true
            }
            node {
                label = "Text 2"
                isAccessibilityElement = true
            }
        }
    }

    @Test
    fun testAccessibilityInterop() = runUIKitInstrumentedTest {
        setContent {
            Column(modifier = Modifier.testTag("Container")) {
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Disabled")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = false)
                )
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Enabled")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true)
                )
                UIKitView(
                    factory = {
                        val view = UIView()
                        view.setIsAccessibilityElement(true)
                        view.setAccessibilityLabel("Enabled With Tag")
                        view
                    },
                    properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
                    modifier = Modifier.testTag("Container Tag")
                )
            }
        }

        assertAccessibilityTree {
            identifier = "Container"
            isAccessibilityElement = false
            node {
                label = "Enabled"
                isAccessibilityElement = true
            }
            node {
                identifier = "Container Tag"
                isAccessibilityElement = false
                node {
                    label = "Enabled With Tag"
                    isAccessibilityElement = true
                }
            }
        }
    }

    @Test
    fun testChildrenOfCollapsedNode() = runUIKitInstrumentedTest {
        setContent {
            Column {
                Row(modifier = Modifier.testTag("row").clickable {}) {
                    Text("Foo", modifier = Modifier.testTag("row_title"))
                    Text("Bar", modifier = Modifier.testTag("row_subtitle"))
                }
            }
        }

        assertAccessibilityTree {
            node {
                label = "Foo\nBar"
                identifier = "row"
                isAccessibilityElement = true
                traits(UIAccessibilityTraitButton)
            }
            node {
                label = "Foo"
                identifier = "row_title"
                isAccessibilityElement = false
                traits(UIAccessibilityTraitStaticText)
            }
            node {
                label = "Bar"
                identifier = "row_subtitle"
                isAccessibilityElement = false
                traits(UIAccessibilityTraitStaticText)
            }
        }
    }
}
