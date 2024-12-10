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

package androidx.compose.ui.autofill

import kotlin.jvm.JvmInline

// TODO https://youtrack.jetbrains.com/issue/CMP-7154/Adopt-Autofill-semantic-properties

actual typealias NativeContentDataType = Int

@JvmInline
actual value class ContentDataType actual constructor(val dataType: Int) {
    actual companion object {
        actual val Text = ContentDataType(1)
        actual val List = ContentDataType(2)
        actual val Date = ContentDataType(3)
        actual val Toggle = ContentDataType(4)
        actual val None = ContentDataType(0)
    }
}
