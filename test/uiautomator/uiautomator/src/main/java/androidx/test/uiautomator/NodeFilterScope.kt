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

package androidx.test.uiautomator

import android.view.accessibility.AccessibilityNodeInfo

/**
 * A filter scope that allows defining conditions on an accessibility node, that need to be
 * satisfied when calling [androidx.test.uiautomator.onView] and
 * [androidx.test.uiautomator.onViews].
 *
 * Internally it allows accessing directly to [android.view.accessibility.AccessibilityNodeInfo] and
 * some convenience properties like [depth] or the view [index].
 */
public class NodeFilterScope
internal constructor(

    /** The accessibility node to filter. */
    public val view: AccessibilityNodeInfo,

    /** The depth of the node in the accessibility tree, where 0 is the root. */
    public val depth: Int,

    /** An index for the node calculated sequentially counting the nodes. */
    public val index: Int,
    // This is used to avoid recalculating the children if the user looks for the children.
    internal val lazyChildren: () -> List<AccessibilityNodeInfo>,
) {

    /** Returns the list of visible children of this node. */
    public val children: List<AccessibilityNodeInfo> by lazy { lazyChildren() }

    // Simplifies some properties for easier syntax and access.

    /**
     * Returns this node's id without the full resource namespace, i.e. only the portion after the
     * "/" character.
     */
    public val AccessibilityNodeInfo.id: String?
        get() = viewIdResourceName?.substringAfter("/")

    /**
     * Returns this node's text as a string. This should always be preferred to
     * [AccessibilityNodeInfo.text] that instead returns a [CharSequence], that might be either a
     * [String] or a [android.text.SpannableString].
     *
     * Usage:
     * ```kotlin
     * onView { view.textAsString == "Some text" }.click()
     * ```
     */
    public val AccessibilityNodeInfo.textAsString: String?
        get() = text?.toString()
}
