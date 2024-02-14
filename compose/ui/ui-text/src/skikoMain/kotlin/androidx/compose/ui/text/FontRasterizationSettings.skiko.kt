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
enum class FontEdging {
    /**
     * no transparent pixels on glyph edges
     */
    ALIAS,

    /**
     * may have transparent pixels on glyph edges
     */
    ANTI_ALIAS,

    /**
     * glyph positioned in pixel using transparency
     */
    SUBPIXEL_ANTI_ALIAS;
}

/**
 * Level of glyph outline adjustment
 */
enum class FontHinting {
    /**
     * glyph outlines unchanged
     */
    NONE,

    /**
     * minimal modification to improve constrast
     */
    SLIGHT,

    /**
     * glyph outlines modified to improve constrast
     */
    NORMAL,

    /**
     * modifies glyph outlines for maximum constrast
     */
    FULL;
}

data class FontRasterizationSettings(
    val edging: FontEdging,
    val hinting: FontHinting,
    val subpixelPositioning: Boolean,
    val autoHintingForced: Boolean
) {
    companion object {
        val defaultForCurrentPlatform by lazy {
            when (currentPlatform()) {
                Platform.Windows -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    edging = FontEdging.SUBPIXEL_ANTI_ALIAS, // Most UIs still use ClearType on Windows, so we should match this
                    hinting = FontHinting.NORMAL, // NONE would trigger some potentially unwanted behavior, but everything else is forced into NORMAL on Windows
                    autoHintingForced = false,
                )
                Platform.Linux, Platform.Android, Platform.Unknown -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    edging = FontEdging.SUBPIXEL_ANTI_ALIAS, // Not all distributions default to SUBPIXEL_ANTI_ALIAS, but we still do to ensure sharpness on Low-DPI displays
                    hinting = FontHinting.SLIGHT, // Most distributions use SLIGHT now by default
                    autoHintingForced = false,
                )
                Platform.MacOS, Platform.IOS, Platform.TvOS, Platform.WatchOS -> FontRasterizationSettings(
                    subpixelPositioning = true,
                    edging = FontEdging.ANTI_ALIAS, // macOS doesn't support SUBPIXEL_ANTI_ALIAS anymore as of Catalina
                    hinting = FontHinting.NORMAL, // Completely ignored on macOS
                    autoHintingForced = false, // Completely ignored on macOS
                )
            }
        }
    }
}