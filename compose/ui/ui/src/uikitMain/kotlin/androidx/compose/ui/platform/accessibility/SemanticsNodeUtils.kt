/*
 * Copyright 2024 The Android Open Source Project
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

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.Strings
import androidx.compose.ui.platform.getString
import androidx.compose.ui.semantics.AccessibilityAction
import androidx.compose.ui.semantics.ScrollAxisRange
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.SemanticsProperties.HideFromAccessibility
import androidx.compose.ui.semantics.SemanticsProperties.InvisibleToUser
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.toSize
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityScrollDirectionDown
import platform.UIKit.UIAccessibilityScrollDirectionLeft
import platform.UIKit.UIAccessibilityScrollDirectionRight
import platform.UIKit.UIAccessibilityScrollDirectionUp


internal fun SemanticsNode.scrollIfPossible(
    direction: UIAccessibilityScrollDirection,
): AccessibilityScrollEventResult? {
    val config = this.config

    val deltaX: Int
    val deltaY: Int
    val isForward: Boolean
    val pageAction: SemanticsPropertyKey<AccessibilityAction<() -> Boolean>>
    val rangeProperty = if (direction.isHorizontal) {
        SemanticsProperties.HorizontalScrollAxisRange
    } else {
        SemanticsProperties.VerticalScrollAxisRange
    }

    val axisRange = config.getOrNull(rangeProperty)
    val isReverse = axisRange?.reverseScrolling == true
    val normalisedDirection = when (direction) {
        UIAccessibilityScrollDirectionUp -> if (isReverse) {
            UIAccessibilityScrollDirectionDown
        } else {
            UIAccessibilityScrollDirectionUp
        }

        UIAccessibilityScrollDirectionDown -> if (isReverse) {
            UIAccessibilityScrollDirectionUp
        } else {
            UIAccessibilityScrollDirectionDown
        }

        UIAccessibilityScrollDirectionRight -> if (isRTL xor isReverse) {
            UIAccessibilityScrollDirectionLeft
        } else {
            UIAccessibilityScrollDirectionRight
        }

        UIAccessibilityScrollDirectionLeft -> if (isRTL xor isReverse) {
            UIAccessibilityScrollDirectionRight
        } else {
            UIAccessibilityScrollDirectionLeft
        }

        else -> return null
    }

    when (normalisedDirection) {
        UIAccessibilityScrollDirectionUp -> {
            deltaX = 0
            deltaY = -size.height
            isForward = false
            pageAction = SemanticsActions.PageUp
        }

        UIAccessibilityScrollDirectionDown -> {
            deltaX = 0
            deltaY = size.height
            isForward = true
            pageAction = SemanticsActions.PageDown
        }

        UIAccessibilityScrollDirectionRight -> {
            deltaX = -size.width
            deltaY = 0
            isForward = false
            pageAction = SemanticsActions.PageLeft
        }

        UIAccessibilityScrollDirectionLeft -> {
            deltaX = size.width
            deltaY = 0
            isForward = true
            pageAction = SemanticsActions.PageRight
        }

        else -> return null
    }

    val succeeded = config.getOrNull(pageAction)?.action?.invoke()
        ?: config.getOrNull(SemanticsActions.ScrollBy)
            ?.action
            ?.invoke(deltaX.toFloat(), deltaY.toFloat())

    return when (succeeded) {
        true -> AccessibilityScrollEventResult(
            announceMessage = {
                announceMessage(isForward, config.getOrNull(rangeProperty))
            }
        )

        false -> null
        null -> parent?.scrollIfPossible(direction)
    }
}

private fun announceMessage(isForward: Boolean, range: ScrollAxisRange?): String? {
    range ?: return null
    return if (range.value() == 0f) {
        getString(Strings.FirstPage)
    } else if (range.value() == range.maxValue()) {
        getString(Strings.LastPage)
    } else if (isForward) {
        getString(Strings.NextPage)
    } else {
        getString(Strings.PreviousPage)
    }
}

internal data class AccessibilityScrollEventResult(
    val announceMessage: () -> String?,
)

/**
 * Try to perform a scroll on any ancestor of this element if the element is not fully visible.
 * @param rect to place in the center of scrollable area
 * @param safeAreaRectInWindow safe area rect to reduce focusable borders
 * @return true if the scroll was successful, otherwise returns false
 */
