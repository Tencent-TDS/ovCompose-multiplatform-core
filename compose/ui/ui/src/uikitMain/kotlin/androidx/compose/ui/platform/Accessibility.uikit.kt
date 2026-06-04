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

import androidx.compose.runtime.ComposeTabService
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.platformSynchronizedObject
import androidx.compose.runtime.synchronized
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.interop.NativeAccessibilityViewSemanticsKey
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.accessibility.AccessibilityScrollEventResult
import androidx.compose.ui.platform.accessibility.accessibilityCustomActions
import androidx.compose.ui.platform.accessibility.accessibilityLabel
import androidx.compose.ui.platform.accessibility.accessibilityTraits
import androidx.compose.ui.platform.accessibility.accessibilityValue
import androidx.compose.ui.platform.accessibility.allScrollableParentNodeIds
import androidx.compose.ui.platform.accessibility.isRTL
import androidx.compose.ui.platform.accessibility.isScreenReaderFocusable
import androidx.compose.ui.platform.accessibility.scrollIfPossible
import androidx.compose.ui.platform.accessibility.scrollToCenterRectIfNeeded
import androidx.compose.ui.platform.accessibility.unclippedBoundsInWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getAllUncoveredSemanticsNodesToIntObjectMap
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.semantics.isHidden
import androidx.compose.ui.semantics.sortByGeometryGroupings
import androidx.compose.ui.uikit.utils.CMPAccessibilityElement
import androidx.compose.ui.uikit.utils.OVMPAccessibilityElement
import androidx.compose.ui.uikit.utils.OVMPAccessibilityElementProxy
import androidx.compose.ui.uikit.utils.OVMPAccessibilityElementSyncType
import androidx.compose.ui.uikit.utils.OVMPAccessibilityElementsCache
import androidx.compose.ui.uikit.utils.OVMPAccessibilityRootElement
import androidx.compose.ui.uikit.utils.TMMInteropWrapView
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.window.DisplayLinkListener
import androidx.compose.ui.window.uiContentSizeCategoryToFontScaleMap
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.time.measureTime
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.readValue
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectEqualToRect
import platform.CoreGraphics.CGRectGetMaxX
import platform.CoreGraphics.CGRectGetMaxY
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectIntersectsRect
import platform.CoreGraphics.CGRectIsEmpty
import platform.CoreGraphics.CGRectZero
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityContainerType
import platform.UIKit.UIAccessibilityContainerTypeNone
import platform.UIKit.UIAccessibilityContainerTypeSemanticGroup
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityElement
import platform.UIKit.UIAccessibilityFocusedElement
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPageScrolledNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIContentSizeCategoryUnspecified
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEdgeInsetsInsetRect
import platform.UIKit.UIFocusAnimationCoordinator
import platform.UIKit.UIFocusEnvironmentProtocol
import platform.UIKit.UIFocusItemContainerProtocol
import platform.UIKit.UIFocusItemProtocol
import platform.UIKit.UIFocusSystem
import platform.UIKit.UIFocusUpdateContext
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIScreen
import platform.UIKit.UIView
import platform.UIKit.UIWindow
import platform.UIKit.accessibilityElementAtIndex
import platform.UIKit.accessibilityElementCount
import platform.UIKit.accessibilityElements
import platform.UIKit.accessibilityFrame
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setAccessibilityElements
import platform.darwin.DISPATCH_TIME_NOW
import platform.darwin.NSEC_PER_SEC
import platform.darwin.NSInteger
import platform.darwin.NSObject
import platform.darwin.dispatch_after
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import platform.darwin.dispatch_time

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()

internal sealed interface AccessibilityElementKey {
    val id: Int

    data class Semantics(override val id: Int) : AccessibilityElementKey
    data class Container(override val id: Int) : AccessibilityElementKey
}

internal val UIView.density: Density
    get() {
        // TODO: It's a code smell that we have to retrive a default UIScreen here.
        //   We probably should reorder the code so that density is either injected from outside
        //   or view is attached to a window before this is called.
        val screen = if (this is UIWindow) {
            screen
        } else {
            window?.screen ?: UIScreen.mainScreen
        }

        val contentSizeCategory = traitCollection.preferredContentSizeCategory ?: UIContentSizeCategoryUnspecified

        return Density(
            density = screen.scale.toFloat(),
            fontScale = uiContentSizeCategoryToFontScaleMap[contentSizeCategory] ?: 1.0f
        )
    }


/**
 * Sealed interface that represents behavior of actual accessibility element.
 */
private sealed interface AccessibilityNode {
    val key: AccessibilityElementKey
    val isAccessibilityElement: Boolean
    val semanticsNode: SemanticsNode

    val accessibilityLabel: String? get() = null
    val accessibilityHint: String? get() = null
    val accessibilityValue: String? get() = null
    val accessibilityTraits: UIAccessibilityTraits get() = UIAccessibilityTraitNone
    val accessibilityContainerType: UIAccessibilityContainerType
        get() = UIAccessibilityContainerTypeNone
    val accessibilityIdentifier: String? get() = null
    val accessibilityInteropView: TMMInteropWrapView? get() = null
    val accessibilityCustomActions: List<UIAccessibilityCustomAction> get() = emptyList()

    fun accessibilityActivate(): Boolean = false
    fun accessibilityIncrement() {}
    fun accessibilityDecrement() {}
    fun accessibilityElementDidBecomeFocused() {}
    fun accessibilityElementDidLoseFocus() {}
    fun accessibilityScrollToVisible(): Boolean = false
    fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean = false
    fun accessibilityPerformEscape(): Boolean = false

    // Focus API
    val canBecomeFocused: Boolean get() = false
    fun didBecomeFocused() {}
    fun didResignFocused() {}

    /**
     * Represents a projection of the Compose semantics node to the iOS world.
     * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode].
     * @semanticsNode node associated with current accessibility element
     * @mediator reference to the containing AccessibilityMediator
     */
    class Semantics(
        override val semanticsNode: SemanticsNode,
        private val mediator: AccessibilityMediator,
        private val isBeyondBounds: Boolean
    ) : AccessibilityNode {
        private val cachedConfig = semanticsNode.copyWithMergingEnabled().config

        override val key: AccessibilityElementKey get() = semanticsNode.semanticsKey

        override val isAccessibilityElement: Boolean get() {
            if (!semanticsNode.isScreenReaderFocusable()) {
                return false
            }

            if (isBeyondBounds) {
                // Semantics node is outside visible bounds.
                // Check if it can be focused by scrolling.
                return semanticsNode.allScrollableParentNodeIds.any {
                    mediator.focusedNodesScrollableParentsIds.contains(it)
                }
            }

            return true
        }

        override val accessibilityInteropView: TMMInteropWrapView?
            get() = cachedConfig.getOrNull(NativeAccessibilityViewSemanticsKey)

        override val accessibilityLabel: String?
            get() = cachedConfig.accessibilityLabel()

        override val accessibilityIdentifier: String?
            get() = cachedConfig.getOrNull(SemanticsProperties.TestTag)

        override val accessibilityHint: String?
            get() = cachedConfig.getOrNull(SemanticsActions.OnClick)?.label

        override val accessibilityCustomActions: List<UIAccessibilityCustomAction>
            get() = cachedConfig.accessibilityCustomActions()

        override val accessibilityTraits: UIAccessibilityTraits
            get() = cachedConfig.accessibilityTraits()

        override val accessibilityValue: String?
            get() = cachedConfig.accessibilityValue()

        override fun accessibilityActivate(): Boolean {
            if (!semanticsNode.isValid) {
                return false
            }

            val config = cachedConfig

            if (config.contains(SemanticsProperties.Disabled)) {
                return false
            }

            val onClick = config.getOrNull(SemanticsActions.OnClick) ?: return false
            val action = onClick.action ?: return false

            return action()
        }

        override fun accessibilityIncrement() =
            updateProgress(increment = true)

        override fun accessibilityDecrement() =
            updateProgress(increment = false)

        private fun updateProgress(increment: Boolean) {
            val progress =
                cachedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo) ?: return
            val setProgress = cachedConfig.getOrNull(SemanticsActions.SetProgress) ?: return
            val step = (progress.range.endInclusive - progress.range.start) / progress.steps
            val value = progress.current + if (increment) step else -step
            setProgress.action?.invoke(value)
        }

        override fun accessibilityElementDidBecomeFocused() {
            accessibilityDebugLogger?.apply {
                log(null)
                log { "Focused on:" }
                log { cachedConfig }
            }
            mediator.setFocusTarget(key)
        }

        override fun accessibilityElementDidLoseFocus() {
            mediator.clearFocusTargetIfNeeded(key)
        }

        private var listener: DisplayLinkListener? = null
        override fun accessibilityScrollToVisible(): Boolean {
            if (listener != null) {
                return false
            }
            val listener = DisplayLinkListener()
            listener.start()
            this.listener = listener
            CoroutineScope(mediator.coroutineContext + listener.frameClock).launch {
                semanticsNode.parent?.scrollToCenterRectIfNeeded(
                    targetRect = semanticsNode.unclippedBoundsInWindow,
                    safeAreaRectInWindow = mediator.safeAreaRectInWindow
                )
                listener.invalidate()
                this@Semantics.listener = null
            }

            return true
        }

        override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
            if (cachedConfig.contains(SemanticsProperties.Disabled)) {
                return false
            }

            val frame = semanticsNode.boundsInWindow
            val approximateScrollAnimationDuration = 350L

