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

import androidx.compose.foundation.cupertino.CupertinoOverscrollEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalAccessorScope
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

@Composable
internal actual fun rememberPlatformOverscrollEffect(): OverscrollEffect? =
    rememberOverscrollEffect(applyClip = false)

@Composable
internal fun rememberOverscrollEffect(applyClip: Boolean): OverscrollEffect {
    val density = LocalDensity.current.density
    val layoutDirection = LocalLayoutDirection.current

    return remember(density, layoutDirection) {
        CupertinoOverscrollEffect(density, layoutDirection, applyClip)
    }
}

internal actual fun CompositionLocalAccessorScope.defaultOverscrollFactory(): OverscrollFactory? {
    val density = LocalDensity.currentValue
    val layoutDirection = LocalLayoutDirection.currentValue
    return CupertinoOverscrollEffectFactory(density, layoutDirection)
}

private data class CupertinoOverscrollEffectFactory(
    private val density: Density,
    private val layoutDirection: LayoutDirection
) : OverscrollFactory {
    override fun createOverscrollEffect(): OverscrollEffect {
        return CupertinoOverscrollEffect(density.density, layoutDirection, applyClip = false)
    }
}