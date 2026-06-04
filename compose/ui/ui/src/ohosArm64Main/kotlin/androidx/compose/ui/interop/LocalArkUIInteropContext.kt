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

package androidx.compose.ui.interop

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.OhosLock

enum class ArkUIInteropState {
    BEGAN, UNCHANGED, ENDED
}

enum class ArkUIInteropViewHierarchyChange {
    VIEW_ADDED,
    VIEW_REMOVED
}

/**
 * Lambda containing changes to UIKit objects, which can be synchronized within [CATransaction]
 */
typealias ArkUIInteropAction = () -> Unit

interface ArkUIInteropTransaction {
    val actions: List<ArkUIInteropAction>
    val state: ArkUIInteropState

    companion object {
        val empty = object : ArkUIInteropTransaction {
            override val actions: List<ArkUIInteropAction>
                get() = emptyList()

            override val state: ArkUIInteropState
                get() = ArkUIInteropState.UNCHANGED
        }
    }
}

internal fun ArkUIInteropTransaction.isEmpty() = actions.isEmpty() && state == ArkUIInteropState.UNCHANGED
internal fun ArkUIInteropTransaction.isNotEmpty() = !isEmpty()

private class ArkUIInteropMutableTransaction : ArkUIInteropTransaction {
    override val actions = mutableListOf<ArkUIInteropAction>()
    override var state = ArkUIInteropState.UNCHANGED
        set(value) {
            field = when (value) {
                ArkUIInteropState.UNCHANGED -> error("Can't assign UNCHANGED value explicitly")
                ArkUIInteropState.BEGAN -> when (field) {
                    ArkUIInteropState.BEGAN -> error("Can't assign BEGAN twice in the same transaction")
                    ArkUIInteropState.UNCHANGED -> value
                    ArkUIInteropState.ENDED -> ArkUIInteropState.UNCHANGED
                }

                ArkUIInteropState.ENDED -> when (field) {
                    ArkUIInteropState.BEGAN -> ArkUIInteropState.UNCHANGED
                    ArkUIInteropState.UNCHANGED -> value
                    ArkUIInteropState.ENDED -> error("Can't assign ENDED twice in the same transaction")
                }
            }
        }
}

/**
 * Class which can be used to add actions related to ArkUI objects to be executed in sync with compose rendering,
 * Addding deferred actions is threadsafe, but they will be executed in the order of their submission, and on the main thread.
 */
class ArkUIInteropContext(
    val requestRedraw: () -> Unit
) {
    private val lock: OhosLock = OhosLock()
    private var transaction = ArkUIInteropMutableTransaction()

    /**
     * Number of views, created by interop API and present in current view hierarchy
     */
    private var viewsCount = 0
        set(value) {
            require(value >= 0)

            field = value
        }

    /**
     * Add lambda to a list of commands which will be executed later in the same CATransaction, when the next rendered Compose frame is presented
     */
    fun deferAction(hierarchyChange: ArkUIInteropViewHierarchyChange? = null, action: () -> Unit) {
        requestRedraw()

        lock.doLocked {
            if (hierarchyChange == ArkUIInteropViewHierarchyChange.VIEW_ADDED) {
                if (viewsCount == 0) {
                    transaction.state = ArkUIInteropState.BEGAN
                }
                viewsCount += 1
            }

            transaction.actions.add(action)

            if (hierarchyChange == ArkUIInteropViewHierarchyChange.VIEW_REMOVED) {
                viewsCount -= 1
                if (viewsCount == 0) {
                    transaction.state = ArkUIInteropState.ENDED
                }
            }
        }
    }

    /**
     * Return an object containing pending changes and reset internal storage
     */
    fun retrieve(): ArkUIInteropTransaction =
        lock.doLocked {
            val result = transaction
            transaction = ArkUIInteropMutableTransaction()
            result
        }
}

internal inline fun <T> OhosLock.doLocked(block: () -> T): T {
    lock()

    try {
        return block()
    } finally {
        unlock()
    }
}

val LocalArkUIInteropContext = staticCompositionLocalOf<ArkUIInteropContext> {
    error("CompositionLocal ArkUIInteropContext not provided")
}