            val result = semanticsNode.scrollIfPossible(direction)
            return if (result != null) {
                mediator.clearFocusTargetIfNeeded(key)
                mediator.notifyScrollCompleted(
                    scrollResult = result,
                    delay = approximateScrollAnimationDuration,
                    focusedNode = semanticsNode,
                    focusedRectInWindow = frame
                )
                true
            } else {
                false
            }
        }

        override fun accessibilityPerformEscape(): Boolean {
            if (mediator.performEscape()) {
                UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
                return true
            } else {
                return false
            }
        }

        override val canBecomeFocused: Boolean
            get() = semanticsNode.unmergedConfig.contains(SemanticsProperties.Focused)

        override fun didBecomeFocused() {
            accessibilityScrollToVisible()
            mediator.keyboardFocusedElementKey = key
        }

        override fun didResignFocused() {
            if (mediator.keyboardFocusedElementKey == key) {
                mediator.keyboardFocusedElementKey = null
            }
        }
    }


    class Semantics2(
        override val semanticsNode: SemanticsNode,
        private val mediator: AccessibilityMediator2,
        private val isBeyondBounds: Boolean
    ) : AccessibilityNode
    {
        private val cachedConfig = semanticsNode.copyWithMergingEnabled().config

        override val key: AccessibilityElementKey get() = semanticsNode.semanticsKey

        override val isAccessibilityElement: Boolean get() {
            if (!semanticsNode.isScreenReaderFocusable()) {
                return false
            }

            if (isBeyondBounds) {
                // Semantics node is outside visible bounds.
                // Check if it can be focused by scrolling.
                return semanticsNode.allScrollableParentNodeIds.any {
                    mediator.focusedNodesScrollableParentsIds.contains(it)
                }
            }

            return true
        }

        override val accessibilityInteropView: TMMInteropWrapView?
            get() = cachedConfig.getOrNull(NativeAccessibilityViewSemanticsKey)

        override val accessibilityLabel: String?
            get() = cachedConfig.accessibilityLabel()

        override val accessibilityIdentifier: String?
            get() = cachedConfig.getOrNull(SemanticsProperties.TestTag)

        override val accessibilityHint: String?
            get() = cachedConfig.getOrNull(SemanticsActions.OnClick)?.label

        override val accessibilityCustomActions: List<UIAccessibilityCustomAction>
            get() = cachedConfig.accessibilityCustomActions()

        override val accessibilityTraits: UIAccessibilityTraits
            get() = cachedConfig.accessibilityTraits()

        override val accessibilityValue: String?
            get() = cachedConfig.accessibilityValue()

        override fun accessibilityActivate(): Boolean {
            if (!semanticsNode.isValid) {
                return false
            }

            val config = cachedConfig

            if (config.contains(SemanticsProperties.Disabled)) {
                return false
            }

            val onClick = config.getOrNull(SemanticsActions.OnClick) ?: return false
            val action = onClick.action ?: return false

            return action()
        }

        override fun accessibilityIncrement() =
            updateProgress(increment = true)

        override fun accessibilityDecrement() =
            updateProgress(increment = false)

        private fun updateProgress(increment: Boolean) {
            val progress =
                cachedConfig.getOrNull(SemanticsProperties.ProgressBarRangeInfo) ?: return
            val setProgress = cachedConfig.getOrNull(SemanticsActions.SetProgress) ?: return
            val step = (progress.range.endInclusive - progress.range.start) / progress.steps
            val value = progress.current + if (increment) step else -step
            setProgress.action?.invoke(value)
        }

        override fun accessibilityElementDidBecomeFocused() {
            accessibilityDebugLogger?.apply {
                log(null)
                log { "Focused on:" }
                log { cachedConfig }
            }
            mediator.setFocusTarget(key)
        }

        override fun accessibilityElementDidLoseFocus() {
            mediator.clearFocusTargetIfNeeded(key)
        }

        private var listener: DisplayLinkListener? = null
        override fun accessibilityScrollToVisible(): Boolean {
            if (listener != null) {
                return false
            }
            val listener = DisplayLinkListener()
            listener.start()
            this.listener = listener
            CoroutineScope(mediator.coroutineContext + listener.frameClock).launch {
                // region Tencent Code Modify
                /*
                * semanticsNode.parent?.scrollToCenterRectIfNeeded(
                        targetRect = semanticsNode.unclippedBoundsInWindow,
                        safeAreaRectInWindow = mediator.safeAreaRectInWindow
                    )
                */
                if (semanticsNode.layoutNode.isAttached) {
                    semanticsNode.parent?.scrollToCenterRectIfNeeded(
                        targetRect = semanticsNode.unclippedBoundsInWindow,
                        safeAreaRectInWindow = mediator.safeAreaRectInWindow
                    )
                }
                // end region
                listener.invalidate()
                this@Semantics2.listener = null
            }

            return true
        }

        override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
            if (cachedConfig.contains(SemanticsProperties.Disabled)) {
                return false
            }

            val frame = semanticsNode.boundsInWindow
            val approximateScrollAnimationDuration = 350L

            val result = semanticsNode.scrollIfPossible(direction)
            return if (result != null) {
                mediator.clearFocusTargetIfNeeded(key)
                mediator.notifyScrollCompleted(
                    scrollResult = result,
                    delay = approximateScrollAnimationDuration,
                    focusedNode = semanticsNode,
                    focusedRectInWindow = frame
                )
                true
            } else {
                false
            }
        }

        override fun accessibilityPerformEscape(): Boolean {
            if (mediator.performEscape()) {
                UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
                return true
            } else {
                return false
            }
        }

        override val canBecomeFocused: Boolean
            get() = semanticsNode.unmergedConfig.contains(SemanticsProperties.Focused)

        override fun didBecomeFocused() {
            accessibilityScrollToVisible()
            mediator.keyboardFocusedElementKey = key
        }

        override fun didResignFocused() {
            if (mediator.keyboardFocusedElementKey == key) {
                mediator.keyboardFocusedElementKey = null
            }
        }
    }

    /**
     * Unlike Android, UIAccessibilityElement can't be a container and an element at the same time.
     * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
     * UIAccessibilityContainer methods. To implement this behavior, flatting the container node
     * with all its children. [Container] is used to indicate element that contains container
     * semantic node with all its children.
     */
    class Container(
        override val semanticsNode: SemanticsNode
    ) : AccessibilityNode {
        override val key: AccessibilityElementKey = semanticsNode.containerKey

        override val isAccessibilityElement = false

        override val accessibilityContainerType: UIAccessibilityContainerType =
            UIAccessibilityContainerTypeSemanticGroup
    }
}

private class CachedAccessibilityPropertyKey<V>

private object CachedAccessibilityPropertyKeys {
    val accessibilityLabel = CachedAccessibilityPropertyKey<String?>()
    val accessibilityIdentifier = CachedAccessibilityPropertyKey<String?>()
    val accessibilityHint = CachedAccessibilityPropertyKey<String?>()
    val accessibilityCustomActions = CachedAccessibilityPropertyKey<List<UIAccessibilityCustomAction>>()
    val accessibilityTraits = CachedAccessibilityPropertyKey<UIAccessibilityTraits>()
    val accessibilityValue = CachedAccessibilityPropertyKey<String?>()
    val accessibilityElements = CachedAccessibilityPropertyKey<List<Any>>()
}

@OptIn(BetaInteropApi::class)
@ExportObjCClass
private class AccessibilityRoot(
    val mediator: AccessibilityMediator,
    var onKeyboardPresses: (Set<*>) -> Unit = {}
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER),
    UIFocusItemContainerProtocol {
    var element: AccessibilityElement? = null
        set(value) {
            if (field?.accessibilityContainer === this) {
                field?.setAccessibilityContainer(null)
            }
            /* region Tencent Code */
            delayReleaseElement = field
            dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC.toLong()), dispatch_get_main_queue()) {
                delayReleaseElement = null
            }
            /* end region */
            field = value
            field?.setAccessibilityContainer(this)
            mediator.onScreenReaderActive(value != null)
        }

    /* region Tencent Code */
    private var delayReleaseElement: AccessibilityElement? = null
    /* end region */

    override fun isAccessibilityElement(): Boolean = false

    override fun accessibilityElementCount(): NSInteger = if (mediator.isEnabled) 1 else 0

    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        return if (mediator.isEnabled) {
            mediator.activateAccessibilityIfNeeded()
            element
        } else {
            null
        }
    }


    override fun accessibilityContainer() = mediator.view

    override fun accessibilityFrame(): CValue<CGRect> =
        mediator.view.convertRect(mediator.view.bounds, toView = null)

    // UIFocusItemContainerProtocol

    override fun coordinateSpace(): UICoordinateSpaceProtocol = mediator.view

    override fun focusItemsInRect(rect: CValue<CGRect>): List<*> {
        return if (mediator.isEnabled) {
            mediator.activateAccessibilityIfNeeded()
            listOfNotNull(element)
        } else {
            emptyList<Any>()
        }
    }

    override fun pressesBegan(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesBegan(presses, withEvent)
    }

    override fun pressesEnded(presses: Set<*>, withEvent: UIPressesEvent?) {
        onKeyboardPresses(presses)
        super.pressesEnded(presses, withEvent)
    }

    // region Tencent code
    override fun accessibilityElementWithIdentifier(identifier: String): Any? {
        return mediator.accessibilityElementWithIdentifier(identifier)
    }
    // endregion
}

