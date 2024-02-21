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

package androidx.compose.ui.text

import androidx.compose.ui.text.platform.Platform
import androidx.compose.ui.text.platform.currentPlatform

/**
 * Whether edge pixels draw opaque or with partial transparency.
 */
@ExperimentalTextApi
enum class FontSmoothing {
    /**
     * no transparent pixels on glyph edges
     */
    None,

    /**
     * may have transparent pixels on glyph edges
     */
    AntiAlias,

    /**
     * glyph positioned in pixel using transparency
     */
    SubpixelAntiAlias;
}

/**
 * Level of glyph outline adjustment
 */
@ExperimentalTextApi
enum class FontHinting {
    /**
     * glyph outlines unchanged
     */
    None,

    /**
     * minimal modification to improve constrast
     */
    Slight,

    /**
     * glyph outlines modified to improve constrast
     */
    Normal,

    /**
     * modifies glyph outlines for maximum constrast
     */
    Full;
}

@ExperimentalTextApi
class FontRasterizationSettings(
    val smoothing: FontSmoothing,
    val hinting: FontHinting,
    val subpixelPositioning: Boolean,
    val autoHintingForced: Boolean
) {
    companion object {
        val PlatformDefault by lazy {
            when (currentPlatform()) {
                Platform.Windows -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    smoothing = FontSmoothing.SubpixelAntiAlias, // Most UIs still use ClearType on Windows, so we should match this
                    hinting = FontHinting.Normal, // None would trigger some potentially unwanted behavior, but everything else is forced into Normal on Windows
                    autoHintingForced = false,
                )

                Platform.Linux, Platform.Android, Platform.Unknown -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    smoothing = FontSmoothing.SubpixelAntiAlias, // Not all distributions default to SubpixelAntiAlias, but we still do to ensure sharpness on Low-DPI displays
                    hinting = FontHinting.Slight, // Most distributions use Slight now by default
                    autoHintingForced = false,
                )

                Platform.MacOS, Platform.IOS, Platform.TvOS, Platform.WatchOS -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    smoothing = FontSmoothing.AntiAlias, // macOS doesn't support SubpixelAntiAlias anymore as of Catalina
                    hinting = FontHinting.Normal, // Completely ignored on macOS
                    autoHintingForced = false, // Completely ignored on macOS
                )
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as FontRasterizationSettings

        if (smoothing != other.smoothing) return false
        if (hinting != other.hinting) return false
        if (subpixelPositioning != other.subpixelPositioning) return false
        return autoHintingForced == other.autoHintingForced
    }

    override fun hashCode(): Int {
        var result = smoothing.hashCode()
        result = 31 * result + hinting.hashCode()
        result = 31 * result + subpixelPositioning.hashCode()
        result = 31 * result + autoHintingForced.hashCode()
        return result
    }

    override fun toString(): String {
        return "FontRasterizationSettings(smoothing=$smoothing, " +
            "hinting=$hinting, " +
            "subpixelPositioning=$subpixelPositioning, " +
            "autoHintingForced=$autoHintingForced)"
    }
}