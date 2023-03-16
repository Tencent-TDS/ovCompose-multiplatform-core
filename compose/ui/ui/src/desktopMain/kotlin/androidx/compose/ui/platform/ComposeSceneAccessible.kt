/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.platform.ComposeSceneAccessible.ComposeSceneAccessibleContext
import java.awt.Color
import java.awt.Cursor
import java.awt.Dimension
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.FocusListener
import java.util.Locale
import javax.accessibility.Accessible
import javax.accessibility.AccessibleComponent
import javax.accessibility.AccessibleContext
import javax.accessibility.AccessibleRole
import javax.accessibility.AccessibleStateSet

/**
 * This is a root [Accessible] for a [androidx.compose.ui.ComposeScene]
 *
 * It provides [AccessibleContext] to Swing accessibility support.
 * This context has no parents and provides [ComposeAccessible] as its children.
 *
 * The main purpose of this class is to support screen readers that read text under the mouse (not only, but mostly).
 * To support it [accessibleContext] provides custom [ComposeSceneAccessibleContext.getAccessibleAt] implementation.
 *
 * Note about a11y for focus-based tools (e.g. VoiceOver).
 * Now focus-based tools are supported on [org.jetbrains.skiko.HardwareLayer] side.
 * When Compose's [androidx.compose.ui.semantics.SemanticsNode] is focused
 * [AccessibilityControllerImpl.onFocusRequested] is called and
 * [org.jetbrains.skiko.HardwareLayer] provides mapped [ComposeAccessible] to accessibility tool.
 *
 * @param rootsProvider provider of all the skia roots such as main window, popups etc.
 * @param mainRootProvider provider of a main skia root
 * those size, bounds, location are used for [androidx.compose.ui.ComposeScene] accessibility
 *
 * @see AccessibilityControllerImpl
 * @see ComposeAccessible
 */
internal class ComposeSceneAccessible(
    private val rootsProvider: () -> List<SkiaBasedOwner>,
    private val mainRootProvider: () -> SkiaBasedOwner?
) : Accessible {
    private val a11yDisabled by lazy {
        System.getProperty("compose.accessibility.enable") == "false" ||
            System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") != null
    }

    private val accessibleContext by lazy {
        ComposeSceneAccessibleContext(rootsProvider, mainRootProvider)
    }

    override fun getAccessibleContext(): AccessibleContext? {
        if (a11yDisabled) {
            return null
        }
        return accessibleContext
    }

    private class ComposeSceneAccessibleContext(
        private val allOwners: () -> List<SkiaBasedOwner>,
        private val mainOwner: () -> SkiaBasedOwner?,
    ) : AccessibleContext(), AccessibleComponent {
        private fun getMainOwnerAccessibleRoot(): ComposeAccessible? {
            return (mainOwner()?.accessibilityController as? AccessibilityControllerImpl)?.rootAccessible
        }

        /**
         * This function is used by Swing accessibility support to get accessible under a [Point]
         * For example, it is used by screen reader to read text under a cursor.
         *
         * To support that [ComposeSceneAccessibleContext] goes through all skia roots in a [androidx.compose.ui.ComposeScene]
         * and finds the best [Accessible] under the pointer.
         */
        override fun getAccessibleAt(p: Point): Accessible? {
            val controllers = allOwners()
                .map { it.accessibilityController }
                .filterIsInstance<AccessibilityControllerImpl>()
            for (controller in controllers.reversed()) {
                val rootAccessible = controller.rootAccessible
                val context =
                    rootAccessible.getAccessibleContext() as? AccessibleComponent
                        ?: continue
                val accessibleOnPoint = context.getAccessibleAt(p) ?: continue
                if (accessibleOnPoint != rootAccessible) {
                    // TODO: ^ this check produce weird behavior
                    //  when there is a component under the popup,
                    //  and this component will be read by screen reader
                    //  but this check is needed since rootAccessible has full width in [getSize]
                    //  when it will be fixed, check can be removed and better results will be produced
                    return accessibleOnPoint
                }
            }

            return null
        }

        override fun contains(p: Point): Boolean = true

        override fun getAccessibleIndexInParent(): Int {
            return -1
        }

        override fun getAccessibleChildrenCount(): Int {
            return allOwners().size
        }

        override fun getAccessibleChild(i: Int): Accessible {
            return (allOwners()[i].accessibilityController as AccessibilityControllerImpl).rootAccessible
        }

        override fun getSize(): Dimension? {
            return getMainOwnerAccessibleRoot()?.accessibleContext?.size
        }

        override fun getLocationOnScreen(): Point? {
            return getMainOwnerAccessibleRoot()?.accessibleContext?.locationOnScreen
        }

        override fun getLocation(): Point? {
            return getMainOwnerAccessibleRoot()?.accessibleContext?.location
        }

        override fun getBounds(): Rectangle? {
            return getMainOwnerAccessibleRoot()?.accessibleContext?.bounds
        }

        override fun isShowing(): Boolean = true

        override fun isFocusTraversable(): Boolean = true

        override fun getAccessibleParent(): Accessible? {
            return null
        }

        override fun getAccessibleComponent(): AccessibleComponent {
            return this
        }

        override fun getLocale(): Locale = Locale.getDefault()

        override fun isVisible(): Boolean = true

        override fun isEnabled(): Boolean = true

        override fun requestFocus() {
        }

        override fun getAccessibleRole(): AccessibleRole {
            return AccessibleRole.PANEL
        }

        override fun getAccessibleStateSet(): AccessibleStateSet {
            return AccessibleStateSet()
        }

        // ---------------------------
        // NOT IMPLEMENTED

        override fun setLocation(p: Point?) {
            // NOT IMPLEMENTED
        }

        override fun setBounds(r: Rectangle?) {
            // NOT IMPLEMENTED
        }

        override fun setSize(d: Dimension?) {
            // NOT IMPLEMENTED
        }

        override fun setVisible(b: Boolean) {
            // NOT IMPLEMENTED
        }

        override fun getBackground(): Color? {
            return null
        }

        override fun setBackground(c: Color?) {
            // NOT IMPLEMENTED
        }

        override fun getForeground(): Color? {
            return null
        }

        override fun setForeground(c: Color?) {
            // NOT IMPLEMENTED
        }

        override fun getCursor(): Cursor? {
            return null
        }

        override fun setCursor(cursor: Cursor?) {
            // NOT IMPLEMENTED
        }

        override fun getFont(): Font? {
            return null
        }

        override fun setFont(f: Font?) {
            // NOT IMPLEMENTED
        }

        override fun getFontMetrics(f: Font?): FontMetrics? {
            return null
        }

        override fun setEnabled(b: Boolean) {
            // NOT IMPLEMENTED
        }

        override fun addFocusListener(l: FocusListener?) {
            // NOT IMPLEMENTED
        }

        override fun removeFocusListener(l: FocusListener?) {
            // NOT IMPLEMENTED
        }
    }
}