@OptIn(BetaInteropApi::class)
@ExportObjCClass
private class AccessibilityElement(
    var node: AccessibilityNode,
    children: List<AccessibilityElement>
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER),
    UIFocusItemProtocol,
    UIFocusItemContainerProtocol {
    /**
     * A cache for the properties that are computed from the [SemanticsNode.config] and are communicated
     * to iOS Accessibility services.
     */
    private val cachedProperties = mutableMapOf<CachedAccessibilityPropertyKey<*>, Any?>()

    // region Tencent Code
    private val propertiesLock = platformSynchronizedObject()
    // end region

    val key: AccessibilityElementKey get() = node.key

    /**
     * Indicates whether this element is still present in the tree.
     */
    var isAlive = true
        private set

    private var newChildren : List<Any>? = null

    init {
        newChildren = children + nodeSemanticsElements()
        setAccessibilityElements(newChildren)
        children.forEach { it.setAccessibilityContainer(this) }
    }

    private fun nodeSemanticsElements(): List<Any> =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityElements) {
            listOfNotNull(node.accessibilityInteropView?.also {
                it.actualAccessibilityContainer = this
            })
        }

    fun update(node: AccessibilityNode, children: List<AccessibilityElement>) {
        assert(key == node.key) {
            "Element should be updated with a node that has the same key as the initial node"
        }
        this.node = node

        accessibilityElements?.forEach {
            (it as? CMPAccessibilityElement)?.setAccessibilityContainer(null)
        }
        setAccessibilityElements(children + nodeSemanticsElements())
        children.forEach { it.setAccessibilityContainer(this) }
        this.cachedProperties.clear()
    }

    fun dispose() {
        check(isAlive) {
            "AccessibilityElement is already disposed"
        }

        isAlive = false
        setAccessibilityContainer(null)
        setAccessibilityElements(emptyList<Any>())
        // region Tencent Code Modify
        /**
         * cachedProperties.clear()
         */
        synchronized(propertiesLock) {
            cachedProperties.clear()
        }
        // end region
    }

    /**
     * Returns the value for the given [key] from the cache if it's present, otherwise computes the
     * value using the given [block] and caches it.
     */
    @Suppress("UNCHECKED_CAST") // cast is safe because the set value is constrained by the key T
    private inline fun <T> getOrElse(
        key: CachedAccessibilityPropertyKey<T>,
        crossinline block: () -> T
    ): T {
        // region Tencent Code Modify
        /**
         * cachedProperties.getOrElse(key) {
         *     val newValue = block()
         *     cachedProperties[key] = newValue
         *     newValue
         * }
         */
        val value = synchronized(propertiesLock) {
            cachedProperties.getOrElse(key) {
                val newValue = block()
                cachedProperties[key] = newValue
                newValue
            }
        }
        // end region
        return value as T
    }

    override fun accessibilityLabel(): String? {
        // region Tencent Code
        if (!isAlive) {
            return null
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityLabel) {
            node.accessibilityLabel
        }
    }

    override fun accessibilityElementDidBecomeFocused() {
        if (!isAlive) {
            return
        }

        node.accessibilityElementDidBecomeFocused()
    }

    override fun accessibilityElementDidLoseFocus() {
        node.accessibilityElementDidLoseFocus()
    }

    override fun accessibilityActivate(): Boolean {
        if (!isAlive) {
            return false
        }

        return node.accessibilityActivate()
    }

    override fun accessibilityIncrement() {
        if (!isAlive) {
            return
        }

        node.accessibilityIncrement()
    }

    override fun accessibilityDecrement() {
        if (!isAlive) {
            return
        }

        node.accessibilityDecrement()
    }

    override fun accessibilityScrollToVisible(): Boolean {
        if (!isAlive) {
            return false
        }

        return node.accessibilityScrollToVisible()
    }

    override fun accessibilityScrollToVisibleWithChild(child: Any): Boolean {
        if (!isAlive) {
            return false
        }

        if (child is AccessibilityElement) {
            return child.accessibilityScrollToVisible()
        }

        return false
    }

    override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
        if (!isAlive) {
            return false
        }

        return node.accessibilityScroll(direction)
    }


    override fun isAccessibilityElement(): Boolean {
        // Node visibility changes don't trigger accessibility semantic recalculation.
        // This value should not be cached. See [SemanticsNode.isScreenReaderFocusable()]
        return isAlive && node.isAccessibilityElement
    }

    override fun accessibilityIdentifier(): String? {
        // region Tencent Code
        if (!isAlive) {
            return null
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityIdentifier) {
            node.accessibilityIdentifier
        }
    }

    override fun accessibilityHint(): String? {
        // region Tencent Code
        if (!isAlive) {
            return null
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityHint) {
            node.accessibilityHint
        }
    }

    override fun accessibilityCustomActions(): List<UIAccessibilityCustomAction> {
        // region Tencent Code
        if (!isAlive) {
            return emptyList()
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityCustomActions) {
            node.accessibilityCustomActions
        }
    }

    override fun accessibilityTraits(): UIAccessibilityTraits {
        // region Tencent Code
        if (!isAlive) {
            return UIAccessibilityTraitNone
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityTraits) {
            node.accessibilityTraits
        }
    }

    override fun accessibilityValue(): String? {
        // region Tencent Code
        if (!isAlive) {
            return null
        }
        // end region
        return getOrElse(CachedAccessibilityPropertyKeys.accessibilityValue) {
            node.accessibilityValue
        }
    }

    override fun accessibilityPerformEscape(): Boolean {
        if (!isAlive) {
            return false
        }

        return if (node.accessibilityPerformEscape()) {
            true
        } else {
            super.accessibilityPerformEscape()
        }
    }

    override fun accessibilityContainerType(): UIAccessibilityContainerType {
        // region Tencent Code Modify
        /*
        node.accessibilityContainerType
        */
        if (!isAlive) {
            return UIAccessibilityContainerTypeNone
        }
        return node.accessibilityContainerType
        // end region
    }

    private fun debugContainmentChain() = debugContainmentChain(this)

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)
        logger.apply {
            log { "${indent}${key}-${this}" }
            log { "$indent  isAccessibilityElement: ${isAccessibilityElement()}" }
            log { "$indent  containmentChain: ${debugContainmentChain()}" }
            log { "$indent  accessibilityLabel: ${accessibilityLabel()}" }
            log { "$indent  accessibilityValue: ${accessibilityValue()}" }
            log { "$indent  accessibilityTraits: ${accessibilityTraits()}" }
            log { "$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame())}" }
            log { "$indent  accessibilityIdentifier: ${accessibilityIdentifier()}" }
            log { "$indent  accessibilityCustomActions: ${accessibilityCustomActions()}" }
        }
    }

    // UIFocusItemProtocol & UIFocusItemContainerProtocol

    override fun canBecomeFocused(): Boolean = isAlive && node.canBecomeFocused

    override fun didUpdateFocusInContext(
        context: UIFocusUpdateContext,
        withAnimationCoordinator: UIFocusAnimationCoordinator
    ) {
        if (context.previouslyFocusedItem === this) {
            node.didResignFocused()
        }
        if (context.nextFocusedItem === this) {
            node.didBecomeFocused()
        }
    }

    override fun focusItemContainer(): UIFocusItemContainerProtocol = this

    var focusFrame: CValue<CGRect> = CGRectZero.readValue()
    override fun frame(): CValue<CGRect> = focusFrame

    override fun parentFocusEnvironment(): UIFocusEnvironmentProtocol? =
        accessibilityContainer as? UIFocusEnvironmentProtocol

    override fun preferredFocusEnvironments(): List<*> =
        accessibilityElements?.mapNotNull { it as? UIFocusEnvironmentProtocol } ?: emptyList<Any>()

    private var updateFocusScheduled = false
    override fun setNeedsFocusUpdate() {
        if (updateFocusScheduled) {
            return
        }
        updateFocusScheduled = true
        CoroutineScope(Dispatchers.Main).launch {
            updateFocusIfNeeded()
            updateFocusScheduled = false
        }
    }

    override fun updateFocusIfNeeded() {
        UIFocusSystem.focusSystemForEnvironment(environment = this)?.updateFocusIfNeeded()
    }

    override fun shouldUpdateFocusInContext(context: UIFocusUpdateContext): Boolean = true

    override fun coordinateSpace(): UICoordinateSpaceProtocol {
        var component: Any? = accessibilityContainer
        while (component != null) {
            when (component) {
                is UIView -> return component
                is CMPAccessibilityElement -> component = component.accessibilityContainer
                else -> error("Unexpected coordinate space.")
            }
        }
        error("Unexpected coordinate space.")
    }

    override fun focusItemsInRect(rect: CValue<CGRect>): List<*> = accessibilityElements?.filter {
        it is UIFocusItemProtocol && CGRectIntersectsRect(it.frame, rect)
    } ?: emptyList<Any>()

    override fun isTransparentFocusItem(): Boolean = true
}

private class NodesSyncResult(
    val newElementToFocus: Any?,
    val isScreenChange: Boolean
)

/**
 * An interface for logging accessibility debug messages.
 */
interface AccessibilityDebugLogger {
    /**
     * Logs the given [message].
     */
    fun log(message: (() -> Any?)?)
}

public var accessibilityDebugLogger: AccessibilityDebugLogger? = null
// Uncomment for debugging:
// private val accessibilityDebugLogger: AccessibilityDebugLogger? =
//     object : AccessibilityDebugLogger {
//         override fun log(message: Any?) {
//             if (message == null) {
//                 println()
//             } else {
//                 println("[a11y]: $message")
//             }
//         }
//     }

private sealed interface AccessibilityElementFocusMode {
    val targetElementKey: AccessibilityElementKey?

    /**
     * Do not change focus. Notifies about content changes.
     */
    data object None : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey? = null
    }

    /**
     * Keeps focus at the element if present, or notify about significant changes on a screen
     */
    data class KeepFocus(val key: AccessibilityElementKey) : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey = key
    }
}

/**
 * A sealed class that represents the options for syncing the Compose SemanticsNode tree with the iOS UIAccessibility tree.
 */
