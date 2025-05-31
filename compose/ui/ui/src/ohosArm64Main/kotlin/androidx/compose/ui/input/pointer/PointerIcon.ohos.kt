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

package androidx.compose.ui.input.pointer

private data class OhosCursor(val id: String) : PointerIcon

// TODO("补API；后续可能参考import pointer from '@ohos.multimodalInput.pointer'）
internal actual val pointerIconDefault: PointerIcon = OhosCursor("default")
internal actual val pointerIconCrosshair: PointerIcon = OhosCursor("crosshair")
internal actual val pointerIconText: PointerIcon = OhosCursor("text")
internal actual val pointerIconHand: PointerIcon = OhosCursor("hand")


