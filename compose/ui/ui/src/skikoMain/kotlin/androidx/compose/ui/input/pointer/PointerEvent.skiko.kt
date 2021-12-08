/*
 * Copyright 2021 The Android Open Source Project
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


package androidx.compose.ui.input.pointer

import org.jetbrains.skiko.SkikoPointerEventKind
import org.jetbrains.skiko.SkikoGestureEventState

fun SkikoPointerEventKind.toCompose() = when(this) {
    SkikoPointerEventKind.UP -> PointerEventType.Release
    SkikoPointerEventKind.DOWN -> PointerEventType.Press
    SkikoPointerEventKind.MOVE -> PointerEventType.Move
    else -> PointerEventType.Unknown
}

fun SkikoGestureEventState.toCompose() = when(this) {
    SkikoGestureEventState.ENDED -> PointerEventType.Release
    SkikoGestureEventState.STARTED -> PointerEventType.Press
    SkikoGestureEventState.CHANGED -> PointerEventType.Move
    else -> PointerEventType.Unknown
}
