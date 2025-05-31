/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.platform.textinput

import androidx.compose.runtime.MutableState
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

internal interface KeyboardVisibilityListener {
    fun keyboardWillShow(keyboardHeight: Float)
    fun keyboardWillHide()
}

internal class KeyboardVisibilityListenerImpl(
    private val density: () -> Density,
    private val keyboardOverlapHeightState: MutableState<Dp>,
    private val keyboardAvoidFocusOffsetState: MutableState<Dp>,
    private val sceneMediatorProvider: () -> ComposeSceneMediator,
    private val focusManagerProvider: () -> ComposeSceneFocusManager,
) : KeyboardVisibilityListener {

    override fun keyboardWillShow(keyboardHeight: Float) {
        log(TAG, "keyboardWillShow - keyboardHeight: $keyboardHeight")

        keyboardOverlapHeightState.value = with(density()) { keyboardHeight.toDp() }
        val mediator = sceneMediatorProvider()
        val focusedRect = focusManagerProvider().getFocusRect()

        if (focusedRect != null) {
            keyboardAvoidFocusOffsetState.value = with(density()) {
                calcFocusedLiftingY(mediator, focusedRect, keyboardHeight).toDp()
            }
        }
    }

    override fun keyboardWillHide() {
        log(TAG, "keyboardWillHide")

        keyboardOverlapHeightState.value = 0.dp
        keyboardAvoidFocusOffsetState.value = 0.dp
    }

    private fun calcFocusedLiftingY(
        composeSceneMediator: ComposeSceneMediator,
        focusedRect: Rect,
        keyboardHeight: Float
    ): Int {
        val viewHeight = composeSceneMediator.getViewHeight()
        val bottomBarHeight = 0
        val hiddenPartOfFocusedElement: Float = keyboardHeight - bottomBarHeight - viewHeight + focusedRect.bottom
        return if (hiddenPartOfFocusedElement > 0) {
            // If focused element is partially hidden by the keyboard, we need to lift it upper
            val focusedTopY = focusedRect.top
            val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
            if (isFocusedElementRemainsVisible) {
                // We need to lift focused element to be fully visible
                hiddenPartOfFocusedElement.toInt()
            } else {
                // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                // Top edge of focused element should be visible. Same logic on Android.
                maxOf(focusedTopY, 0f).toInt()
            }
        } else {
            // Focused element is not hidden by the keyboard.
            0
        }
    }

    companion object {
        const val TAG = "KeyboardVisibilityListener"
    }
}