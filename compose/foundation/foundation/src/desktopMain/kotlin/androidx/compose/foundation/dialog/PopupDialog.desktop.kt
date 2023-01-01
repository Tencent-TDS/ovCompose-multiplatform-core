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

package androidx.compose.foundation.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.awtEventOrNull
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import java.awt.event.KeyEvent

@InternalComposeApi
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PopupDialog(onDismissRequest: () -> Unit, content: @Composable () -> Unit) {
    //TODO Change PopupDialog to LayerDialog (https://github.com/JetBrains/compose-jb/issues/933)
    // Popups on the desktop are by default embedded in the component in which
    // they are defined and aligned within its bounds. But Dialog needs
    // to be aligned within the window, not the parent component, so we cannot use
    // [alignment] property of [Popup] and have to use [Box] that fills all the
    // available space. Also [Box] provides a dismiss request feature when clicked
    // outside of the [AlertDialog] content.
    Popup(
        popupPositionProvider = object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize
            ): IntOffset = IntOffset.Zero
        },
        focusable = true,
        onDismissRequest = onDismissRequest,
        onKeyEvent = {
            if (it.type == KeyEventType.KeyDown && it.awtEventOrNull?.keyCode == KeyEvent.VK_ESCAPE) {
                onDismissRequest()
                true
            } else {
                false
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(LocalDialogSettings.current.scrimColor)
                .pointerInput(onDismissRequest) {
                    detectTapGestures(onPress = { onDismissRequest() })
                },
            contentAlignment = Alignment.Center
        ) {
            content()
        }
    }
}