@ExperimentalComposeApi
sealed class AccessibilitySyncOptions(
    internal val debugLogger: AccessibilityDebugLogger?
) {
    /**
     * Never sync the tree.
     */
    data object Never: AccessibilitySyncOptions(debugLogger = null)

    /**
     * Sync the tree only when the accessibility services are running.
     *
     * @param debugLogger Optional [AccessibilityDebugLogger] to log into the info about the
     * accessibility tree syncing and interactions.
     */
    class WhenRequiredByAccessibilityServices(debugLogger: AccessibilityDebugLogger?): AccessibilitySyncOptions(debugLogger)

    /**
     * Always sync the tree, can be quite handy for debugging and testing.
     * Be aware that there is a significant overhead associated with doing it that can degrade
     * the visual performance of the app.
     *
     * @param debugLogger Optional [AccessibilityDebugLogger] to log into the info about the
     * accessibility tree syncing and interactions.
     */
    class Always(debugLogger: AccessibilityDebugLogger?): AccessibilitySyncOptions(debugLogger = debugLogger)
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
@OptIn(ExperimentalComposeApi::class)
internal class AccessibilityMediator(
    val view: UIView,
    val owner: SemanticsOwner,
    val coroutineContext: CoroutineContext,
    val performEscape: () -> Boolean,
    onKeyboardPresses: (Set<*>) -> Unit,
    val onScreenReaderActive: (Boolean) -> Unit,
    private val syncOptions: AccessibilitySyncOptions
) {
    private var focusMode: AccessibilityElementFocusMode = AccessibilityElementFocusMode.None
        set(value) {
            field = value
            accessibilityDebugLogger?.log {"Focus mode: $focusMode"}

            scheduleFocusedScrollableParentsIdsUpdate()
        }

    var focusedNodesScrollableParentsIds = setOf<Int>()
        private set

    var keyboardFocusedElementKey: AccessibilityElementKey? = null

    /**
     * A set of node ids that had their bounds invalidated after the last sync.
     */
    private val invalidationChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    private val root = AccessibilityRoot(mediator = this, onKeyboardPresses = onKeyboardPresses)

    /**
     * A map of all [AccessibilityElementKey] currently present in the tree to corresponding
     * [AccessibilityElement].
     */
    private val accessibilityElementsMap =
        mutableMapOf<AccessibilityElementKey, AccessibilityElement>()

    // region Tencent Code
    private val delayedReleaseElements = mutableListOf<AccessibilityElement>()

    private var disposed = false
    // endregion


    internal fun accessibilityElementWithIdentifier(identifier: String) : Any? {
        activateAccessibilityIfNeeded()
        val iterator = accessibilityElementsMap.iterator()
        while (iterator.hasNext()) {
            val accessibilityElement = iterator.next().value
            if (accessibilityElement.isAlive) {
                if (identifier == accessibilityElement.accessibilityIdentifier) {
                    return accessibilityElement
                }
                else if (identifier == accessibilityElement.accessibilityLabel) {
                    return accessibilityElement
                }
            }
        }
        return null
    }

    var isEnabled: Boolean = syncOptions != AccessibilitySyncOptions.Never
        set(value) {
            // region Tencent code
            if (syncOptions == AccessibilitySyncOptions.Never) {
                return
            }
            // region

            if (field != value) {
                field = value
                onSemanticsChange()

                UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
            }
        }

    val safeAreaRectInWindow: Rect get() {
        val rectInWindow = view.convertRect(
            rect = UIEdgeInsetsInsetRect(view.bounds, view.safeAreaInsets),
            toView = null
        )
        return rectInWindow.asDpRect().toRect(view.density)
    }

    private val forceEnable = syncOptions is AccessibilitySyncOptions.Always

    // region Tencent Code
    private val isIOSSortEnable = ComposeTabService.composeIOSSemanticSortEnable
    // endregion

    init {
        accessibilityDebugLogger?.log { "AccessibilityMediator for $view created" }
        view.accessibilityElements = listOf(root)

        coroutineScope.launch {
            // The main loop that listens for invalidations and performs the tree syncing
            // Will exit on CancellationException from within await on `invalidationChannel.receive()`
            // when [job] is cancelled
            while (true) {
                invalidationChannel.receive()

                // Estimated delay between the iOS Accessibility Engine sync intervals.
                // There is no reason to post change notifications more frequently because the iOS
                // Accessibility Engine will ignore them.
                delay(100)

                while (invalidationChannel.tryReceive().isSuccess) {
                    // Do nothing, just consume the channel
                    // Workaround for the channel buffering two invalidations despite the capacity of 1
                }

                // region Tencent Code Modify
                /*
                *  if (isEnabled) {
                    if (isAccessibilityActive)
                */
                if (isEnabled || forceEnable) {
                    if (isAccessibilityActive || forceEnable) {
                        scheduleAccessibilityDisablingAndCleanup()
                        val time = measureTime {
                            sync().postNotification()
                        }
                        accessibilityDebugLogger?.log { "AccessibilityMediator.sync took $time" }
                    }
                } else if (root.element != null) {
                    refocusKeyboardElementIfNeeded()
                    root.element = null
                    UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, null)
                }
                releaseDisposedElements()
                // endregion
            }
        }
    }

    // region Tencent Code
    private fun releaseDisposedElements() {
        /*
        延迟释放避免 crash，猜测是 delayedReleaseElements 中的元素被 dispose 后，
        被 GC 回收后，UIAccessibilityPostNotification 后系统仍然在访问，因此需要移动到 PostNotification 后，
        这里暂定 3s 解除引用
        */
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 3 * NSEC_PER_SEC.toLong()), dispatch_get_main_queue()) {
            delayedReleaseElements.clear()
        }
    }
    // endregion

    /**
     * Indicates that accessibility has recently been requested and can be considered active.
     * The flag is set to false if no accessibility tree reads occur for some time.
     */
    private var isAccessibilityActive: Boolean = false

    private var disableAccessibilityJob: Job? = null

    private fun scheduleAccessibilityDisablingAndCleanup() {
        if (disableAccessibilityJob != null ||
            keyboardFocusedElementKey != null ||
            focusMode is AccessibilityElementFocusMode.KeepFocus) {
            return
        }
        disableAccessibilityJob = coroutineScope.launch {
            // Allow some time for the iOS Accessibility Engine to read the updated accessibility
            // elements tree. If no new reads occur during this time, it is assumed that iOS
            // Accessibility has been disabled and resources can be cleaned up.
            delay(2000)
            if (!disposed) {
                cleanUp()
            }
        }
    }

    private fun cancelAccessibilityDisabling() {
        disableAccessibilityJob?.cancel()
        disableAccessibilityJob = null
    }

    fun activateAccessibilityIfNeeded() {
        isAccessibilityActive = true
        if (root.element == null) {
            sync().postNotification()
            releaseDisposedElements()
        }
        cancelAccessibilityDisabling()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasPendingInvalidations: Boolean get() = !invalidationChannel.isEmpty

    private fun convertToAppWindowCGRect(rect: Rect): CValue<CGRect> {
        return view.convertRect(rect.toDpRect(view.density).asCGRect(), toView = null)
    }

    private fun convertToRootViewCGRect(rect: Rect): CValue<CGRect> {
        return rect.toDpRect(view.density).asCGRect()
    }

    fun notifyScrollCompleted(
        scrollResult: AccessibilityScrollEventResult,
        delay: Long,
        focusedNode: SemanticsNode,
        focusedRectInWindow: Rect
    ) {
        coroutineScope.launch {
            delay(delay)

            UIAccessibilityPostNotification(
                UIAccessibilityPageScrolledNotification,
                scrollResult.announceMessage()
            )

            UIAccessibilityPostNotification(
                UIAccessibilityPageScrolledNotification,
                null
            )

            accessibilityDebugLogger?.log { "PageScrolled" }

            if (accessibilityElementsMap[focusedNode.semanticsKey] == null) {
                val element = findClosestElementToRect(rect = focusedRectInWindow)
                accessibilityDebugLogger?.log { "LayoutChanged, result: $element" }

                (element as? AccessibilityElement)?.let {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(element.key)
                }

                UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, element)
            }
        }
    }

    fun onSemanticsChange() {
        accessibilityDebugLogger?.log { "onSemanticsChange" }
        invalidationChannel.trySend(Unit)
    }

    fun onLayoutChange(nodeId: Int) {
        accessibilityDebugLogger?.log { "onLayoutChange (nodeId=$nodeId)" }
        invalidationChannel.trySend(Unit)
    }

    fun dispose() {
        disposed = true
        job.cancel()
        disableAccessibilityJob?.cancel()

        refocusKeyboardElementIfNeeded()
        view.accessibilityElements = listOf<NSObject>()
        root.onKeyboardPresses = {}

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }
        cleanUp2()
    }

    // region Tencent Code
    private fun cleanUp2() {
        disableAccessibilityJob = null
        isAccessibilityActive = false

        root.element = null
        UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, null)
        dispatch_after(dispatch_time(DISPATCH_TIME_NOW, 2 * NSEC_PER_SEC.toLong()), dispatch_get_main_queue()) {
            accessibilityElementsMap.clear()
            delayedReleaseElements.clear()
        }
    }
    // end region

    private fun cleanUp() {
        disableAccessibilityJob = null
        isAccessibilityActive = false

        root.element = null
        accessibilityElementsMap.clear()
    }

    private var focusedScrollableParentsIdsUpdateJob: Job? = null
    private fun scheduleFocusedScrollableParentsIdsUpdate() {
        focusedScrollableParentsIdsUpdateJob?.cancel()
        focusedScrollableParentsIdsUpdateJob = coroutineScope.launch {
            // Throttle the recalculation of scrollable parent node IDs to avoid unnecessary
            // reloading of the accessibility tree when the focusMode changes quickly.
            delay(10)
            val ids = (focusMode as? AccessibilityElementFocusMode.KeepFocus)?.key?.let {
                accessibilityElementsMap[it]?.node?.semanticsNode?.allScrollableParentNodeIds
            } ?: emptySet()

            if (focusedNodesScrollableParentsIds != ids) {
                focusedNodesScrollableParentsIds = ids
                invalidationChannel.trySend(Unit)

                if (ids.isNotEmpty()) {
                    // Hack to fix an issue where iOS accessibility only reads the items visible
                    // at the moment of the beginning of the "Speak Screen" command.
                    UIAccessibilityPostNotification(UIAccessibilityPageScrolledNotification, null)
                }
            }
        }
    }

    private fun createOrUpdateAccessibilityElement(
        node: AccessibilityNode,
        children: List<AccessibilityElement> = emptyList(),
        frame: Rect
    ): AccessibilityElement {
        val element = accessibilityElementsMap[node.key]?.also {
            it.update(node = node, children = children)
        } ?: AccessibilityElement(node = node, children = children).also {
            accessibilityElementsMap[node.key] = it
        }

        val accessibilityFrame = convertToAppWindowCGRect(frame)
        if (!CGRectEqualToRect(accessibilityFrame, element.accessibilityFrame)) {
            element.setAccessibilityFrame(accessibilityFrame)
        }
        element.focusFrame = convertToRootViewCGRect(frame)
        return element
    }

    /**
     * Traverses semantics tree starting from rootNode and returns an accessibility object which will
     * be put into iOS view's [accessibilityElements] property.
     *
     * Inserts new elements to [accessibilityElementsMap], updates the old ones, and removes the elements
     * that are not present in the tree anymore.
     */
    private fun traverseSemanticsTree(rootNode: SemanticsNode): AccessibilityElement {
        val presentIds = mutableSetOf<AccessibilityElementKey>()

        val nodes = owner.getAllUncoveredSemanticsNodesToIntObjectMap(rootNode.id)
        keyboardFocusedElementKey?.id?.let {
            if (!nodes.contains(it)) {
                // The keyboard-focused node is removed. It's important to trigger focus reload
                // before the node is actually removed from the accessibility elements tree.
                refocusKeyboardElementIfNeeded()
            }
        }

        // 1. Flatten all children except nodes inside traversal groups to:
        // - have the same traversal order as on Android
        // - allow navigation between semantic containers on iOS
        // 2. Split non-visible children beyond bounds to be located go before and after the group
        // of visible semantic children in the accessibility elements tree.
        // See [isBeforeBeyondBoundsItem] for more details.
        fun SemanticsNode.flattenChildrenInsideTraversalGroup(
            node: SemanticsNode,
            semanticsChildren: ArrayList<SemanticsNode>,
            beforeBeyondBoundsChildren: ArrayList<SemanticsNode>,
            afterBeyondBoundsChildren: ArrayList<SemanticsNode>
        ) {
            node.replacedChildren.fastForEach { child ->
                if (child.isValid) {
                    if (nodes.contains(child.id)) {
                        semanticsChildren.add(child)
                    } else if (child.size != IntSize.Zero && child.isScreenReaderFocusable()) {
                        if (child.isBeforeBeyondBoundsItem(container = this)) {
                            beforeBeyondBoundsChildren.add(child)
                        } else {
                            afterBeyondBoundsChildren.add(child)
                        }
                    }
                }
                if (!child.isTraversalGroup) {
                    flattenChildrenInsideTraversalGroup(
                        child,
                        semanticsChildren,
                        beforeBeyondBoundsChildren,
                        afterBeyondBoundsChildren
                    )
                }
            }
        }

        fun traverseGroup(node: SemanticsNode, isBeyondBounds: Boolean): AccessibilityElement {
            presentIds.add(node.semanticsKey)

            val frame = nodes[node.id]?.adjustedBounds?.toRect() ?: node.unclippedBoundsInWindow

            fun makeSemanticsNode() = createOrUpdateAccessibilityElement(
                node = AccessibilityNode.Semantics(
                    semanticsNode = node,
                    mediator = this,
                    isBeyondBounds = isBeyondBounds
                ),
                frame = frame
            )

            if (!node.isTraversalGroup && node.id != rootNode.id) {
                return makeSemanticsNode()
            }

            val visibleChildren = ArrayList<SemanticsNode>()
            val beforeChildren = ArrayList<SemanticsNode>()
            val afterChildren = ArrayList<SemanticsNode>()
            node.flattenChildrenInsideTraversalGroup(
                node, visibleChildren, beforeChildren, afterChildren
            )

            // region Tencent Code Modify
            /**
             *  val sortedChildren = node.sortByGeometryGroupings(visibleChildren)
             */
            val zIndexMap = mutableMapOf<Int, Int>()
            visibleChildren.forEachIndexed { index, node ->
                zIndexMap[node.id] = index
            }

            val sortedChildren = node.sortByGeometryGroupings(visibleChildren, zIndexMap = zIndexMap, isIOS = isIOSSortEnable)
            // endregion

            beforeChildren.sortWith(BeyondBoundsComparator(node.isRTL))
            afterChildren.sortWith(BeyondBoundsComparator(node.isRTL))

            val visibleElements = sortedChildren.map { traverseGroup(it, isBeyondBounds) }
            val beforeElements = beforeChildren.map { traverseGroup(it, isBeyondBounds = true) }
            val afterElements = afterChildren.map { traverseGroup(it, isBeyondBounds = true) }

            val containerElements = if (node.isImportantForAccessibility()) {
                listOf(makeSemanticsNode())
            } else {
                emptyList()
            }

            presentIds.add(node.containerKey)
            return createOrUpdateAccessibilityElement(
                node = AccessibilityNode.Container(semanticsNode = node),
                children = beforeElements + containerElements + visibleElements + afterElements,
                frame = frame
            )
        }

        val rootAccessibilityElement = traverseGroup(rootNode, isBeyondBounds = false)

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            val isPresent = it in presentIds

            if (!isPresent) {
                accessibilityDebugLogger?.log { "$it removed" }
                // region Tencent Code Modify
                /*
                checkNotNull(accessibilityElementsMap[it]).dispose()
                */
                val disposedElement = accessibilityElementsMap[it]
                checkNotNull(disposedElement).dispose()
                delayedReleaseElements.add(disposedElement)
                // end region
            }

            isPresent
        }

        return rootAccessibilityElement
    }

    /**
     * Performs a complete sync of the accessibility tree with the current semantics tree.
     */
    private fun sync(): NodesSyncResult {
        val rootSemanticsNode = owner.unmergedRootSemanticsNode

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        root.element = traverseSemanticsTree(rootSemanticsNode)

        accessibilityDebugLogger?.let {
            debugTraverse(it, view)
        }

        return updateFocusedElement()
    }

    private fun updateFocusedElement(): NodesSyncResult {
        return when (val mode = focusMode) {
            AccessibilityElementFocusMode.None -> {
                NodesSyncResult(newElementToFocus = null, isScreenChange = false)
            }

            is AccessibilityElementFocusMode.KeepFocus -> {
                val focusedElement = UIAccessibilityFocusedElement(null)
                val element = accessibilityElementsMap[mode.key]
                if (element != null && !CGRectIsEmpty(element.accessibilityFrame())) {
                    NodesSyncResult(element.takeIf { it !== focusedElement }, isScreenChange = false)
                } else if (focusedElement is AccessibilityElement) {
                    val newFocusedElement = root.element?.let { findFocusableElement(it) }

                    focusMode = if (newFocusedElement is AccessibilityElement) {
                        AccessibilityElementFocusMode.KeepFocus(newFocusedElement.key)
                    } else {
                        AccessibilityElementFocusMode.None
                    }

                    NodesSyncResult(newFocusedElement, isScreenChange = true)
                } else {
                    NodesSyncResult(null, isScreenChange = false)
                }
            }
        }
    }

    private fun findClosestElementToRect(rect: Rect): Any? {
        val windowRect = convertToAppWindowCGRect(rect)
        val centerPoint = CGPointMake(
            x = CGRectGetMidX(windowRect),
            y = CGRectGetMidY(windowRect)
        )

        var closestElement: Pair<Double, NSObject>? = null

        fun findElement(element: NSObject, point: CValue<CGPoint>): Any? {
            if (element.isAccessibilityElement) {
                val distanceSQ = minimalDistanceSQ(point, element.accessibilityFrame)
                if (distanceSQ == 0.0) {
                    return element
                } else if (closestElement == null || distanceSQ < closestElement!!.first) {
                    closestElement = distanceSQ to element
                }
            }

            repeat(element.accessibilityElementCount().toInt()) { index ->
                element.accessibilityElementAtIndex(index.toLong())?.let { element ->
                    findElement(element as NSObject, point)?.let {
                        return it
                    }
                }
            }

            return null
        }

        findElement(root as NSObject, centerPoint)

        return closestElement?.second
    }

    /**
     * Calculates the squared minimal Euclidean distance between a point and the nearest point on
     * the boundary of a rectangle.
     */
    private fun minimalDistanceSQ(point: CValue<CGPoint>, rect: CValue<CGRect>): Double {
        // Clamp the point to the nearest point on the rectangle
        val clampedX = min(max(point.useContents { x }, CGRectGetMinX(rect)), CGRectGetMaxX(rect))
        val clampedY = min(max(point.useContents { y }, CGRectGetMinY(rect)), CGRectGetMaxY(rect))

        // Return the Euclidean distance between the `point` and the nearest point on the edge
        val dx = clampedX - point.useContents { x }
        val dy = clampedY - point.useContents { y }
        return dx * dx + dy * dy
    }

    fun setFocusTarget(key: AccessibilityElementKey) {
        focusMode = AccessibilityElementFocusMode.KeepFocus(key)
    }

    fun clearFocusTargetIfNeeded(key: AccessibilityElementKey) {
        if (focusMode.targetElementKey == key) {
            focusMode = AccessibilityElementFocusMode.None
        }
    }

    private fun findFocusableElement(node: Any): Any? {
        val nsNode = node as NSObject
        if (nsNode.isAccessibilityElement) {
            return nsNode
        }
        repeat(node.accessibilityElementCount().toInt()) { index ->
            node.accessibilityElementAtIndex(index.toLong())?.let {
                findFocusableElement(it)
            }
        }
        return null
    }

    private fun NodesSyncResult.postNotification() {
        val notificationName = if (isScreenChange) {
            UIAccessibilityScreenChangedNotification
        } else {
            UIAccessibilityLayoutChangedNotification
        }
        UIAccessibilityPostNotification(notificationName, newElementToFocus)
        accessibilityDebugLogger?.log { "UIAccessibilityPostNotification newElementToFocus:$newElementToFocus" }
    }

    private fun refocusKeyboardElementIfNeeded() {
        if (keyboardFocusedElementKey != null) {
            view.window?.let {
                UIFocusSystem.focusSystemForEnvironment(it)?.requestFocusUpdateToEnvironment(it)
            }
            keyboardFocusedElementKey = null
        }
    }
}

