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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.gestures.OHOSScrollConfig
import androidx.compose.foundation.cupertino.CupertinoOverscrollEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection

@ExperimentalFoundationApi
@Composable
internal actual fun rememberOverscrollEffect(): OverscrollEffect {
    val applyClip = false
    return if (OHOSScrollConfig.isRubberBandingOverscrollEnabled) {
        val density = LocalDensity.current.density
        val layoutDirection = LocalLayoutDirection.current

        remember(density, layoutDirection) {
            CupertinoOverscrollEffect(density, layoutDirection, applyClip)
        }
    } else {
        NoOpOverscrollEffect
    }
}
