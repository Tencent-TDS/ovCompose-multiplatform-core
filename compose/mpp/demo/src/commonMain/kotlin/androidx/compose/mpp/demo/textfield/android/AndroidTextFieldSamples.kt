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

val AndroidTextFieldSamples = Screen.Selection(
    "Android TextField samples",
    Screen.Example("Basic input fields") { InputFieldDemo() },
    Screen.Example("Capitalization/AutoCorrect") { CapitalizationAutoCorrectDemo() },
    Screen.Example("Cursor configuration") { TextFieldCursorBlinkingDemo() },
    Screen.Selection(
        "Focus",
        Screen.Example("Focus transition") { TextFieldFocusTransition() },
        Screen.Example("Focus keyboard interaction") { TextFieldFocusKeyboardInteraction() },
    ),
    Screen.Example("Full-screen field") { FullScreenTextFieldDemo() },
    Screen.Example("Ime Action") { ImeActionDemo() },
    Screen.Example("Ime SingleLine") { ImeSingleLineDemo() },
    Screen.Example("Inside Dialog") { TextFieldsInDialogDemo() },
    Screen.Example("Inside scrollable") { TextFieldsInScrollableDemo() },
    Screen.Example("Keyboard Types") { KeyboardTypeDemo() },
    Screen.Example("Min/Max Lines") { BasicTextFieldMinMaxDemo() },
    Screen.Example("Reject Text Change") { RejectTextChangeDemo() },
    Screen.Example("Scrollable text fields") { ScrollableTextFieldDemo() },
    Screen.Example("Visual Transformation") { VisualTransformationDemo() },
    Screen.Example("TextFieldValue") { TextFieldValueDemo() },
    Screen.Example("Tail Following Text Field") { TailFollowingTextFieldDemo() },
    Screen.Example("Focus immediately") { FocusTextFieldImmediatelyDemo() },
    Screen.Example("Secondary input system") { PlatformTextInputAdapterDemo() },
    Screen.Example("TextField focus") { TextFieldFocusDemo() },
    Screen.Example("TextFieldBrush") { TextFieldBrush() },
)
