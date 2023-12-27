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

import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.DpSize

/**
 * Specifies how a [magnifier] should create the underlying [Magnifier] widget. These properties
 * should not be changed while a magnifier is showing, since the magnifier will be dismissed and
 * recreated with the new properties which will cause it to disappear for at least a frame.
 *
 * Not all magnifier features are supported on all platforms. The [isSupported] property will return
 * false for styles that cannot be fully supported on the given platform.
 *
 * @param size See [Magnifier.Builder.setSize]. Only supported on API 29+.
 * @param cornerRadius See [Magnifier.Builder.setCornerRadius]. Only supported on API 29+.
 * @param elevation See [Magnifier.Builder.setElevation]. Only supported on API 29+.
 * @param clippingEnabled See [Magnifier.Builder.setClippingEnabled]. Only supported on API 29+.
 * @param fishEyeEnabled Configures the magnifier to distort the magnification at the edges to
 * look like a fisheye lens. Not currently supported.
 */
@ExperimentalFoundationApi
@Stable
expect class MagnifierStyle internal constructor(
    useTextDefault: Boolean,
    size: DpSize,
    cornerRadius: Dp,
    elevation: Dp,
    clippingEnabled: Boolean,
    fishEyeEnabled: Boolean
) {

    internal val useTextDefault: Boolean
    internal val size: DpSize
    internal val cornerRadius: Dp
    internal val elevation: Dp
    internal val clippingEnabled: Boolean
    internal val fishEyeEnabled: Boolean

    @ExperimentalFoundationApi
    constructor(
        size: DpSize = DpSize.Unspecified,
        cornerRadius: Dp = Dp.Unspecified,
        elevation: Dp = Dp.Unspecified,
        clippingEnabled: Boolean = true,
        fishEyeEnabled: Boolean = false
    )

    /**
     * Returns true if this style is supported by this version of the platform.
     * When false is returned, it may be either because the [Magnifier] widget is not supported at
     * all because the platform is too old, or because a particular style flag (e.g.
     * [fishEyeEnabled]) is not supported on the current platform.
     * [Default] and [TextDefault] styles are supported on all platforms with SDK version 28 and
     * higher.
     */
    val isSupported: Boolean

    companion object {
        /** A [MagnifierStyle] with all default values. */
        @ExperimentalFoundationApi
        val Default : MagnifierStyle

        /**
         * A [MagnifierStyle] that uses the system defaults for text magnification.
         *
         * Different versions of Android may use different magnifier styles for magnifying text, so
         * using this configuration ensures that the correct style is used to match the system.
         */
        @ExperimentalFoundationApi
        val TextDefault : MagnifierStyle

    }
}

/**
 * Shows a [Magnifier] widget that shows an enlarged version of the content at [sourceCenter]
 * relative to the current layout node.
 *
 * This function returns a no-op modifier on API levels below P (28), since the framework does not
 * support the [Magnifier] widget on those levels. However, even on higher API levels, not all
 * magnifier features are supported on all platforms. To check whether a given [MagnifierStyle] is
 * supported by the current platform, check the [MagnifierStyle.isSupported] property.
 *
 * This function does not allow configuration of [source bounds][Magnifier.Builder.setSourceBounds]
 * since the magnifier widget does not support constraining to the bounds of composables.
 *
 * @sample androidx.compose.foundation.samples.MagnifierSample
 *
 * @param sourceCenter The offset of the center of the magnified content. Measured in pixels from
 * the top-left of the layout node this modifier is applied to. This offset is passed to
 * [Magnifier.show].
 * @param magnifierCenter The offset of the magnifier widget itself, where the magnified content is
 * rendered over the original content. Measured in density-independent pixels from the top-left of
 * the layout node this modifier is applied to. If [unspecified][DpOffset.Unspecified], the
 * magnifier widget will be placed at a default offset relative to [sourceCenter]. The value of that
 * offset is specified by the system.
 * @param zoom See [Magnifier.setZoom]. Not supported on SDK levels < Q.
 * @param style The [MagnifierStyle] to use to configure the magnifier widget.
 * @param onSizeChanged An optional callback that will be invoked when the magnifier widget is
 * initialized to report on its actual size. This can be useful if one of the default
 * [MagnifierStyle]s is used to find out what size the system decided to use for the widget.
 */
@ExperimentalFoundationApi
expect fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset = { Offset.Unspecified },
    zoom: Float = Float.NaN,
    style: MagnifierStyle = MagnifierStyle.Default,
    onSizeChanged: ((DpSize) -> Unit)? = null
): Modifier