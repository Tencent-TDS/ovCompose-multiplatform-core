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

package androidx.compose.ui.platform.accessibility

import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.semantics.SemanticsOwner
import kotlin.coroutines.EmptyCoroutineContext
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL
import platform.arkui.ArkUI_AccessibilityFocusMoveDirection
import platform.arkui.ArkUI_AccessibilityFocusType
import platform.arkui.ArkUI_AccessibilityProviderCallbacks
import platform.arkui.ArkUI_AccessibilitySearchMode
import platform.arkui.ArkUI_Accessibility_ActionType
import platform.arkui.OH_ArkUI_AccessibilityProviderRegisterCallback
import platform.arkui.OH_NativeXComponent_GetNativeAccessibilityProvider

class SemanticsOwnerListenerImpl(private val component: OHNativeXComponent) :
    PlatformContext.SemanticsOwnerListener {

    private val callbacks = nativeHeap.alloc<ArkUI_AccessibilityProviderCallbacks> {
        findAccessibilityNodeInfosById = FindAccessibilityNodeInfosById
        findAccessibilityNodeInfosByText = FindAccessibilityNodeInfosByText
        findFocusedAccessibilityNode = FindFocusedAccessibilityNode
        findNextFocusAccessibilityNode = FindNextFocusAccessibilityNode
        executeAccessibilityAction = ExecuteAccessibilityAction
        getAccessibilityNodeCursorPosition = GetAccessibilityNodeCursorPosition
        clearFocusedFocusAccessibilityNode = ClearFocusedFocusAccessibilityNode
    }

    init {
        memScoped {
            val provider = alloc<CPointerVar<cnames.structs.ArkUI_AccessibilityProvider>>()
            OH_NativeXComponent_GetNativeAccessibilityProvider(component, provider.ptr)
            OH_ArkUI_AccessibilityProviderRegisterCallback(provider.value, callbacks.ptr)
        }
    }

    fun dispose() {
        nativeHeap.free(callbacks)
    }

    override fun onSemanticsOwnerAppended(semanticsOwner: SemanticsOwner) {
        AccessibilityMediatorMap[semanticsOwner] = AccessibilityMediator(semanticsOwner)
    }

    override fun onSemanticsOwnerRemoved(semanticsOwner: SemanticsOwner) {
        AccessibilityMediatorMap.remove(semanticsOwner)
    }

    override fun onSemanticsChange(semanticsOwner: SemanticsOwner) {
        AccessibilityMediatorMap[semanticsOwner]?.onSemanticsChange()
    }

    private companion object ArkUIXComponentAccessibility {
        private val AccessibilityMediatorMap = mutableMapOf<SemanticsOwner, AccessibilityMediator>()

        private val FindAccessibilityNodeInfosById =
            staticCFunction { elementId: Long, mode: ArkUI_AccessibilitySearchMode, requestId: Int, elementList: ArkUIAccessibilityElementInfoList ->
                withMainThread {
                    val mediator = findAccessibilityMediator(elementId)
                    mediator?.findAccessibilityNodeInfosById(
                        elementId.toInt(), mode, requestId, elementList
                    )
                    ARKUI_ACCESSIBILITY_NATIVE_RESULT_SUCCESSFUL
                }
            }

        private val FindAccessibilityNodeInfosByText =
            staticCFunction { elementId: Long, text: CPointer<ByteVar>?, requestId: Int, elementList: ArkUIAccessibilityElementInfoList ->
                ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
            }

        private val FindFocusedAccessibilityNode =
            staticCFunction { elementId: Long, type: ArkUI_AccessibilityFocusType, requestId: Int, info: ArkUIAccessibilityElementInfo ->
                ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
            }

        private val FindNextFocusAccessibilityNode =
            staticCFunction { elementId: Long, direction: ArkUI_AccessibilityFocusMoveDirection, requestId: Int, info: ArkUIAccessibilityElementInfo ->
                ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
            }

        private val ExecuteAccessibilityAction =
            staticCFunction { elementId: Long, action: ArkUI_Accessibility_ActionType, actionArguments: ArkUIAccessibilityActionArguments, requestId: Int ->
                ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
            }

        private val GetAccessibilityNodeCursorPosition =
            staticCFunction { elementId: Long, requestId: Int, index: CPointer<IntVar>? ->
                ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
            }

        private val ClearFocusedFocusAccessibilityNode = staticCFunction { ->
            ARKUI_ACCESSIBILITY_NATIVE_RESULT_FAILED
        }

        private fun findAccessibilityMediator(elementId: Long): AccessibilityMediator? {
            if (elementId < 0) return AccessibilityMediatorMap.values.lastOrNull()
            val id = elementId.toInt()
            return AccessibilityMediatorMap.values.findLast { id in it }
        }

        private fun <T> withMainThread(block: () -> T): T {
            return if (Dispatchers.Main.immediate.isDispatchNeeded(EmptyCoroutineContext)) {
                runBlocking(Dispatchers.Main) { block() }
            } else {
                block()
            }
        }
    }
}
