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

package androidx.compose.foundation.gestures.monitor

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow

/**
 * Apply scrollable monitor to the [ScrollableState] if monitor and page name are available.
 * All instances of ScrollableState should be applied with this implicitly.
 */
@OptIn(InternalComposeApi::class)
@Composable
internal fun ApplyScrollableMonitor(state: ScrollableState) {
    val monitor = StaticScrollableMonitor.current
    if (monitor != null) {
        // Get pageName only if the monitor is nonnull to avoid
        val pageNames = StaticPageNames.current
        if (pageNames.isNotEmpty()) {
            val priority = StaticScrollableMutatePriority.current
            var isScrollInProgressSaved by remember { mutableStateOf(false) }
            var isMonitoring by  remember { mutableStateOf(false) }
            val isScrollInProgressNew = state.isScrollInProgress
            // Do not use 'snapshotFlow' !!!
            // Scrolls triggered by user input will have its state changes by
            // false -> true (start dragging) -> false (end dragging) -> true (start fling) -> false (end fling)
            // The changes from dragging to fling should be ignored
            // to treat dragging and fling as two phases of a single scroll
            // while 'snapshotFlow' may be too sensitive sometimes to achieve this.
            LaunchedEffect(isScrollInProgressNew) {
                // value of state.isScrollInProgress may change again.
                val isScrollInProgress =
                    if (monitor.isAggressive) isScrollInProgressNew else state.isScrollInProgress

                // Get rid of the first call of onComposeListStopScroll.
                if (isScrollInProgressSaved != isScrollInProgress) {
                    if (isScrollInProgress) {
                        // monitor the scroll for the specific priority.
                        if (priority == null || state.currentScrollPriority == priority) {
                            monitor.onStartScroll(pageNames)
                            isMonitoring = true
                        }
                    } else if (isMonitoring) {
                        monitor.onStopScroll()
                        isMonitoring = false
                    }
                    isScrollInProgressSaved = isScrollInProgress
                }
            }

            DisposableEffect(Unit) {
                onDispose {
                    // Ensure to notify the monitor to reset the state
                    // When current node is removing from the layout tree.
                    if (isScrollInProgressSaved) {
                        monitor.onStopScroll()
                    }
                }
            }
        }
    }
}