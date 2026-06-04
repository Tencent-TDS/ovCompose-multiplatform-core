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

package androidx.compose.foundation.gestures.monitor

import androidx.compose.foundation.MutatePriority
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

internal val StaticScrollableMonitor = staticCompositionLocalOf<ScrollableMonitor?> { null }
internal val StaticPageNames = staticCompositionLocalOf<List<String>> { emptyList() }
internal val StaticScrollableMutatePriority = staticCompositionLocalOf<MutatePriority?> { null }

enum class ScrollType {

    /**
     * Dragged by user and fling to settle.
     */
    Drag,

    /**
     * Scrolled with animation programmatically.
     */
    Animation,

    /**
     * All types above.
     */
    All,

    /**
     * Default. Inherit the value from parent scope.
     */
    Inherited;

    internal fun toMutatePriority(): MutatePriority? {
        return when(this) {
            Drag -> MutatePriority.UserInput
            Animation -> MutatePriority.Default
            All -> null
            Inherited -> null
        }
    }
}

@Composable
fun InstallScrollableMonitor(pageName: String, content: @Composable () -> Unit) {
    InstallScrollableMonitor(pageName, ScrollType.Inherited, content)
}

@Composable
inline fun InstallScrollableMonitor(
    vararg pageNames: String,
    noinline content: @Composable () -> Unit
) {
    InstallScrollableMonitor(pageNames.asList(), ScrollType.Inherited, content)
}

@Composable
fun InstallScrollableMonitor(pageNames: List<String>, content: @Composable () -> Unit) {
    InstallScrollableMonitor(pageNames, ScrollType.Inherited, content)
}

@Composable
fun InstallScrollableMonitor(
    pageName: String,
    type: ScrollType,
    content: @Composable () -> Unit
) {
    InstallScrollableMonitor(listOf(pageName), type, content)
}

@Composable
inline fun InstallScrollableMonitor(
    vararg pageNames: String,
    type: ScrollType,
    noinline content: @Composable () -> Unit
) {
    InstallScrollableMonitor(pageNames.asList(), type, content)
}

@Composable
fun InstallScrollableMonitor(
    pageNames: List<String>,
    type: ScrollType,
    content: @Composable () -> Unit
) {
    val localMonitor = sharedScrollableMonitor
    if (type == ScrollType.Inherited) {
        CompositionLocalProvider(
            StaticScrollableMonitor provides localMonitor,
            StaticPageNames provides StaticPageNames.current + pageNames
        ) {
            content()
        }
    } else {
        CompositionLocalProvider(
            StaticScrollableMonitor provides localMonitor,
            StaticPageNames provides StaticPageNames.current + pageNames,
            StaticScrollableMutatePriority provides type.toMutatePriority()
        ) {
            content()
        }
    }
}

@Composable
fun UninstallScrollableMonitor(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        StaticScrollableMonitor provides null,
        StaticPageNames provides emptyList(),
        StaticScrollableMutatePriority provides null
    ) {
        content()
    }
}