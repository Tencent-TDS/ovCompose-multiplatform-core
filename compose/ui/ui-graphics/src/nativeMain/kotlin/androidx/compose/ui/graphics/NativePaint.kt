/*
 * Copyright 2021 The Android Open Source Project
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

import org.jetbrains.skiko.skia.native.*

actual typealias PlatformPaint = org.jetbrains.skiko.skia.native.Paint

actual fun Paint(): Paint = NativePaint()

class NativePaint : Paint {
    internal val skia = org.jetbrains.skiko.skia.native.Paint()

    constructor() {
        filterQuality = FilterQuality.Medium
    }

    override fun asFrameworkPaint(): PlatformPaint = skia

    override var alpha: Float
        get() = Color(skia.getColor().toInt()).alpha
        set(value) {
            skia.setColor(Color(skia.getColor().toInt()).copy(alpha = value).toArgb().toUInt())
        }

    override var isAntiAlias: Boolean
        get() = skia.isAntiAlias()
        set(value) {
            skia.setAntiAlias(value)
        }

    override var color: Color
        get() = Color(skia.getColor().toInt())
        set(color) {
            skia.setColor(color.toArgb().toUInt())
        }

    override var blendMode: BlendMode = BlendMode.SrcOver
        set(value) {
            skia.setBlendMode(value.toSkia())
            field = value
        }

    override var style: PaintingStyle = PaintingStyle.Fill
        set(value) {
            skia.setStyle(value.toSkia())
            field = value
        }

    override var strokeWidth: Float
        get() = skia.getStrokeWidth()
        set(value) {
            skia.setStrokeWidth(value)
        }

    override var strokeCap = StrokeCap.Butt
        set(value) {
            skia.setStrokeCap(value.toSkia())
            field = value
        }

    override var strokeJoin: StrokeJoin = StrokeJoin.Round
        set(value) {
            skia.setStrokeJoin(value.toSkia())
            field = value
        }

    override var strokeMiterLimit: Float = 0f
        set(value) {
            skia.setStrokeMiter(value)
            field = value
        }

    override var filterQuality: FilterQuality = FilterQuality.None
        set(value) {
            skia.setFilterQuality(value.toSkia())
            field = value
        }

    override var shader: Shader? = null
        set(value) {
            TODO("figure out NativePaint shader")
            // skia.setShader(value)
            field = value
        }

    override var colorFilter: ColorFilter? = null
        set(value) {
            TODO("figure out color filter")
            // skia.colorFilter = value?.asNativeColorFilter()
            field = value
        }

    override var pathEffect: PathEffect? = null
        set(value) {
            TODO("figur out NativePaint path effect")
            // skia.setPathEffect((value as NativePathEffect?)?.asNativePathEffect())
            field = value
        }

    private fun PaintingStyle.toSkia() = when (this) {
        PaintingStyle.Fill -> SkPaint__Style.kFill_Style
        PaintingStyle.Stroke -> SkPaint__Style.kStroke_Style
        else -> error("Unexpected PaintingStyle")
    }

    private fun StrokeCap.toSkia() = when (this) {
        StrokeCap.Butt -> kButt_Cap
        StrokeCap.Round -> kRound_Cap
        StrokeCap.Square -> kSquare_Cap
        else -> error("Unexpected StrokeCap")
    }

    private fun StrokeJoin.toSkia() = when (this) {
        StrokeJoin.Miter -> kMiter_Join
        StrokeJoin.Round -> kRound_Join
        StrokeJoin.Bevel -> kBevel_Join
        else -> error("Unexpected StrokeJoin")
    }

    private fun FilterQuality.toSkia(): SkFilterQuality =
        when (this) {
            FilterQuality.None -> kNone_SkFilterQuality
            FilterQuality.Low -> kLow_SkFilterQuality
            FilterQuality.Medium -> kMedium_SkFilterQuality
            FilterQuality.High -> kHigh_SkFilterQuality
            else -> error("Unexpected FilterQuality")
        }
}

actual fun BlendMode.isSupported(): Boolean = true