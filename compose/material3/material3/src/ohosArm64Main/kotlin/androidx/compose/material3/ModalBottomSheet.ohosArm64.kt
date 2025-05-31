/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable

/**
 * Properties used to customize the behavior of a [ModalBottomSheet].
 *
 * @param isFocusable Whether the modal bottom sheet is focusable. When true,
 * the modal bottom sheet will receive IME events and key presses, such as when
 * the back button is pressed.
 * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
 * the back button. If true, pressing the back button will call onDismissRequest.
 * Note that [isFocusable] must be set to true in order to receive key events such as
 * the back button - if the modal bottom sheet is not focusable then this property does nothing.
 */
@ExperimentalMaterial3Api
actual class ModalBottomSheetProperties actual constructor(
    isFocusable: Boolean,
    shouldDismissOnBackPress: Boolean
) {
    actual val isFocusable: Boolean
        get() = TODO("Not yet implemented")
    actual val shouldDismissOnBackPress: Boolean
        get() = TODO("Not yet implemented")
}

/**
 * Default values for [ModalBottomSheet]
 */
@Immutable
@ExperimentalMaterial3Api
actual object ModalBottomSheetDefaults {
    /**
     * Properties used to customize the behavior of a [ModalBottomSheet].
     *
     * @param isFocusable Whether the modal bottom sheet is focusable. When true,
     * the modal bottom sheet will receive IME events and key presses, such as when
     * the back button is pressed.
     * @param shouldDismissOnBackPress Whether the modal bottom sheet can be dismissed by pressing
     * the back button. If true, pressing the back button will call onDismissRequest.
     * Note that [isFocusable] must be set to true in order to receive key events such as
     * the back button - if the modal bottom sheet is not focusable then this property does nothing.
     */
    actual fun properties(
        isFocusable: Boolean,
        shouldDismissOnBackPress: Boolean
    ): ModalBottomSheetProperties {
        TODO("Not yet implemented")
    }
}

/**
 * Popup specific for modal bottom sheet.
 */
@Composable
internal actual fun ModalBottomSheetPopup(
    properties: ModalBottomSheetProperties,
    onDismissRequest: () -> Unit,
    windowInsets: WindowInsets,
    content: @Composable () -> Unit
) {
}