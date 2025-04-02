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

package androidx.test.uiautomator.internal

import android.view.accessibility.AccessibilityNodeInfo
import androidx.test.uiautomator.NodeFilterScope
import androidx.test.uiautomator.children

/**
 * Internal core function that performs a DFS for all the publicly exposed api. This is called by
 * all the [androidx.test.uiautomator.onView] and [androidx.test.uiautomator.onViews] functions.
 */
internal fun findViews(
    timeoutMs: Long,
    pollIntervalMs: Long,
    shouldStop: (MutableList<AccessibilityNodeInfo>) -> (Boolean),
    block: NodeFilterScope.() -> (Boolean),
    rootNodeBlock: () -> (List<AccessibilityNodeInfo>)
): List<AccessibilityNodeInfo> {

    // DFS to find a view matching the given filter
    fun dfs(
        node: AccessibilityNodeInfo,
        depth: Int,
        collected: MutableList<AccessibilityNodeInfo>,
        indexPtr: IntArray,
    ) {

        val n =
            NodeFilterScope(
                view = node,
                depth = depth,
                lazyChildren = { node.children() },
                index = indexPtr[0]
            )

        // Check if this is the node we're looking for
        if (block(n)) {
            collected.add(node)
            if (shouldStop(collected)) return
        }

        // If not, explore the children.
        n.children.forEach { child ->
            indexPtr[0]++
            dfs(child, depth + 1, collected, indexPtr)
            indexPtr[0]--
        }
    }

    // Run DFS starting from root produced by the given factory.
    fun findNodes(): List<AccessibilityNodeInfo> {
        val clock = TimeoutClock(timeoutMs = timeoutMs, sleepIntervalMs = pollIntervalMs)
        while (true) {
            val foundNodes =
                rootNodeBlock()
                    .map {
                        val list = mutableListOf<AccessibilityNodeInfo>()
                        dfs(node = it, depth = 0, collected = list, indexPtr = intArrayOf(0))
                        list
                    }
                    .flatten()
            if (foundNodes.isNotEmpty()) return foundNodes
            if (clock.isTimeoutOrSleep()) return emptyList()

            // Run the ui watchers, in case there is a ui watcher for a dialog to dismiss.
            uiDevice.runWatchers()
        }
    }
    return findNodes()
}
