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

package androidx.compose.foundation.lazy.list

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.filters.LargeTest
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@OptIn(ExperimentalFoundationApi::class)
@LargeTest
@RunWith(Parameterized::class)
class LazyListCacheWindowTest(orientation: Orientation) :
    BaseLazyListTestWithOrientation(orientation) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun initParameters(): Array<Any> = arrayOf(Orientation.Vertical, Orientation.Horizontal)
    }

    val itemsSizePx = 30
    val itemsSizeDp = with(rule.density) { itemsSizePx.toDp() }

    lateinit var state: LazyListState

    private val viewportWindow = LazyLayoutCacheWindow(aheadFraction = 1f)

    @Test
    fun notPrefetchingForwardInitially() {
        composeList(cacheWindow = viewportWindow)
        rule.onNodeWithTag("2").assertDoesNotExist()
    }

    @Test
    fun notPrefetchingBackwardInitially() {
        composeList(firstItem = 2, cacheWindow = viewportWindow)
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun smallScrollForward_shouldFillEntireWindow() {
        composeList(cacheWindow = viewportWindow)
        val preFetchIndex = 2
        rule.runOnIdle { runBlocking { state.scrollBy(5f) } }

        // the viewport fits 1.5 items, we start with indices 0 and 1 visible.
        // index 1 is halfway in the window, we will be able to fit 1 extra item.
        // Since we're moving 5pixels, we can prefetch another item to fill the window.
        rule.onNodeWithTag("$preFetchIndex").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 1}").assertExists()
        rule.onNodeWithTag("${preFetchIndex + 2}").assertDoesNotExist()
    }

    @Test
    fun smallScrollBackwardShouldFillEntireWindow() {
        composeList(firstItem = 3, itemOffset = 10, cacheWindow = viewportWindow)

        rule.runOnIdle { runBlocking { state.scrollBy(-5f) } }

        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("0").assertDoesNotExist()
    }

    @Test
    fun scrollForward_shouldNotDisposeItemsInWindow() {
        composeList(firstItem = 3, cacheWindow = viewportWindow)
        rule.runOnIdle { runBlocking { state.scrollBy(itemsSizePx * 2.5f) } }
        // Starting on item 3 and moving 2.5 items, we will end up on item 5.
        // This means item 6 will be visible, item 7 and 8 will be in the window.
        // On the other side, items 4 and 3 will be in the window.
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(5) }
        rule.onNodeWithTag("2").assertDoesNotExist()
        rule.onNodeWithTag("3").assertExists()
        rule.onNodeWithTag("4").assertExists()
        rule.onNodeWithTag("5").assertIsDisplayed()
        rule.onNodeWithTag("6").assertIsDisplayed()
        rule.onNodeWithTag("7").assertExists()
        rule.onNodeWithTag("8").assertExists()
        rule.onNodeWithTag("9").assertDoesNotExist()
    }

    @Test
    fun scrollBackward_shouldNotDisposeItemsInWindow() {
        composeList(firstItem = 6, itemOffset = -itemsSizePx / 2, cacheWindow = viewportWindow)
        rule.runOnIdle { runBlocking { state.scrollBy(-itemsSizePx * 2.5f) } }
        // Starting on item 6 and moving back 2.5 items, we will end up on item 3.
        // This means item 6 will be visible, item 7 and 8 will be in the window.
        // On the other side, items 4 and 3 will be in the window.
        rule.runOnIdle { assertThat(state.firstVisibleItemIndex).isEqualTo(3) }
        rule.onNodeWithTag("0").assertDoesNotExist()
        rule.onNodeWithTag("1").assertExists()
        rule.onNodeWithTag("2").assertExists()
        rule.onNodeWithTag("3").assertIsDisplayed()
        rule.onNodeWithTag("4").assertIsDisplayed()
        rule.onNodeWithTag("5").assertExists()
        rule.onNodeWithTag("6").assertExists()
        rule.onNodeWithTag("7").assertDoesNotExist()
    }

    private val activeNodes = mutableSetOf<Int>()

    private fun composeList(
        firstItem: Int = 0,
        itemOffset: Int = 0,
        reverseLayout: Boolean = false,
        contentPadding: PaddingValues = PaddingValues(0.dp),
        cacheWindow: LazyLayoutCacheWindow
    ) {
        rule.setContent {
            @OptIn(ExperimentalFoundationApi::class)
            state =
                rememberLazyListState(
                    initialFirstVisibleItemIndex = firstItem,
                    initialFirstVisibleItemScrollOffset = itemOffset,
                    cacheWindow = cacheWindow
                )
            LazyColumnOrRow(
                Modifier.mainAxisSize(itemsSizeDp * 1.5f),
                state,
                reverseLayout = reverseLayout,
                contentPadding = contentPadding,
            ) {
                items(100) {
                    DisposableEffect(it) {
                        activeNodes.add(it)
                        onDispose { activeNodes.remove(it) }
                    }
                    Spacer(
                        Modifier.mainAxisSize(itemsSizeDp)
                            .fillMaxCrossAxis()
                            .testTag("$it")
                            .layout { measurable, constraints ->
                                val placeable = measurable.measure(constraints)
                                layout(placeable.width, placeable.height) { placeable.place(0, 0) }
                            }
                    )
                }
            }
        }
    }
}