/**
 * Traverse the accessibility tree starting from [accessibilityObject] using the same(assumed) logic
 * as iOS Accessibility services, and prints its debug data.
 */
private fun debugTraverse(debugLogger: AccessibilityDebugLogger, accessibilityObject: Any, depth: Int = 0) {
    val indent = " ".repeat(depth * 2)

    when (accessibilityObject) {
        is UIView -> {
            debugLogger.log { "${indent}View($accessibilityObject)" }

            accessibilityObject.accessibilityElements?.let { elements ->
                for (element in elements) {
                    element?.let {
                        debugTraverse(debugLogger, element, depth + 1)
                    }
                }
            }
        }

        is AccessibilityElement -> {
            accessibilityObject.debugLog(debugLogger, depth)

            val count = accessibilityObject.accessibilityElementCount()
            for (index in 0 until count) {
                val element = accessibilityObject.accessibilityElementAtIndex(index)
                element?.let {
                    debugTraverse(debugLogger, element, depth + 1)
                }
            }
        }

        is AccessibilityRoot -> {
            debugLogger.log{ "${indent}Root"}
            accessibilityObject.element?.let {
                debugTraverse(debugLogger, it, depth + 1)
            }
        }

        else -> {
            throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
        }
    }
}

private fun debugContainmentChain(accessibilityObject: Any): String {
    val strings = mutableListOf<String>()

    var currentObject = accessibilityObject as? Any

    while (currentObject != null) {
        when (val constCurrentObject = currentObject) {
            is AccessibilityElement -> {
                strings.add(constCurrentObject.key.toString())
                currentObject = constCurrentObject.accessibilityContainer
            }

            is AccessibilityRoot -> {
                strings.add("Root")
                currentObject = constCurrentObject.accessibilityContainer
            }

            is UIView -> {
                strings.add("View")
                currentObject = null
            }

            else -> {
                throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
            }
        }
    }

    return strings.joinToString(" -> ")
}

