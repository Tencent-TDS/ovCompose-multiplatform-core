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

package bugs

import androidx.compose.foundation.text.BasicTextField
import androidx.compose.mpp.demo.Screen
import androidx.compose.runtime.*

val SelectionHandlePadding = Screen.Example("SelectionHandlePadding") {
    var text by remember { mutableStateOf("select the text from the left edge to see the bug") }
    BasicTextField(text, { text = it })
}
