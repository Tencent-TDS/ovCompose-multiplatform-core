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

import androidx.compose.ui.unit.IntRect
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.CPointerVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.cstr
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toCValues
import kotlinx.cinterop.toKString
import kotlinx.cinterop.value
import platform.arkui.ArkUI_AccessibilityEventType
import platform.arkui.ArkUI_Accessibility_ActionType
import platform.arkui.ArkUI_AccessibleAction
import platform.arkui.ArkUI_AccessibleGridInfo
import platform.arkui.ArkUI_AccessibleGridItemInfo
import platform.arkui.ArkUI_AccessibleRangeInfo
import platform.arkui.ArkUI_AccessibleRect
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityDescription
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityFocused
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityGroup
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityLevel
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityOffset
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityOpacity
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetAccessibilityText
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetBackgroundColor
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetBackgroundImage
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetBlur
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetCheckable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetChecked
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetChildNodeIds
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetClickable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetComponentType
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetContents
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetCurrentItemIndex
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetEditable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetElementId
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetEnabled
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetEndItemIndex
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetFocusable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetFocused
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetGridInfo
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetGridItemInfo
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetHintText
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetHitTestBehavior
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetIsHint
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetIsPassword
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetItemCount
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetLongClickable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetOperationActions
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetParentId
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetRangeInfo
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetScreenRect
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetScrollable
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetSelected
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetSelectedTextEnd
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetSelectedTextStart
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetStartItemIndex
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetVisible
import platform.arkui.OH_ArkUI_AccessibilityElementInfoSetZIndex
import platform.arkui.OH_ArkUI_AccessibilityEventSetElementInfo
import platform.arkui.OH_ArkUI_AccessibilityEventSetEventType
import platform.arkui.OH_ArkUI_AccessibilityEventSetRequestFocusId
import platform.arkui.OH_ArkUI_AccessibilityEventSetTextAnnouncedForAccessibility
import platform.arkui.OH_ArkUI_CreateAccessibilityElementInfo
import platform.arkui.OH_ArkUI_CreateAccessibilityEventInfo
import platform.arkui.OH_ArkUI_FindAccessibilityActionArgumentByKey

typealias OHNativeXComponent = CPointer<cnames.structs.OH_NativeXComponent>?
typealias ArkUIAccessibilityElementInfo = CPointer<cnames.structs.ArkUI_AccessibilityElementInfo>?
typealias ArkUIAccessibilityEventInfo = CPointer<cnames.structs.ArkUI_AccessibilityEventInfo>?
typealias ArkUIAccessibilityActionArguments = CPointer<cnames.structs.ArkUI_AccessibilityActionArguments>?
typealias ArkUIAccessibilityElementInfoList = CPointer<cnames.structs.ArkUI_AccessibilityElementInfoList>?

fun ArkUIAccessibilityElementInfo(): ArkUIAccessibilityElementInfo =
    OH_ArkUI_CreateAccessibilityElementInfo()

fun ArkUIAccessibilityEventInfo(): ArkUIAccessibilityEventInfo =
    OH_ArkUI_CreateAccessibilityEventInfo()

fun ArkUIAccessibilityEventInfo.setElementInfo(info: ArkUIAccessibilityElementInfo) =
    OH_ArkUI_AccessibilityEventSetElementInfo(this, info)

fun ArkUIAccessibilityEventInfo.setEventType(type: ArkUI_AccessibilityEventType) =
    OH_ArkUI_AccessibilityEventSetEventType(this, type)

fun ArkUIAccessibilityEventInfo.setRequestFocusId(id: Int) =
    OH_ArkUI_AccessibilityEventSetRequestFocusId(this, id)

fun ArkUIAccessibilityEventInfo.setTextAnnouncedForAccessibility(text: String?) =
    OH_ArkUI_AccessibilityEventSetTextAnnouncedForAccessibility(this, text)

operator fun ArkUIAccessibilityActionArguments.get(key: String): String? =
    memScoped {
        val value = alloc<CPointerVar<ByteVar>>()
        OH_ArkUI_FindAccessibilityActionArgumentByKey(this@get, key, value.ptr)
        value.value?.toKString()
    }