private val SemanticsNode.semanticsKey get() = AccessibilityElementKey.Semantics(id)
private val SemanticsNode.containerKey get() = AccessibilityElementKey.Container(id)

/**
 * Returns true if corresponding [LayoutNode] is placed and attached, false otherwise.
 */
private val SemanticsNode.isValid: Boolean
    get() = layoutNode.isPlaced && layoutNode.isAttached

private val SemanticsNode.isTraversalGroup: Boolean
    get() = unmergedConfig.getOrElse(SemanticsProperties.IsTraversalGroup) { false }

/**
 * Divides semantics beyond bounds children to be located before and after the block of visible
 * semantics children, based on the assumption that `before children` are located above and to the
 * left (to the right for RTL layout) of the centre of the parent node.
 * This rule corresponds to the way the iOS accessibility engine traverses elements on the screen.
 */
private fun SemanticsNode.isBeforeBeyondBoundsItem(container: SemanticsNode): Boolean {
    var centerOffset = container.unclippedBoundsInWindow.center - unclippedBoundsInWindow.center
    if (!container.isRTL) {
        centerOffset = centerOffset.copy(x = -centerOffset.x)
    }

    return centerOffset.x < centerOffset.y
}

/**
 * Simplified version of [SemanticsNode.sortByGeometryGroupings] based on the
 * [SemanticsNode.unclippedBoundsInWindow] because [SemanticsNode.boundsInWindow] is empty for
 * nodes beyond visible bounds.
 */
private class BeyondBoundsComparator(private val isRTL: Boolean) : Comparator<SemanticsNode> {
    override fun compare(a: SemanticsNode, b: SemanticsNode): Int {
        var result = a.unmergedConfig
            .getOrElse(SemanticsProperties.TraversalIndex) { 0f }
            .compareTo(b.unmergedConfig.getOrElse(SemanticsProperties.TraversalIndex) { 0f })

        if (result != 0) {
            return result
        }

        result = a.unclippedBoundsInWindow.center.y
            .compareTo(b.unclippedBoundsInWindow.center.y)

        if (result != 0) {
            return result
        }

        result = a.unclippedBoundsInWindow.center.x
            .compareTo(b.unclippedBoundsInWindow.center.x)

        if (result != 0) {
            return if (isRTL) -result else result
        }

        return result
    }
}

internal fun SemanticsNode.isImportantForAccessibility() =
    !isHidden &&
            (unmergedConfig.isMergingSemanticsOfDescendants ||
                    unmergedConfig.contains(SemanticsProperties.TestTag) ||
                    unmergedConfig.containsImportantForAccessibility())

