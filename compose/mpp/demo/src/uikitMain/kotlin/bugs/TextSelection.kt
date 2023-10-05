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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Text
import androidx.compose.mpp.demo.Screen
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

val TextSelection = Screen.Example("TextSelection") {
    // https://github.com/JetBrains/compose-multiplatform/issues/3718
    SelectionContainer {

        Column {

            Text("Some")

            Spacer(Modifier.height(8.dp))

            Text("multiline")

            Spacer(Modifier.height(8.dp))

            Text("")

            Spacer(Modifier.height(8.dp))

            Text("text to select in SelectionContainer")
        }
    }
}
