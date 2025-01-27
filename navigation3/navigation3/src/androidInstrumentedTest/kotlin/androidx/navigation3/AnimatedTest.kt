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

package androidx.navigation3

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.isDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.kruth.assertThat
import androidx.navigation3.NavDisplay.DEFAULT_TRANSITION_DURATION_MILLISECOND
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class AnimatedTest {
    @get:Rule val composeTestRule = createComposeRule()

    @Test
    fun testNavHostAnimations() {
        lateinit var backstack: MutableList<Any>

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack) {
                when (it) {
                    first -> NavEntry(first) { Text(first) }
                    second -> NavEntry(second) { Text(second) }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        assertThat(composeTestRule.onNodeWithText(first).isDisplayed()).isTrue()

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backstack.add(second) }

        // advance half way between animations
        composeTestRule.mainClock.advanceTimeBy(
            DEFAULT_TRANSITION_DURATION_MILLISECOND.toLong() / 2
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertExists()
    }

    @Test
    fun testNavHostAnimationsCustom() {
        lateinit var backstack: MutableList<Any>

        composeTestRule.mainClock.autoAdvance = false
        val customDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND * 2

        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(customDuration)),
                                    exit = fadeOut(tween(customDuration))
                                )
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertIsDisplayed()

        composeTestRule.mainClock.autoAdvance = false

        composeTestRule.runOnIdle { backstack.add(second) }

        // advance past the default duration but not the custom duration
        composeTestRule.mainClock.advanceTimeBy(
            ((DEFAULT_TRANSITION_DURATION_MILLISECOND * 3 / 2)).toLong()
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertExists()
        composeTestRule.onNodeWithText(second).assertExists()

        composeTestRule.mainClock.autoAdvance = true

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertExists()
    }

    @Test
    fun testPop() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                            featureMap =
                                NavDisplay.popTransition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backstack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
    }

    @Test
    fun testPopMultiple() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                            featureMap =
                                NavDisplay.popTransition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(2)
            backstack.removeAt(1)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        // pop to first
        assertThat(backstack).containsExactly(first)
        composeTestRule.onNodeWithText(first).assertIsDisplayed()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
    }

    @Test
    fun testPopNavigate() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(1)
            backstack.add(third)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentrePop() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.removeAt(1) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentreNavigate() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.add(1, second) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, second, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
    }

    @Test
    fun testCentrePopAndEndPop() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second, third, fourth) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                            NavDisplay.transition(
                                enter = fadeIn(tween(testDuration)),
                                exit = fadeOut(tween(testDuration))
                            )
                        ) {
                            Text(third)
                        }
                    fourth ->
                        NavEntry(
                            fourth,
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second, third, fourth)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(3)
            backstack.removeAt(1)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        composeTestRule.onNodeWithText(fourth).assertDoesNotExist()
    }

    @Test
    fun testCentrePopAndEndNavigate() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                        ) {
                            Text(third)
                        }
                    fourth ->
                        NavEntry(
                            fourth,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(1)
            backstack.add(fourth)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())

        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, third, fourth)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
    }

    @Test
    fun testCentreNavigateAndEndPop() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, third, fourth) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(third)
                        }
                    fourth ->
                        NavEntry(
                            fourth,
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, third, fourth)
        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(2)
            backstack.add(1, second)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, second, third)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        composeTestRule.onNodeWithText(fourth).assertDoesNotExist()
    }

    @Test
    fun testCentreNavigateAndEndNavigate() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                        ) {
                            Text(third)
                        }
                    fourth ->
                        NavEntry(
                            fourth,
                            NavDisplay.transition(
                                enter = fadeIn(tween(testDuration)),
                                exit = fadeOut(tween(testDuration))
                            )
                        ) {
                            Text(fourth)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.add(1, second)
            backstack.add(fourth)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, second, third, fourth)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(fourth).assertIsDisplayed()
    }

    @Test
    fun testSameStack() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                            NavDisplay.transition(
                                enter = fadeIn(tween(testDuration)),
                                exit = fadeOut(tween(testDuration))
                            )
                        ) {
                            Text(second)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle {
            backstack.removeAt(1)
            backstack.add(second)
        }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, second)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }

    @Test
    fun testDuplicateLastEntry() {
        lateinit var backstack: MutableList<Any>
        val testDuration = DEFAULT_TRANSITION_DURATION_MILLISECOND / 5
        composeTestRule.setContent {
            backstack = remember { mutableStateListOf(first, second, third) }
            NavDisplay(backstack) {
                when (it) {
                    first ->
                        NavEntry(
                            first,
                        ) {
                            Text(first)
                        }
                    second ->
                        NavEntry(
                            second,
                            featureMap =
                                NavDisplay.transition(
                                    enter = fadeIn(tween(testDuration)),
                                    exit = fadeOut(tween(testDuration))
                                )
                        ) {
                            Text(second)
                        }
                    third ->
                        NavEntry(
                            third,
                        ) {
                            Text(third)
                        }
                    else -> error("Invalid key passed")
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertIsDisplayed()
        assertThat(backstack).containsExactly(first, second, third)

        composeTestRule.mainClock.autoAdvance = false
        composeTestRule.runOnIdle { backstack.add(second) }

        // advance by a duration that is much shorter than the default duration
        // to ensure that the custom animation is used and has completed after this
        composeTestRule.mainClock.advanceTimeBy((testDuration * 1.5).toLong())
        // not pop
        composeTestRule.waitForIdle()
        assertThat(backstack).containsExactly(first, second, third, second)
        composeTestRule.onNodeWithText(first).assertDoesNotExist()
        composeTestRule.onNodeWithText(third).assertDoesNotExist()
        composeTestRule.onNodeWithText(second).assertIsDisplayed()
    }
}

private const val first = "first"
private const val second = "second"
private const val third = "third"
private const val fourth = "fourth"
