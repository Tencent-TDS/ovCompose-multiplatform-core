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

package androidx.compose.ui.viewinterop

import androidx.compose.ui.Modifier

/**
 * An interface for platform-specific configurable details of how interop is implemented on a specific
 * platform.
 *
 * It's needed in case the [InteropViewHolder] is reused with new properties and the modifier
 * chain needs to be changed within ComposeNode Updater
 */
internal interface InteropPlatformDetails {
    fun platformModifier(holder: InteropViewHolder): Modifier
}