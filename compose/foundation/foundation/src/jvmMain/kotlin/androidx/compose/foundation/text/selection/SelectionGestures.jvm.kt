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

package androidx.compose.foundation.text.selection

import androidx.compose.foundation.text.TextDragObserver
import androidx.compose.ui.input.pointer.PointerInputScope

/**
 * Gesture handler for mouse and touch. Determines whether this is mouse or touch based on the first
 * down, then uses the gesture handler for that input type, delegating to the appropriate observer.
 */
internal actual suspend fun PointerInputScope.selectionGesturePointerInputBtf2(
    mouseSelectionObserver: MouseSelectionObserver,
    textDragObserver: TextDragObserver
) = defaultSelectionGesturePointerInputBtf2(mouseSelectionObserver, textDragObserver)