/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.mpp.demo

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Checkbox
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController

// TODO This module is just a proxy to run the demo from mpp:demo. Figure out how to get rid of it.
//  If it is removed, there is no available configuration in IDE
fun getViewControllerWithCompose() = ComposeUIViewController {
    Column {
        var checked1 by remember { mutableStateOf(false) }
        Checkbox(checked1, { checked1 = it })
        var checked2 by remember { mutableStateOf(false) }
        Checkbox(checked2, { checked2 = it })
    }
}
