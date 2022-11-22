/*
 * Copyright 2020 The Android Open Source Project
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
@file:OptIn(ExperimentalTestApi::class)

package androidx.compose.ui.test

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.platform.SkiaRootForTest

@OptIn(InternalComposeUiApi::class)
internal actual fun createInputDispatcher(
    testContext: TestContext,
    root: RootForTest
): InputDispatcher {
    return UikitInputDispatcher(testContext, root as SkiaRootForTest)
}

@OptIn(InternalComposeUiApi::class)
internal class UikitInputDispatcher(
    testContext: TestContext,
    private val root: SkiaRootForTest
) : InputDispatcher(
    testContext,
    root,
    exitHoverOnPress = false,
    moveOnScroll = false,
) {
    override fun RotaryInputState.enqueueRotaryScrollHorizontally(horizontalScrollPixels: Float) {
        TODO("Not yet implemented")
    }

    override fun RotaryInputState.enqueueRotaryScrollVertically(verticalScrollPixels: Float) {
        TODO("Not yet implemented")
    }

    override fun flush() {
        TODO("Not yet implemented")
    }

    override fun PartialGesture.enqueueDown(pointerId: Int) {
        TODO("Not yet implemented")
    }

    override fun KeyInputState.enqueueDown(key: Key) {
        TODO("Not yet implemented")
    }

    override fun PartialGesture.enqueueMove() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueMove() {
        TODO("Not yet implemented")
    }

    override fun PartialGesture.enqueueMoves(
        relativeHistoricalTimes: List<Long>,
        historicalCoordinates: List<List<Offset>>
    ) {
        TODO("Not yet implemented")
    }

    override fun PartialGesture.enqueueUp(pointerId: Int) {
        TODO("Not yet implemented")
    }

    override fun KeyInputState.enqueueUp(key: Key) {
        TODO("Not yet implemented")
    }

    override fun PartialGesture.enqueueCancel() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueCancel() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueuePress(buttonId: Int) {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueRelease(buttonId: Int) {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueEnter() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueExit() {
        TODO("Not yet implemented")
    }

    override fun MouseInputState.enqueueScroll(delta: Float, scrollWheel: ScrollWheel) {
        TODO("Not yet implemented")
    }

}

/**
 * The [KeyEvent] is usually created by the system. This function creates an instance of
 * [KeyEvent] that can be used in tests.
 */
internal actual fun keyEvent(
    key: Key, keyEventType: KeyEventType, modifiers: Int
): KeyEvent = TODO()