internal fun SemanticsNode.scrollToCenterRectIfNeeded(
    rect: Rect,
    safeAreaRectInWindow: Rect
): Boolean {
    val scrollableAncestor = scrollableByAncestor ?: return false
    val scrollableAncestorRect = scrollableAncestor.boundsInWindow
    val scrollableViewportRect = scrollableAncestorRect.intersect(safeAreaRectInWindow)

    fun Float.invertIfNeeded() = if (isRTL) -this else this

    val dy = if (rect.top < scrollableViewportRect.top) {
        // The element is above the screen, scroll up
        rect.top - scrollableViewportRect.top -
            (scrollableAncestor.size.height - rect.size.height) / 2
    } else if (rect.bottom > scrollableViewportRect.bottom) {
        // The element is below the screen, scroll down
        rect.bottom - scrollableViewportRect.bottom +
            (scrollableAncestor.size.height - rect.size.height) / 2
    } else {
        0f
    }

    val dx = if (rect.left < scrollableViewportRect.left) {
        // The element is to the left of the screen, scroll left
        (rect.left - scrollableViewportRect.left -
            (scrollableAncestor.size.width - rect.size.width) / 2).invertIfNeeded()
    } else if (rect.right > scrollableViewportRect.right) {
        // The element is to the right of the screen, scroll right
        (rect.right - scrollableViewportRect.right +
            (scrollableAncestor.size.width - rect.size.width) / 2).invertIfNeeded()
    } else {
        0f
    }
    return scrollByIfPossible(dx, dy)
}

private fun SemanticsNode.scrollByIfPossible(dx: Float, dy: Float): Boolean {
    // if it has scrollBy action, invoke it, otherwise try to scroll the parent
    val action = config.getOrNull(SemanticsActions.ScrollBy)?.action

    return if (action != null) {
        action(dx, dy)
    } else {
        parent?.scrollByIfPossible(dx, dy) ?: false
    }
}

internal val SemanticsNode.unclippedBoundsInWindow: Rect
    get() = Rect(positionInWindow, size.toSize())

internal val SemanticsNode.isRTL: Boolean
    get() = layoutInfo.layoutDirection == LayoutDirection.Rtl

// Simplified version of the isScreenReaderFocusable() from the
// AndroidComposeViewAccessibilityDelegateCompat.android.kt
internal fun SemanticsNode.isScreenReaderFocusable(): Boolean {
    return !isTransparent && canBeAccessibilityElement()
}

internal fun SemanticsNode.canBeAccessibilityElement(): Boolean {
    return !isHiddenFromAccessibility &&
        (unmergedConfig.isMergingSemanticsOfDescendants ||
            isUnmergedLeafNode && isSpeakingNode)
}

private val SemanticsNode.isSpeakingNode: Boolean get() {
    return unmergedConfig.contains(SemanticsProperties.ContentDescription) ||
        unmergedConfig.contains(SemanticsProperties.EditableText) ||
        unmergedConfig.contains(SemanticsProperties.Text) ||
        unmergedConfig.contains(SemanticsProperties.StateDescription) ||
        unmergedConfig.contains(SemanticsProperties.ToggleableState) ||
        unmergedConfig.contains(SemanticsProperties.Selected) ||
        unmergedConfig.contains(SemanticsProperties.ProgressBarRangeInfo)
}

@Suppress("DEPRECATION")
private val SemanticsNode.isHiddenFromAccessibility: Boolean
    get() = unmergedConfig.contains(HideFromAccessibility) ||
        unmergedConfig.contains(InvisibleToUser)

/**
 * Closest ancestor that has [SemanticsActions.ScrollBy] action
 */
private val SemanticsNode.scrollableByAncestor: SemanticsNode?
    get() {
        var current: SemanticsNode? = this

        while (current != null) {
            if (current.config.getOrNull(SemanticsActions.ScrollBy) != null) {
                return current
            }

            current = current.parent
        }

        return null
    }

private val UIAccessibilityScrollDirection.isHorizontal get() =
    this == UIAccessibilityScrollDirectionRight || this == UIAccessibilityScrollDirectionLeft

