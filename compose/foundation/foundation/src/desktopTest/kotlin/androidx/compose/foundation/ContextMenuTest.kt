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

package androidx.compose.foundation

import androidx.compose.foundation.copyPasteAndroidTests.lazy.list.assertIsNotPlaced
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalLocalization
import androidx.compose.ui.platform.PlatformLocalization
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTextInputSelection
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.rightClick
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import kotlin.test.assertEquals
import org.junit.Test


@OptIn(ExperimentalTestApi::class)
class ContextMenuTest {

    // https://github.com/JetBrains/compose-multiplatform/issues/2729
    @Test
    fun `contextMenuArea emits one child when open`() = runContextMenuTest {
        var childrenCount = 0

        setContent {
            // We can't just look up the number of children via the semantic node tree because
            // the layout added for the context menu (the empty one in PopupLayout) is not a
            // semantic node
            Layout(
                content = {
                    val state = ContextMenuState()
                    state.status = ContextMenuState.Status.Open(
                        Rect(Offset(1f, 1f), 0f)
                    )

                    ContextMenuArea(
                        items = {
                            listOf(
                                ContextMenuItem(
                                    label = "Copy",
                                    onClick = {}
                                )
                            )
                        },
                        state = state
                    ) {
                        Box(content = {})
                    }
                },
                measurePolicy = { measurables, _ ->
                    childrenCount = measurables.size
                    layout(0, 0) {}
                }
            )
        }

        assertEquals(1, childrenCount)
    }

    // https://youtrack.jetbrains.com/issue/CMP-7083/Context-menu-on-desktop-shows-incorrect-items-after-the-second-showing
    @Test
    fun `different items for different selections in textfield`() = runContextMenuTest {
        val localization = object : PlatformLocalization {
            override val copy = "copy"
            override val cut = "cut"
            override val paste = "paste"
            override val selectAll = "selectAll"
        }
        val clipboardManager = object : ClipboardManager {
            override fun setText(annotatedString: AnnotatedString) = Unit
            override fun getText() = AnnotatedString("text")
        }
        setContent {
            CompositionLocalProvider(
                LocalLocalization provides localization,
                LocalClipboardManager provides clipboardManager,
            ) {
                BasicTextField("Text", {}, Modifier.testTag("textfield"))
            }
        }

        onNodeWithText(localization.copy).assertIsNotPlaced()
        onNodeWithText(localization.cut).assertIsNotPlaced()
        onNodeWithText(localization.paste).assertIsNotPlaced()
        onNodeWithText(localization.selectAll).assertIsNotPlaced()

        onNodeWithTag("textfield").performMouseInput { rightClick() }
        onNodeWithText(localization.copy).assertIsNotPlaced()
        onNodeWithText(localization.cut).assertIsNotPlaced()
        onNodeWithText(localization.paste).isDisplayed()
        onNodeWithText(localization.selectAll).isDisplayed()

        onNodeWithTag("textfield").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText(localization.copy).assertIsNotPlaced()
        onNodeWithText(localization.cut).assertIsNotPlaced()
        onNodeWithText(localization.paste).assertIsNotPlaced()
        onNodeWithText(localization.selectAll).assertIsNotPlaced()

        onNodeWithTag("textfield").performTextInputSelection(TextRange(0, "Text".length))
        onNodeWithTag("textfield").performMouseInput { rightClick() }
        onNodeWithText(localization.copy).isDisplayed()
        onNodeWithText(localization.cut).isDisplayed()
        onNodeWithText(localization.paste).isDisplayed()
        onNodeWithText(localization.selectAll).assertIsNotPlaced()

        onNodeWithTag("textfield").performKeyInput { pressKey(Key.Escape) }
        onNodeWithText(localization.copy).assertIsNotPlaced()
        onNodeWithText(localization.cut).assertIsNotPlaced()
        onNodeWithText(localization.paste).assertIsNotPlaced()
        onNodeWithText(localization.selectAll).assertIsNotPlaced()
    }

    private fun runContextMenuTest(block: ComposeUiTest.() -> Unit) = runComposeUiTest {
        DesktopPlatform.withOverriddenCurrent(DesktopPlatform.Unknown) {
            block()
        }
    }
}