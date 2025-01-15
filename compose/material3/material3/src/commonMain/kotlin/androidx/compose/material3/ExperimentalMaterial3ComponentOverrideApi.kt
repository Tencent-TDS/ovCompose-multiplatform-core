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

package androidx.compose.material3

<<<<<<<< HEAD:compose/material3/material3/src/skikoMain/kotlin/androidx/compose/material3/Tooltip.skiko.kt
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalWindowInfo

@Composable
internal actual fun windowContainerWidthInPx(): Int =
    LocalWindowInfo.current.containerSize.width
========
@RequiresOptIn(
    "This material3 API is experimental and is likely to change or to be removed in the" +
        " future."
)
@Retention(AnnotationRetention.BINARY)
annotation class ExperimentalMaterial3ComponentOverrideApi
>>>>>>>> c80a82c4ab50276ac6c1a8d9b9175c9fdbb0d1b8:compose/material3/material3/src/commonMain/kotlin/androidx/compose/material3/ExperimentalMaterial3ComponentOverrideApi.kt
