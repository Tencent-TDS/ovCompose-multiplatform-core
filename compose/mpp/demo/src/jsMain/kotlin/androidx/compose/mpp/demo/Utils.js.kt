/*
 * Copyright 2025 The Android Open Source Project
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

@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package androidx.compose.mpp.demo

import androidx.compose.foundation.internal.readText
import androidx.compose.ui.platform.ClipEntry

actual suspend fun ClipEntry?.getPlainText(): String? {
    return this?.readText()
}

actual fun createClipEntryWithPlainText(text: String): ClipEntry {
    return ClipEntry.withPlainText(text)
}