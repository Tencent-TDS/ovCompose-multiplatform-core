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

package androidx.compose.ui.graphics

import androidx.compose.ui.geometry.Offset

class NativePathMeasure : PathMeasure {
    private var path: Path? = null

    override var pathMeasureType: PathMeasureType = PathMeasureType.Native

    override val length: Float
        get() = 0f

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ): Boolean {
        if (destination != path) {
            destination.reset()
            destination.addPath(path ?: Path())
        }
        return stopDistance > startDistance
    }

    override fun setPath(path: Path?, forceClosed: Boolean) {
        // TODO: 只进行了赋值，未做任何处理
        this.path = path
    }

    // TODO: 先返回默认值 这里暂时无法对齐 SkiaPathMeasure
    override fun getPosition(distance: Float): Offset = Offset.Zero

    // TODO: 先返回默认值 这里暂时无法对齐 SkiaPathMeasure
    override fun getTangent(distance: Float): Offset = Offset.Zero
}