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

package androidx.compose.ui.modifier

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.Modifier.Element

// region Tencent Code
/**
 * Provides a means of injecting modifier at the tail of modifier chain
 */
interface ModifierInjection {
    /**
     * [inject] is called on Composer.materialize and guaranteed to be called only once.
     *  Add an injection modifier based on incoming modifier. like current.then(injectModifier)
     *
     *  @param current contain all modifier added by user
     *  @return must contain incoming modifier
     *
     */
    fun inject(current: Modifier): Modifier
}

/**
 * an object used to guarantee that [ModifierInjection.inject] is called only once in one chain.
 */
internal data object InjectModifier : Element

internal val LocalModifierInjection = staticCompositionLocalOf<ModifierInjection?> {
    null
}

/**
 * Support injecting modifier for all ui nodes under specific ui node
 *
 * @see ModifierInjection
 */
@Composable
fun LocalModifierInject(
    modifierInjection: ModifierInjection,
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(
        LocalModifierInjection provides modifierInjection,
        content = content
    )
}
// endregion