// region Tencent Code

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
@OptIn(ExperimentalComposeApi::class)
internal class AccessibilityMediator2(
    val view: UIView,
    val owner: SemanticsOwner,
    val coroutineContext: CoroutineContext,
    val performEscape: () -> Boolean,
    onKeyboardPresses: (Set<*>) -> Unit,
    val onScreenReaderActive: (Boolean) -> Unit,
    private val syncOptions: AccessibilitySyncOptions
) {
    private var focusMode: AccessibilityElementFocusMode = AccessibilityElementFocusMode.None
        set(value) {
            field = value
            accessibilityDebugLogger?.log {"Focus mode: $focusMode"}

            scheduleFocusedScrollableParentsIdsUpdate()
        }

    var focusedNodesScrollableParentsIds = setOf<Int>()
        private set

    var keyboardFocusedElementKey: AccessibilityElementKey? = null

    /**
     * A set of node ids that had their bounds invalidated after the last sync.
     */
    private val invalidationChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    private val root = AccessibilityRoot2(mediator = this, onKeyboardPresses = onKeyboardPresses)

    /**
     * A map of all [AccessibilityElementKey] currently present in the tree to corresponding
     * [AccessibilityElement].
     */
    private val accessibilityElementsMap =
        mutableMapOf<AccessibilityElementKey, AccessibilityElement2>()

    internal fun accessibilityElementWithIdentifier(identifier: String) : Any? {
        activateAccessibilityIfNeeded()
        val iterator = accessibilityElementsMap.iterator()
        while (iterator.hasNext()) {
            val accessibilityElement = iterator.next().value
            if (accessibilityElement.isAlive) {
                if (identifier == accessibilityElement.accessibilityIdentifier2()) {
                    return accessibilityElement
                }
                else if (identifier == accessibilityElement.accessibilityLabel2()) {
                    return accessibilityElement
                }
            }
        }
        return null
    }

    private var disposed = false

    private var isEnabled: Boolean = syncOptions != AccessibilitySyncOptions.Never
        set(value) {
            // region Tencent code
            if (syncOptions == AccessibilitySyncOptions.Never) {
                return
            }
            // region

            if (field != value) {
                field = value
                onSemanticsChange()

                UIAccessibilityPostNotification(UIAccessibilityScreenChangedNotification, null)
            }
        }

    val safeAreaRectInWindow: Rect get() {
        val rectInWindow = view.convertRect(
            rect = UIEdgeInsetsInsetRect(view.bounds, view.safeAreaInsets),
            toView = null
        )
        return rectInWindow.asDpRect().toRect(view.density)
    }

    private val forceEnable = syncOptions is AccessibilitySyncOptions.Always

    // region Tencent Code
    private val isIOSSortEnable = ComposeTabService.composeIOSSemanticSortEnable
    // endregion

    init {
        accessibilityDebugLogger?.log { "AccessibilityMediator for $view created" }
        view.accessibilityElements = listOf(root.nativeRoot)

        coroutineScope.launch {
            // The main loop that listens for invalidations and performs the tree syncing
            // Will exit on CancellationException from within await on `invalidationChannel.receive()`
            // when [job] is cancelled
            while (true) {
                invalidationChannel.receive()

                // Estimated delay between the iOS Accessibility Engine sync intervals.
                // There is no reason to post change notifications more frequently because the iOS
                // Accessibility Engine will ignore them.
                delay(100)

                while (invalidationChannel.tryReceive().isSuccess) {
                    // Do nothing, just consume the channel
                    // Workaround for the channel buffering two invalidations despite the capacity of 1
                }

                // region Tencent Code Modify
                /*
                *  if (isEnabled) {
                    if (isAccessibilityActive)
                */
                if (isEnabled || forceEnable) {
                    if (isAccessibilityActive || forceEnable) {
                        scheduleAccessibilityDisablingAndCleanup()
                        val time = measureTime {
                            sync().postNotification()
                        }
                        accessibilityDebugLogger?.log { "AccessibilityMediator.sync took $time" }
                    }
                } else if (root.element != null) {
                    refocusKeyboardElementIfNeeded()
                    root.element = null
                    UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, null)
                }
                // endregion
            }
        }
    }

    /**
     * Indicates that accessibility has recently been requested and can be considered active.
     * The flag is set to false if no accessibility tree reads occur for some time.
     */
    private var isAccessibilityActive: Boolean = false

    private var disableAccessibilityJob: Job? = null

    private fun scheduleAccessibilityDisablingAndCleanup() {
        if (disableAccessibilityJob != null ||
            keyboardFocusedElementKey != null ||
            focusMode is AccessibilityElementFocusMode.KeepFocus) {
            return
        }
        disableAccessibilityJob = coroutineScope.launch {
            // Allow some time for the iOS Accessibility Engine to read the updated accessibility
            // elements tree. If no new reads occur during this time, it is assumed that iOS
            // Accessibility has been disabled and resources can be cleaned up.
            delay(2000)
            if (!disposed) {
                cleanUp()
            }
        }
    }

    private fun cancelAccessibilityDisabling() {
        disableAccessibilityJob?.cancel()
        disableAccessibilityJob = null
    }

    fun activateAccessibilityIfNeeded() {
        isAccessibilityActive = true
        if (root.element == null) {
            sync().postNotification()
        }
        cancelAccessibilityDisabling()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasPendingInvalidations: Boolean get() = !invalidationChannel.isEmpty

    private fun convertToAppWindowCGRect(rect: Rect): CValue<CGRect> {
        return view.convertRect(rect.toDpRect(view.density).asCGRect(), toView = null)
    }

    private fun convertToRootViewCGRect(rect: Rect): CValue<CGRect> {
        return rect.toDpRect(view.density).asCGRect()
    }

    fun notifyScrollCompleted(
        scrollResult: AccessibilityScrollEventResult,
        delay: Long,
        focusedNode: SemanticsNode,
        focusedRectInWindow: Rect
    ) {
        coroutineScope.launch {
            delay(delay)

            UIAccessibilityPostNotification(
                UIAccessibilityPageScrolledNotification,
                scrollResult.announceMessage()
            )

            UIAccessibilityPostNotification(
                UIAccessibilityPageScrolledNotification,
                null
            )

            accessibilityDebugLogger?.log { "PageScrolled" }

            if (accessibilityElementsMap[focusedNode.semanticsKey] == null) {
                val element = findClosestElementToRect(rect = focusedRectInWindow)
                accessibilityDebugLogger?.log { "LayoutChanged, result: $element" }

                (element as? AccessibilityElement)?.let {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(element.key)
                }

                UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, element)
            }
        }
    }

    fun onSemanticsChange() {
        accessibilityDebugLogger?.log { "onSemanticsChange" }
        invalidationChannel.trySend(Unit)
    }

    fun onLayoutChange(nodeId: Int) {
        accessibilityDebugLogger?.log { "onLayoutChange (nodeId=$nodeId)" }
        invalidationChannel.trySend(Unit)
    }

    fun dispose() {
        disposed = true
        job.cancel()
        disableAccessibilityJob?.cancel()

        refocusKeyboardElementIfNeeded()
        view.accessibilityElements = listOf<NSObject>()

        root.dispose()

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }
        cleanUp()
    }

    private fun cleanUp() {
        disableAccessibilityJob = null
        isAccessibilityActive = false

        root.element = null
        accessibilityElementsMap.clear()
    }

    private var focusedScrollableParentsIdsUpdateJob: Job? = null
    private fun scheduleFocusedScrollableParentsIdsUpdate() {
        focusedScrollableParentsIdsUpdateJob?.cancel()
        focusedScrollableParentsIdsUpdateJob = coroutineScope.launch {
            // Throttle the recalculation of scrollable parent node IDs to avoid unnecessary
            // reloading of the accessibility tree when the focusMode changes quickly.
            delay(10)
            val ids = (focusMode as? AccessibilityElementFocusMode.KeepFocus)?.key?.let {
                accessibilityElementsMap[it]?.node?.semanticsNode?.allScrollableParentNodeIds
            } ?: emptySet()

            if (focusedNodesScrollableParentsIds != ids) {
                focusedNodesScrollableParentsIds = ids
                invalidationChannel.trySend(Unit)

                if (ids.isNotEmpty()) {
                    // Hack to fix an issue where iOS accessibility only reads the items visible
                    // at the moment of the beginning of the "Speak Screen" command.
                    UIAccessibilityPostNotification(UIAccessibilityPageScrolledNotification, null)
                }
            }
        }
    }

    private fun createOrUpdateAccessibilityElement(
        node: AccessibilityNode,
        children: List<AccessibilityElement2> = emptyList(),
        frame: Rect
    ): AccessibilityElement2 {
        val element = accessibilityElementsMap[node.key]?.also {
            it.update(node = node, children = children)
        } ?: AccessibilityElement2(
            node = node,
            children = children,
            nativeRoot = root.nativeRoot
        ).also {
            accessibilityElementsMap[node.key] = it
        }

        convertToAppWindowCGRect(frame).useContents {
            element.setAccessibilityFrame(originX = origin.x, originY = origin.y, width = size.width, height = size.height)
        }
        convertToRootViewCGRect(frame).useContents {
            element.setFocusFrame(originX = origin.x, originY = origin.y, width = size.width, height = size.height)
        }
        return element
    }

    /**
     * Traverses semantics tree starting from rootNode and returns an accessibility object which will
     * be put into iOS view's [accessibilityElements] property.
     *
     * Inserts new elements to [accessibilityElementsMap], updates the old ones, and removes the elements
     * that are not present in the tree anymore.
     */
    private fun traverseSemanticsTree(rootNode: SemanticsNode): AccessibilityElement2 {
        val presentIds = mutableSetOf<AccessibilityElementKey>()

        val nodes = owner.getAllUncoveredSemanticsNodesToIntObjectMap(rootNode.id)
        keyboardFocusedElementKey?.id?.let {
            if (!nodes.contains(it)) {
                // The keyboard-focused node is removed. It's important to trigger focus reload
                // before the node is actually removed from the accessibility elements tree.
                refocusKeyboardElementIfNeeded()
            }
        }

        // 1. Flatten all children except nodes inside traversal groups to:
        // - have the same traversal order as on Android
        // - allow navigation between semantic containers on iOS
        // 2. Split non-visible children beyond bounds to be located go before and after the group
        // of visible semantic children in the accessibility elements tree.
        // See [isBeforeBeyondBoundsItem] for more details.
        fun SemanticsNode.flattenChildrenInsideTraversalGroup(
            node: SemanticsNode,
            semanticsChildren: ArrayList<SemanticsNode>,
            beforeBeyondBoundsChildren: ArrayList<SemanticsNode>,
            afterBeyondBoundsChildren: ArrayList<SemanticsNode>
        ) {
            node.replacedChildren.fastForEach { child ->
                if (child.isValid) {
                    if (nodes.contains(child.id)) {
                        semanticsChildren.add(child)
                    } else if (child.size != IntSize.Zero && child.isScreenReaderFocusable()) {
                        if (child.isBeforeBeyondBoundsItem(container = this)) {
                            beforeBeyondBoundsChildren.add(child)
                        } else {
                            afterBeyondBoundsChildren.add(child)
                        }
                    }
                }
                if (!child.isTraversalGroup) {
                    flattenChildrenInsideTraversalGroup(
                        child,
                        semanticsChildren,
                        beforeBeyondBoundsChildren,
                        afterBeyondBoundsChildren
                    )
                }
            }
        }

        fun traverseGroup(node: SemanticsNode, isBeyondBounds: Boolean): AccessibilityElement2 {
            presentIds.add(node.semanticsKey)

            val frame = nodes[node.id]?.adjustedBounds?.toRect() ?: node.unclippedBoundsInWindow

            fun makeSemanticsNode() = createOrUpdateAccessibilityElement(
                node = AccessibilityNode.Semantics2(
                    semanticsNode = node,
                    mediator = this,
                    isBeyondBounds = isBeyondBounds
                ),
                frame = frame
            )

            if (!node.isTraversalGroup && node.id != rootNode.id) {
                return makeSemanticsNode()
            }

            val visibleChildren = ArrayList<SemanticsNode>()
            val beforeChildren = ArrayList<SemanticsNode>()
            val afterChildren = ArrayList<SemanticsNode>()
            node.flattenChildrenInsideTraversalGroup(
                node, visibleChildren, beforeChildren, afterChildren
            )

            // region Tencent Code Modify
            /**
             *  val sortedChildren = node.sortByGeometryGroupings(visibleChildren)
             */
            val zIndexMap = mutableMapOf<Int, Int>()
            visibleChildren.forEachIndexed { index, node ->
                zIndexMap[node.id] = index
            }

            val sortedChildren = node.sortByGeometryGroupings(visibleChildren, zIndexMap = zIndexMap, isIOS = isIOSSortEnable)
            // endregion

            beforeChildren.sortWith(BeyondBoundsComparator(node.isRTL))
            afterChildren.sortWith(BeyondBoundsComparator(node.isRTL))

            val visibleElements = sortedChildren.map { traverseGroup(it, isBeyondBounds) }
            val beforeElements = beforeChildren.map { traverseGroup(it, isBeyondBounds = true) }
            val afterElements = afterChildren.map { traverseGroup(it, isBeyondBounds = true) }

            val containerElements = if (node.isImportantForAccessibility()) {
                listOf(makeSemanticsNode())
            } else {
                emptyList()
            }

            presentIds.add(node.containerKey)
            return createOrUpdateAccessibilityElement(
                node = AccessibilityNode.Container(semanticsNode = node),
                children = beforeElements + containerElements + visibleElements + afterElements,
                frame = frame
            )
        }

        val rootAccessibilityElement = traverseGroup(rootNode, isBeyondBounds = false)

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            val isPresent = it in presentIds

            if (!isPresent) {
                accessibilityDebugLogger?.log { "$it removed" }
                checkNotNull(accessibilityElementsMap[it]).dispose()
            }

            isPresent
        }

        return rootAccessibilityElement
    }

    /**
     * Performs a complete sync of the accessibility tree with the current semantics tree.
     */
    private fun sync(): NodesSyncResult {
        val rootSemanticsNode = owner.unmergedRootSemanticsNode

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        root.element = traverseSemanticsTree(rootSemanticsNode)

        accessibilityDebugLogger?.let {
            debugTraverse(it, view)
        }

        return updateFocusedElement()
    }

    private fun updateFocusedElement(): NodesSyncResult {
        return when (val mode = focusMode) {
            AccessibilityElementFocusMode.None -> {
                NodesSyncResult(newElementToFocus = null, isScreenChange = false)
            }

            is AccessibilityElementFocusMode.KeepFocus -> {
                val focusedElement = UIAccessibilityFocusedElement(null)
                val element = accessibilityElementsMap[mode.key]
                if (element != null && !element.isAccessibilityFrameEmpty()) {
                    NodesSyncResult(element.takeIf { it !== focusedElement }, isScreenChange = false)
                } else if (focusedElement is AccessibilityElement) {
                    val newFocusedElement = root.element?.let { findFocusableElement(it) }

                    focusMode = if (newFocusedElement is AccessibilityElement) {
                        AccessibilityElementFocusMode.KeepFocus(newFocusedElement.key)
                    } else {
                        AccessibilityElementFocusMode.None
                    }

                    NodesSyncResult(newFocusedElement, isScreenChange = true)
                } else {
                    NodesSyncResult(null, isScreenChange = false)
                }
            }
        }
    }

    private fun findClosestElementToRect(rect: Rect): Any? {
        val windowRect = convertToAppWindowCGRect(rect)
        val centerPoint = CGPointMake(
            x = CGRectGetMidX(windowRect),
            y = CGRectGetMidY(windowRect)
        )

        var closestElement: Pair<Double, NSObject>? = null

        fun findElement(element: NSObject, point: CValue<CGPoint>): Any? {
            if (element.isAccessibilityElement) {
                val distanceSQ = minimalDistanceSQ(point, element.accessibilityFrame)
                if (distanceSQ == 0.0) {
                    return element
                } else if (closestElement == null || distanceSQ < closestElement!!.first) {
                    closestElement = distanceSQ to element
                }
            }

            repeat(element.accessibilityElementCount().toInt()) { index ->
                element.accessibilityElementAtIndex(index.toLong())?.let { element ->
                    findElement(element as NSObject, point)?.let {
                        return it
                    }
                }
            }

            return null
        }

        val nativeRoot = root.nativeRoot
        if (nativeRoot != null) {
            findElement(nativeRoot, centerPoint)
        }

        return closestElement?.second
    }

    /**
     * Calculates the squared minimal Euclidean distance between a point and the nearest point on
     * the boundary of a rectangle.
     */
    private fun minimalDistanceSQ(point: CValue<CGPoint>, rect: CValue<CGRect>): Double {
        // Clamp the point to the nearest point on the rectangle
        val clampedX = min(max(point.useContents { x }, CGRectGetMinX(rect)), CGRectGetMaxX(rect))
        val clampedY = min(max(point.useContents { y }, CGRectGetMinY(rect)), CGRectGetMaxY(rect))

        // Return the Euclidean distance between the `point` and the nearest point on the edge
        val dx = clampedX - point.useContents { x }
        val dy = clampedY - point.useContents { y }
        return dx * dx + dy * dy
    }

    fun setFocusTarget(key: AccessibilityElementKey) {
        focusMode = AccessibilityElementFocusMode.KeepFocus(key)
    }

    fun clearFocusTargetIfNeeded(key: AccessibilityElementKey) {
        if (focusMode.targetElementKey == key) {
            focusMode = AccessibilityElementFocusMode.None
        }
    }

    private fun findFocusableElement(node: Any): Any? {
        val nsNode = node as NSObject
        if (nsNode.isAccessibilityElement) {
            return nsNode
        }
        repeat(node.accessibilityElementCount().toInt()) { index ->
            node.accessibilityElementAtIndex(index.toLong())?.let {
                findFocusableElement(it)
            }
        }
        return null
    }

    private fun NodesSyncResult.postNotification() {
        val notificationName = if (isScreenChange) {
            UIAccessibilityScreenChangedNotification
        } else {
            UIAccessibilityLayoutChangedNotification
        }
        UIAccessibilityPostNotification(notificationName, newElementToFocus)
        accessibilityDebugLogger?.log { "UIAccessibilityPostNotification newElementToFocus:$newElementToFocus" }
    }

    private fun refocusKeyboardElementIfNeeded() {
        if (keyboardFocusedElementKey != null) {
            view.window?.let {
                UIFocusSystem.focusSystemForEnvironment(it)?.requestFocusUpdateToEnvironment(it)
            }
            keyboardFocusedElementKey = null
        }
    }
}

