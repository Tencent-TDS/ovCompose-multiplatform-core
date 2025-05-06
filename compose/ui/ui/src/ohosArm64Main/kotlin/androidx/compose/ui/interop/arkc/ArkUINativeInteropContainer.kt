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

import androidx.compose.ui.Modifier
import androidx.compose.ui.node.InteropContainer
import androidx.compose.ui.node.TrackInteropModifierElement
import androidx.compose.ui.node.TrackInteropModifierNode
import androidx.compose.ui.node.countInteropComponentsBefore

class ArkUINativeInteropContainer : ArkUINativeView(), InteropContainer<ArkUINativeView> {

    override var rootModifier: TrackInteropModifierNode<ArkUINativeView>? = null

    override val interopViews = mutableSetOf<ArkUINativeView>()

    override fun addInteropView(nativeView: ArkUINativeView) {
        val index = countInteropComponentsBefore(nativeView)
        interopViews.add(nativeView)
        insertChildAt(nativeView, index)
    }

    override fun removeInteropView(nativeView: ArkUINativeView) {
        removeChild(nativeView)
        interopViews.remove(nativeView)
    }
}

internal fun Modifier.trackArkUINativeInterop(view: ArkUINativeView): Modifier =
    this then TrackInteropModifierElement(nativeView = view)