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

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val SelectionContainer = Screen.Example("SelectionContainer") {
    // https://github.com/JetBrains/compose-multiplatform/issues/3718
    SelectionContainer {
        Column(Modifier.padding(40.dp)) {
            Text("aaa")
            Text("")
            Text("bbb")
        }
    }
}