// ArkUIAccessibilityElementInfo
fun ArkUIAccessibilityElementInfo.setAccessibilityText(text: String?) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityText(this, text)

fun ArkUIAccessibilityElementInfo.setAccessibilityDescription(desc: String?) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityDescription(this, desc)

fun ArkUIAccessibilityElementInfo.setAccessibilityFocused(focused: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityFocused(this, focused)

fun ArkUIAccessibilityElementInfo.setAccessibilityOffset(offset: Int) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityOffset(this, offset)

fun ArkUIAccessibilityElementInfo.setAccessibilityLevel(level: String?) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityLevel(this, level)

fun ArkUIAccessibilityElementInfo.setAccessibilityGroup(group: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityGroup(this, group)

fun ArkUIAccessibilityElementInfo.setAccessibilityOpacity(opacity: Float) =
    OH_ArkUI_AccessibilityElementInfoSetAccessibilityOpacity(this, opacity)

fun ArkUIAccessibilityElementInfo.setBlur(blur: String?) =
    OH_ArkUI_AccessibilityElementInfoSetBlur(this, blur)

fun ArkUIAccessibilityElementInfo.setBackgroundColor(color: String?) =
    OH_ArkUI_AccessibilityElementInfoSetBackgroundColor(this, color)

fun ArkUIAccessibilityElementInfo.setBackgroundImage(image: String?) =
    OH_ArkUI_AccessibilityElementInfoSetBackgroundImage(this, image)

fun ArkUIAccessibilityElementInfo.setChecked(checked: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetChecked(this, checked)

fun ArkUIAccessibilityElementInfo.setCheckable(checkable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetCheckable(this, checkable)

fun ArkUIAccessibilityElementInfo.setClickable(clickable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetClickable(this, clickable)

fun ArkUIAccessibilityElementInfo.setContents(contents: String?) =
    OH_ArkUI_AccessibilityElementInfoSetContents(this, contents)

fun ArkUIAccessibilityElementInfo.setComponentType(type: String?) =
    OH_ArkUI_AccessibilityElementInfoSetComponentType(this, type)

fun ArkUIAccessibilityElementInfo.setCurrentItemIndex(index: Int) =
    OH_ArkUI_AccessibilityElementInfoSetCurrentItemIndex(this, index)

fun ArkUIAccessibilityElementInfo.setChildNodeIds(childCount: Int, ids: LongArray) =
    OH_ArkUI_AccessibilityElementInfoSetChildNodeIds(this, childCount, ids.toCValues())

fun ArkUIAccessibilityElementInfo.setEnabled(enabled: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetEnabled(this, enabled)

fun ArkUIAccessibilityElementInfo.setEditable(editable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetEditable(this, editable)

fun ArkUIAccessibilityElementInfo.setElementId(id: Int) =
    OH_ArkUI_AccessibilityElementInfoSetElementId(this, id)

fun ArkUIAccessibilityElementInfo.setEndItemIndex(index: Int) =
    OH_ArkUI_AccessibilityElementInfoSetEndItemIndex(this, index)

fun ArkUIAccessibilityElementInfo.setFocused(focused: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetFocused(this, focused)

fun ArkUIAccessibilityElementInfo.setFocusable(focusable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetFocusable(this, focusable)

fun ArkUIAccessibilityElementInfo.setGridInfo(rowCount: Int, columnCount: Int, selectionMode: Int) =
    memScoped {
        val gridInfo = alloc<ArkUI_AccessibleGridInfo> {
            this.rowCount = rowCount
            this.columnCount = columnCount
            this.selectionMode = selectionMode
        }
        OH_ArkUI_AccessibilityElementInfoSetGridInfo(this@setGridInfo, gridInfo.ptr)
    }

fun ArkUIAccessibilityElementInfo.setGridItemInfo(
    heading: Boolean,
    selected: Boolean,
    columnIndex: Int,
    rowIndex: Int,
    columnSpan: Int,
    rowSpan: Int,
) = memScoped {
    val gridItemInfo = alloc<ArkUI_AccessibleGridItemInfo> {
        this.columnIndex = columnIndex
        this.columnSpan = columnSpan
        this.heading = heading
        this.rowIndex = rowIndex
        this.rowSpan = rowSpan
        this.selected = selected
    }
    OH_ArkUI_AccessibilityElementInfoSetGridItemInfo(this@setGridItemInfo, gridItemInfo.ptr)
}

fun ArkUIAccessibilityElementInfo.setHintText(hint: String?) =
    OH_ArkUI_AccessibilityElementInfoSetHintText(this, hint)

fun ArkUIAccessibilityElementInfo.setHitTestBehavior(behavior: String?) =
    OH_ArkUI_AccessibilityElementInfoSetHitTestBehavior(this, behavior)

fun ArkUIAccessibilityElementInfo.setIsHint(isHint: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetIsHint(this, isHint)

fun ArkUIAccessibilityElementInfo.setIsPassword(isPassword: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetIsPassword(this, isPassword)

fun ArkUIAccessibilityElementInfo.setItemCount(count: Int) =
    OH_ArkUI_AccessibilityElementInfoSetItemCount(this, count)

fun ArkUIAccessibilityElementInfo.setLongClickable(longClickable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetLongClickable(this, longClickable)

fun ArkUIAccessibilityElementInfo.setOperationActions(
    actions: List<Pair<ArkUI_Accessibility_ActionType, String?>>
) = memScoped {
    val count = actions.size
    val operationActions = allocArray<ArkUI_AccessibleAction>(count) { index ->
        val (action, desc) = actions[index]
        actionType = action
        description = desc.orEmpty().cstr.ptr
    }
    OH_ArkUI_AccessibilityElementInfoSetOperationActions(
        this@setOperationActions, count, operationActions
    )
}

fun ArkUIAccessibilityElementInfo.setParentId(id: Int) =
    OH_ArkUI_AccessibilityElementInfoSetParentId(this, id)

fun ArkUIAccessibilityElementInfo.setRangeInfo(min: Double, max: Double, current: Double) =
    memScoped {
        val rangeInfo = alloc<ArkUI_AccessibleRangeInfo> {
            this.current = current
            this.min = min
            this.max = max
        }
        OH_ArkUI_AccessibilityElementInfoSetRangeInfo(this@setRangeInfo, rangeInfo.ptr)
    }

fun ArkUIAccessibilityElementInfo.setSelected(selected: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetSelected(this, selected)

fun ArkUIAccessibilityElementInfo.setScrollable(scrollable: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetScrollable(this, scrollable)

fun ArkUIAccessibilityElementInfo.setSelectedTextEnd(end: Int) =
    OH_ArkUI_AccessibilityElementInfoSetSelectedTextEnd(this, end)

fun ArkUIAccessibilityElementInfo.setSelectedTextStart(start: Int) =
    OH_ArkUI_AccessibilityElementInfoSetSelectedTextStart(this, start)

fun ArkUIAccessibilityElementInfo.setStartItemIndex(index: Int) =
    OH_ArkUI_AccessibilityElementInfoSetStartItemIndex(this, index)

fun ArkUIAccessibilityElementInfo.setScreenRect(rect: IntRect) = memScoped {
    val cRect = alloc<ArkUI_AccessibleRect> {
        leftTopX = rect.left
        leftTopY = rect.top
        rightBottomX = rect.right
        rightBottomY = rect.bottom
    }
    OH_ArkUI_AccessibilityElementInfoSetScreenRect(this@setScreenRect, cRect.ptr)
}

fun ArkUIAccessibilityElementInfo.setVisible(visible: Boolean) =
    OH_ArkUI_AccessibilityElementInfoSetVisible(this, visible)

fun ArkUIAccessibilityElementInfo.setZIndex(index: Int) =
    OH_ArkUI_AccessibilityElementInfoSetZIndex(this, index)
