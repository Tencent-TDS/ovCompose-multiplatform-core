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

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.roundToIntRect
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CLICK
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_COPY
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CUT
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_LONG_CLICK
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_PASTE
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SET_TEXT
import platform.arkui.ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_RECURSIVE_CHILDREN
import platform.arkui.ArkUI_AccessibilitySearchMode
import platform.arkui.ArkUI_Accessibility_ActionType
import platform.arkui.OH_ArkUI_AddAndGetAccessibilityElementInfo

internal class AccessibilityMediator(private val semanticsOwner: SemanticsOwner) {

    private var currentSemanticsNodesInvalidated = true

    private var currentSemanticsNodes = mapOf<Int, SemanticsNodeWithAdjustedBounds>()
        get() {
            if (currentSemanticsNodesInvalidated) {
                currentSemanticsNodesInvalidated = false
                field = semanticsOwner.getAllHierarchicalTraversalSemanticsNodesToMap()
            }
            return field
        }

    operator fun contains(elementId: Int): Boolean = elementId in currentSemanticsNodes

    fun findAccessibilityNodeInfosById(
        elementId: Int,
        mode: ArkUI_AccessibilitySearchMode,
        requestId: Int,
        elementList: ArkUIAccessibilityElementInfoList
    ) {
        if (mode == ARKUI_ACCESSIBILITY_NATIVE_SEARCH_MODE_PREFETCH_RECURSIVE_CHILDREN) {
            currentSemanticsNodes.values.forEach { node ->
                createNodeInfo(
                    virtualViewId = node.semanticsNode.id,
                    info = OH_ArkUI_AddAndGetAccessibilityElementInfo(elementList)
                )
            }
        } else {
            val id = if (elementId > 0) elementId else semanticsOwner.rootSemanticsNode.id
            createNodeInfo(
                virtualViewId = id,
                info = OH_ArkUI_AddAndGetAccessibilityElementInfo(elementList)
            )
        }
    }

    private fun createNodeInfo(virtualViewId: Int, info: ArkUIAccessibilityElementInfo) {
        val semanticsNodeWithAdjustedBounds = currentSemanticsNodes[virtualViewId] ?: return
        val semanticsNode: SemanticsNode = semanticsNodeWithAdjustedBounds.semanticsNode
        val parentId = if (virtualViewId == semanticsOwner.rootSemanticsNode.id) {
            HOST_VIEW_PARENT_ID
        } else {
            requireNotNull(semanticsNode.parent?.id) {
                "semanticsNode $virtualViewId has null parent"
            }
        }

        info.setParentId(parentId)
        info.setElementId(virtualViewId)
        info.setScreenRect(semanticsNodeWithAdjustedBounds.adjustedBounds.roundToIntRect())

        semanticsNode.populateArkUIAccessibilityElementInfo(info)
    }

    private fun SemanticsNode.populateArkUIAccessibilityElementInfo(
        info: ArkUIAccessibilityElementInfo,
    ) {
        val actions = mutableListOf<Pair<ArkUI_Accessibility_ActionType, String?>>()
        val unmergedConfig = unmergedConfig
        val replacedChildren = replacedChildren
        val type = when {
            SemanticsProperties.EditableText in unmergedConfig ||
                SemanticsActions.SetText in unmergedConfig -> TextFieldComponentName

            SemanticsProperties.Text in unmergedConfig -> TextComponentName
            else -> ComponentName
        }
        info.setComponentType(type)

        val role = unmergedConfig.getOrNull(SemanticsProperties.Role)
        if (role != null) {
            if (isFake || replacedChildren.isEmpty()) {
                val componentName = role.toString()
                // Images are often minor children of larger widgets, so we only want to
                // announce the Image role when the image itself is focusable.
                if (role != Role.Image || isUnmergedLeafNode || unmergedConfig.isMergingSemanticsOfDescendants) {
                    info.setComponentType(componentName)
                }
            }
        }

        info.setAccessibilityOpacity(1f)
        info.setAccessibilityLevel("yes")

        val children = replacedChildren
        val childIds = LongArray(children.size) { index ->
            children[index].id.toLong()
        }

        info.setChildNodeIds(children.size, childIds)

        val text = text
        info.setAccessibilityText(text)
        info.setContents(text)
        info.setCheckable(isCheckable)

        val toggleState = unmergedConfig.getOrNull(SemanticsProperties.ToggleableState)

        info.setChecked(toggleState == ToggleableState.On)

        unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
            if (role == Role.Tab) {
                info.setSelected(it)
            } else {
                info.setChecked(it)
            }
        }