private class AccessibilityRoot2(
    val mediator: AccessibilityMediator2,
    onKeyboardPresses: (Set<*>) -> Unit = {}
) {
    var nativeRoot: OVMPAccessibilityRootElement? =
        OVMPAccessibilityRootElement.accessibilityRootElement().apply {
            this.mediatorView = mediator.view
            this.activateAccessibilityIfNeeded = { mediator.activateAccessibilityIfNeeded() }
            this.onScreenReaderActive = { it -> mediator.onScreenReaderActive(it) }
            this.onKeyboardPresses = { it ->
                if (it != null) {
                    onKeyboardPresses(it)
                }
            }
        }

    var element: AccessibilityElement2? = null
        set(value) {
            field = value
            nativeRoot?.bindProxy(value?.nativeAccessibilityProxy)
        }

    fun dispose() {
        nativeRoot?.dispose()
        nativeRoot = null
    }
}

@OptIn(BetaInteropApi::class)
@ExportObjCClass
private class AccessibilityElement2(
    var node: AccessibilityNode,
    children: List<AccessibilityElement2>,
    var nativeRoot: OVMPAccessibilityRootElement? = null
) {
    val key: AccessibilityElementKey get() = node.key

    /**
     * Indicates whether this element is still present in the tree.
     */
    var isAlive = true
        private set

    val nativeAccessibilityProxy = OVMPAccessibilityElementProxy()

    init {
        updatePropertiesAndChildren(node, children, nativeRoot?.cache)
    }

    private fun updatePropertiesAndChildren(node: AccessibilityNode, children: List<AccessibilityElement2>, cache: OVMPAccessibilityElementsCache?) {
        val element = this
        with(nativeAccessibilityProxy) {
            // 1 清空 native proxy 层的数据
            clear()

            // 2. 简单的基本类型的属性，直接绑定
            isAccessibilityElement = node.isAccessibilityElement
            accessibilityTraits = node.accessibilityTraits
            accessibilityContainerType = node.accessibilityContainerType

            // 3. 创建同步的 block，需要的时候，在 block 内拿比较重的数据，比如 string
            sync = node.createSyncBlock(element)

            // 4. 处理孩子节点
            willChangeAccessibilityElements()
            children.fastForEach {
                addAccessibilityElementProxy(it.nativeAccessibilityProxy)
            }
            val interopView = node.accessibilityInteropView
            if (interopView != null) {
                addAccessibilityInteropView(interopView)
            }
            didChangeAccessibilityElements(cache)
        }
    }

    fun setFocusFrame(originX:Double, originY:Double, width:Double, height:Double) {
        nativeAccessibilityProxy.setFocusFrame(originX, originY, width, height)
    }

    fun setAccessibilityFrame(originX:Double, originY:Double, width:Double, height:Double) {
        nativeAccessibilityProxy.setAccessibilityFrame(originX, originY, width, height)
    }

    fun isAccessibilityFrameEmpty() : Boolean {
        return nativeAccessibilityProxy.isAccessibilityFrameEmpty()
    }

    private var updateFocusScheduled = false
    fun setNeedsFocusUpdate() {
        if (updateFocusScheduled) {
            return
        }
        updateFocusScheduled = true
        CoroutineScope(Dispatchers.Main).launch {
            nativeAccessibilityProxy.setNeedsFocusUpdate()
            updateFocusScheduled = false
        }
    }

    fun update(node: AccessibilityNode, children: List<AccessibilityElement2>) {
        assert(key == node.key) {
            "Element should be updated with a node that has the same key as the initial node"
        }
        this.node = node
        nativeAccessibilityProxy.clear()
        updatePropertiesAndChildren(node, children, nativeRoot?.cache)
    }

    fun accessibilityIdentifier2() : String? {
        return node.accessibilityIdentifier
    }

    fun accessibilityLabel2() : String? {
        return node.accessibilityLabel
    }

    fun dispose() {
        isAlive = false
        nativeRoot = null
        nativeAccessibilityProxy.dispose()
    }

    private fun debugContainmentChain() = debugContainmentChain2(this)

    override fun toString(): String {
        var accessibilityString = nativeAccessibilityProxy?.accessibilityIdentifier ?: nativeAccessibilityProxy.accessibilityLabel
        accessibilityString = accessibilityString ?: nativeAccessibilityProxy.accessibilityValue
        accessibilityString = accessibilityString ?: nativeAccessibilityProxy.accessibilityHint
        return "<AccessibilityElement2 key:${nativeAccessibilityProxy.identifierKey} ${accessibilityString}>"
    }

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)
        logger.apply {
            log { "${indent}${key}-${this}" }
            log { "$indent  isAccessibilityElement: ${nativeAccessibilityProxy.isAccessibilityElement}" }
            log { "$indent  containmentChain: ${debugContainmentChain()}" }
            log { "$indent  accessibilityLabel: ${nativeAccessibilityProxy.accessibilityLabel}" }
            log { "$indent  accessibilityValue: ${nativeAccessibilityProxy.accessibilityValue}" }
            log { "$indent  accessibilityTraits: ${nativeAccessibilityProxy.accessibilityTraits}" }
            log { "$indent  accessibilityFrame: ${NSStringFromCGRect(nativeAccessibilityProxy.accessibilityFrame)}" }
            log { "$indent  accessibilityIdentifier: ${nativeAccessibilityProxy.accessibilityIdentifier}" }
            log { "$indent  accessibilityCustomActions: ${nativeAccessibilityProxy.accessibilityCustomActions}" }
        }
    }
}

private fun AccessibilityNode.createSyncBlock(element: AccessibilityElement2): ((
    OVMPAccessibilityElementProxy?,
    OVMPAccessibilityElementSyncType,
    UIAccessibilityScrollDirection
) -> Unit) {
    val node = this
    return { proxy, syncType, scrollDirection ->
        when (syncType) {
            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityLabel -> {
                proxy?.accessibilityLabel = node.accessibilityLabel
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityDidBecomeFocused -> {
                node.accessibilityElementDidBecomeFocused()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityDidLoseFocus -> {
                node.accessibilityElementDidLoseFocus()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeActive -> {
                val accessibilityActivate = node.accessibilityActivate()
                proxy?.accessibilityActivate = accessibilityActivate
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeIncrement -> {
                node.accessibilityIncrement()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeDecrement -> {
                node.accessibilityDecrement()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityScrollResult -> {
                proxy?.accessibilityScrollResult = node.accessibilityScroll(scrollDirection)
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityIdentifier -> {
                proxy?.accessibilityIdentifier = node.accessibilityIdentifier
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityHint -> {
                proxy?.accessibilityHint = node.accessibilityHint
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityCustomActions -> {
                proxy?.accessibilityCustomActions = node.accessibilityCustomActions
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityValue -> {
                proxy?.accessibilityValue = node.accessibilityValue
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypePerformEscape -> {
                proxy?.accessibilityPerformEscape = node.accessibilityPerformEscape()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeCanBecomeFocused -> {
                proxy?.canBecomeFocused = node.canBecomeFocused
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeDidResignFocused -> {
                node.didResignFocused()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeDidBecomeFocused -> {
                node.didBecomeFocused()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeSetNeedsFocusUpdate -> {
                element.setNeedsFocusUpdate()
            }

            OVMPAccessibilityElementSyncType.OVMPAccessibilityElementSyncTypeAccessibilityScrollToVisible -> {
                proxy?.accessibilityScrollToVisible = node.accessibilityScrollToVisible()
            }

            else -> {}
        }
    }
}

private fun debugContainmentChain2(accessibilityObject: Any): String {
    val strings = mutableListOf<String>()

    var currentObject = accessibilityObject as? Any

    while (currentObject != null) {
        when (val constCurrentObject = currentObject) {
            is AccessibilityElement2 -> {
                strings.add(constCurrentObject.key.toString())
                currentObject = constCurrentObject.nativeAccessibilityProxy.debugAccessibilityContainer()
            }

            is OVMPAccessibilityElement -> {
                strings.add(constCurrentObject.debugIdentifierKey().toString())
            }

            is AccessibilityRoot2 -> {
                strings.add("Root")
                currentObject = constCurrentObject.nativeRoot?.accessibilityContainer
            }

            is UIView -> {
                strings.add("View")
                currentObject = null
            }

            else -> {
                throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
            }
        }
    }

    return strings.joinToString(" -> ")
}

// end region