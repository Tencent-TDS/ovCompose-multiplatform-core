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

package androidx.compose.ui.platform
value class TraceScene private constructor(val value: Int) {
    companion object {
        val None = TraceScene(0)

        // Compose Scene draw
        val DrawFrame = TraceScene(1)

        val ScheduledTasks = TraceScene(2)

        val Recomposition = TraceScene(3)

        val Layout = TraceScene(4)

        val Effects = TraceScene(5)

        val PointerInput = TraceScene(6)

        val Draw = TraceScene(7)

        // Compose Scene preComposition
        val PreCompose = TraceScene(8)

        // Compose Scene layout
        val PreMeasure = TraceScene(9)

        val PreComposeVsync = TraceScene(10)

    }
}

expect fun performTraceOpen(): Boolean
expect fun traceSync(scene: Int, taskId: Long, block: () -> Unit)
expect fun traceSync(scene: Int, block: () -> Unit)