        if (!unmergedConfig.isMergingSemanticsOfDescendants || replacedChildren.isEmpty()) {
            val contentDescription =
                unmergedConfig.getOrNull(SemanticsProperties.ContentDescription)?.firstOrNull()
            info.setAccessibilityDescription(contentDescription)
        }

        info.setIsPassword(SemanticsProperties.Password in unmergedConfig)
        info.setEditable(SemanticsProperties.EditableText in unmergedConfig)

        val isEnabled = isEnabled
        info.setEnabled(isEnabled)

        val focusable = SemanticsProperties.Focused in unmergedConfig
        info.setFocusable(focusable)

        val isFocused = focusable && unmergedConfig[SemanticsProperties.Focused]
        info.setFocused(focusable && isFocused)

        info.setVisible(isVisible)

        unmergedConfig.getOrNull(SemanticsActions.OnClick)?.let {
            // Selectable tabs and radio buttons that are already selected cannot be selected again
            // so they should not be exposed as clickable.
            val isSelected = unmergedConfig.getOrNull(SemanticsProperties.Selected) == true
            val isRadioButtonOrTab = role == Role.Tab || role == Role.RadioButton
            val isClickable = !isRadioButtonOrTab || (isRadioButtonOrTab && !isSelected)
            info.setClickable(isClickable)
            if (isEnabled && isClickable) {
                actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CLICK to it.label)
            }
        }

        unmergedConfig.getOrNull(SemanticsActions.OnLongClick)?.let {
            info.setLongClickable(true)
            if (isEnabled) {
                actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_LONG_CLICK to it.label)
            }
        }

        // The config will contain this action only if there is a text selection at the moment.
        unmergedConfig.getOrNull(SemanticsActions.CopyText)?.let {
            actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_COPY to it.label)
        }

        if (isEnabled) {
            unmergedConfig.getOrNull(SemanticsActions.SetText)?.let {
                actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_SET_TEXT to it.label)
            }

            // The config will contain this action only if there is a text selection at the moment.
            unmergedConfig.getOrNull(SemanticsActions.CutText)?.let {
                actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_CUT to it.label)
            }

            // The config will contain the action anyway, therefore we check the clipboard text to
            // decide whether to add the action to the node or not.
            unmergedConfig.getOrNull(SemanticsActions.PasteText)?.let {
                if (isFocused) {
                    actions.add(ARKUI_ACCESSIBILITY_NATIVE_ACTION_TYPE_PASTE to it.label)
                }
            }
        }

        info.setOperationActions(actions)
    }

    internal fun onSemanticsChange() {
        currentSemanticsNodesInvalidated = true
    }

    companion object {
        private const val HOST_VIEW_PARENT_ID = -2100000

        private const val ComponentName = "CustomFrameNode"
        private const val TextFieldComponentName = "TextInput"
        private const val TextComponentName = "Text"

        private val SemanticsNode.isEnabled get() = !config.contains(SemanticsProperties.Disabled)

        private val SemanticsNode.isVisible: Boolean
            get() = !isTransparent && !unmergedConfig.contains(SemanticsProperties.InvisibleToUser)

        private val SemanticsNode.isCheckable: Boolean
            get() {
                var isCheckable = false
                val toggleState = unmergedConfig.getOrNull(SemanticsProperties.ToggleableState)
                val role = unmergedConfig.getOrNull(SemanticsProperties.Role)
                toggleState?.let { isCheckable = true }
                unmergedConfig.getOrNull(SemanticsProperties.Selected)?.let {
                    if (role != Role.Tab) isCheckable = true
                }
                return isCheckable
            }

        private val SemanticsNode.text: String?
            get() = unmergedConfig.getOrNull(SemanticsProperties.EditableText)?.text
                ?: unmergedConfig.getOrNull(SemanticsProperties.Text)?.firstOrNull()?.text
    }
}