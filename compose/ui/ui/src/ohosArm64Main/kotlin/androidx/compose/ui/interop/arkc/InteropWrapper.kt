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

package androidx.compose.ui.interop.arkc

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntSize
import platform.arkui.ArkUI_GestureDirectionMask
import platform.arkui.GESTURE_DIRECTION_HORIZONTAL
import platform.arkui.GESTURE_DIRECTION_VERTICAL
import platform.arkui.NORMAL
import platform.arkui.NORMAL_GESTURE_MASK
import platform.arkui.PARALLEL

internal class InteropWrapper : ArkUINativeView() {
    var onRequestDisallowInterceptTouchEvent: Function1<Boolean, Unit>? = null
    var onRequestReMeasure: Function0<Unit>? = null

    private var lastConstraints: Constraints? = null

    private val horizontalPanGestureDetector = PanGestureDetector(GESTURE_DIRECTION_HORIZONTAL) {
        onRequestDisallowInterceptTouchEvent?.invoke(it)
    }

    private val verticalPanGestureDetector = PanGestureDetector(GESTURE_DIRECTION_VERTICAL) {
        onRequestDisallowInterceptTouchEvent?.invoke(it)
    }

    internal var interopView: ArkUINativeView? = null

    init {
        enabled = false
        horizontalPanGestureDetector.applyTo(this)
        verticalPanGestureDetector.applyTo(this)

        onMeasure = onMeasure@{
            val lastMeasuredSize = measuredSize
            val constraints = lastConstraints ?: return@onMeasure
            val interopView = interopView ?: return@onMeasure
            interopView.measureNode(constraints)
            val result = interopView.measuredSize
            measuredSize = IntSize(result.width, result.height)
            if (result != lastMeasuredSize) {
                onRequestReMeasure?.invoke()
            }
        }
    }

    fun measure(constraints: Constraints) {
        lastConstraints = constraints
        measureNode(constraints)
    }

    override fun dispose() {
        super.dispose()
        horizontalPanGestureDetector.dispose()
        verticalPanGestureDetector.dispose()
    }
}

private class PanGestureDetector(
    directions: ArkUI_GestureDirectionMask,
    requestDisallowInterceptTouchEvent: (Boolean) -> Unit
) {
    private var accepted = false
    private val panGesture = PanGesture(directions = directions).apply {
        onAccept = {
            accepted = true
        }
    }

    private val parallelPanGesture = PanGesture(directions = directions).apply {
        onAccept = {
            if (!accepted) requestDisallowInterceptTouchEvent(true)
            accepted = false
        }
    }

    fun applyTo(view: ArkUINativeView) {
        view.addGesture(panGesture.recognizer, NORMAL, NORMAL_GESTURE_MASK)
        view.addGesture(parallelPanGesture.recognizer, PARALLEL, NORMAL_GESTURE_MASK)
    }

    fun dispose() {
        panGesture.dispose()
        parallelPanGesture.dispose()
    }
}