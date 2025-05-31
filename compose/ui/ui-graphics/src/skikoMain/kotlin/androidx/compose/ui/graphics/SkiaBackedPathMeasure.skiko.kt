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

package androidx.compose.ui.graphics

import androidx.compose.runtime.CurrentPlatform
import androidx.compose.runtime.PlatformType
import androidx.compose.ui.geometry.Offset
/**
 * Convert the [org.jetbrains.skia.PathMeasure] instance into a Compose-compatible PathMeasure
 */
fun org.jetbrains.skia.PathMeasure.asComposePathEffect(): PathMeasure = SkiaBackedPathMeasure(this)

/**
 * Obtain a reference to skia PathMeasure type
 */
fun PathMeasure.asSkiaPathMeasure(): org.jetbrains.skia.PathMeasure {
    // region Tencent Code
    val pathMeasure = if(this is PathMeasureProxy) this.skiaBackedPathMeasure else this
    return (pathMeasure as SkiaBackedPathMeasure).skia
    // endregion Tencent Code
}

class SkiaBackedPathMeasure(
    internal val skia: org.jetbrains.skia.PathMeasure = org.jetbrains.skia.PathMeasure()
) : PathMeasure {

    // region Tencent Code
    override var pathMeasureType: PathMeasureType = PathMeasureType.Skia
    // endregion
    override fun setPath(path: Path?, forceClosed: Boolean) {
        val realPath = if (path is PathProxy) path.skiaBackedPath else path
        skia.setPath(realPath?.asSkiaPath(), forceClosed)
    }

    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ) = skia.getSegment(
        startDistance,
        stopDistance,
        destination.asSkiaPath(),
        startWithMoveTo
    )

    override val length: Float
        get() = skia.length

    override fun getPosition(
        distance: Float
    ): Offset {
        val result = skia.getPosition(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }

    override fun getTangent(
        distance: Float
    ): Offset {
        val result = skia.getTangent(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }
}
// region Tencent Code
private var pathMeasureFactory: (() -> PathMeasure)? = null
fun setPathMeasureFactory(factory: () -> PathMeasure) {
    pathMeasureFactory = factory
}

actual fun PathMeasure(): PathMeasure =
    if (CurrentPlatform != PlatformType.IOS) SkiaBackedPathMeasure() else PathMeasureProxy()

class PathMeasureProxy : PathMeasure {
    val skiaBackedPathMeasure = SkiaBackedPathMeasure()
    val nativePathMeasure = pathMeasureFactory?.invoke() ?: throw RuntimeException("Native 未注入实现")
    val currentPathMeasure: PathMeasure
        get() = if (pathMeasureType == PathMeasureType.Native) {
            nativePathMeasure
        } else {
            skiaBackedPathMeasure
        }

    //默认为Skia
    override var pathMeasureType: PathMeasureType = PathMeasureType.Skia
    override fun setPath(path: Path?, forceClosed: Boolean) {
        skiaBackedPathMeasure.setPath(path, forceClosed)
        nativePathMeasure.setPath(path, forceClosed)
    }

    // 此方法涉及到addPath, 所以需要native和skia都要调用
    override fun getSegment(
        startDistance: Float,
        stopDistance: Float,
        destination: Path,
        startWithMoveTo: Boolean
    ): Boolean {
        nativePathMeasure.getSegment(startDistance, stopDistance, destination, startWithMoveTo)
        val result = skiaBackedPathMeasure.getSegment(startDistance, stopDistance, destination, startWithMoveTo)
        return result
    }

    override val length: Float
        get() = currentPathMeasure.length

    override fun getPosition(
        distance: Float
    ): Offset {
        val result = currentPathMeasure.getPosition(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }

    override fun getTangent(
        distance: Float
    ): Offset {
        val result = currentPathMeasure.getTangent(distance)
        return if (result != null) {
            Offset(result.x, result.y)
        } else {
            Offset.Unspecified
        }
    }
}

// endregion