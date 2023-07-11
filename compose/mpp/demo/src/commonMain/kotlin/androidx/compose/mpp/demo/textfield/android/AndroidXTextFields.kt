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

package androidx.compose.mpp.demo.textfield.android

import androidx.compose.mpp.demo.Screen

val AndroidXTextFields = Screen.Selection(
    "androidx TextFields",
    Screen.Example("Basic input fields") {

    },
    Screen.Example("Capitalization/AutoCorrect") {
        CapitalizationAutoCorrectDemo()
    },
    Screen.Example("Cursor configuration") {

    },
    Screen.Selection(
        "Focus",
        Screen.Example("Focus transition") {},
        Screen.Example("Focus keyboard interaction") {},
    ),
    Screen.Example("Full-screen field") {

    },
    Screen.Example("Ime Action") {

    },
    Screen.Example("Ime SingleLine") {

    },
    Screen.Example("Inside Dialog") {

    },
    Screen.Example("Inside scrollable") {

    },
    Screen.Example("Keyboard Types") {
        KeyboardTypeDemo()
    },
    Screen.Example("Min/Max Lines") {

    },
    Screen.Example("Reject Text Change") {

    },
    Screen.Example("Scrollable text fields") {

    },
    Screen.Example("Visual Transformation") {

    },
    Screen.Example("TextFieldValue") {

    },
    Screen.Example("Tail Following Text Field") {

    },
    Screen.Example("Focus immediately") {

    },
    Screen.Example("Secondary input system") {

    },
    Screen.Example("TextField focus") {

    },
)
