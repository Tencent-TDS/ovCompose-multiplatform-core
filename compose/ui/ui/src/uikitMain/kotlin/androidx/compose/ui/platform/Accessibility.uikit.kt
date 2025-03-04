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

import androidx.compose.ui.geometry.Rect
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
import androidx.compose.ui.semantics.isImportantForAccessibility
import androidx.compose.ui.semantics.sortByGeometryGroupings
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.utils.CMPAccessibilityElement
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.asDpRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.util.fastForEach
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.NativeAccessibilityViewSemanticsKey
import androidx.compose.ui.window.DisplayLinkListener
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.time.measureTime
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
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
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityContainerType
import platform.UIKit.UIAccessibilityContainerTypeNone
import platform.UIKit.UIAccessibilityContainerTypeSemanticGroup
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityFocusedElement
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPageScrolledNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UICoordinateSpaceProtocol
import platform.UIKit.UIEdgeInsetsInsetRect
import platform.UIKit.UIFocusAnimationCoordinator
import platform.UIKit.UIFocusEnvironmentProtocol
import platform.UIKit.UIFocusItemContainerProtocol
import platform.UIKit.UIFocusItemProtocol
import platform.UIKit.UIFocusSystem
import platform.UIKit.UIFocusUpdateContext
import platform.UIKit.UIPressesEvent
import platform.UIKit.UIView
import platform.UIKit.accessibilityElementAtIndex
import platform.UIKit.accessibilityElementCount
import platform.UIKit.accessibilityElements
import platform.UIKit.accessibilityFrame
import platform.UIKit.isAccessibilityElement
import platform.UIKit.setAccessibilityElements
import platform.darwin.NSInteger
import platform.darwin.NSObject

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()

internal sealed interface AccessibilityElementKey {
    val id: Int

    data class Semantics(override val id: Int) : AccessibilityElementKey
    data class Container(override val id: Int) : AccessibilityElementKey
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
    val accessibilityInteropView: InteropWrappingView? get() = null
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

        override val accessibilityInteropView: InteropWrappingView?
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
                log("Focused on:")
                log(cachedConfig)
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

        override val accessibilityContainerType: UIAccessibilityContainerType
            get() = if (semanticsNode.isTraversalGroup) {
                UIAccessibilityContainerTypeSemanticGroup
            } else {
                UIAccessibilityContainerTypeNone
            }
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
            field = value
            field?.setAccessibilityContainer(this)
            mediator.onScreenReaderActive(value != null)
        }

    override fun isAccessibilityElement(): Boolean = false

    override fun accessibilityElementCount(): NSInteger = if (mediator.isEnabled) 1 else 0

    override fun accessibilityElementAtIndex(index: NSInteger): Any? =
        if (mediator.isEnabled) {
            mediator.activateAccessibilityIfNeeded()
            element
        } else {
            null
        }

    override fun accessibilityContainer() = mediator.view

    override fun accessibilityFrame(): CValue<CGRect> =
        mediator.view.convertRect(mediator.view.bounds, toView = null)

    // UIFocusItemContainerProtocol

    override fun coordinateSpace(): UICoordinateSpaceProtocol {
        return mediator.view.window ?: mediator.view
    }

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

    val key: AccessibilityElementKey get() = node.key

    /**
     * Indicates whether this element is still present in the tree.
     */
    var isAlive = true
        private set

    init {
        setAccessibilityElements(children + nodeSemanticsElements())
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
        cachedProperties.clear()
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
        val value = cachedProperties.getOrElse(key) {
            val newValue = block()
            cachedProperties[key] = newValue
            newValue
        }

        return value as T
    }

    override fun accessibilityLabel(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityLabel) {
            node.accessibilityLabel
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

    override fun accessibilityIdentifier(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityIdentifier) {
            node.accessibilityIdentifier
        }

    override fun accessibilityHint(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityHint) {
            node.accessibilityHint
        }

    override fun accessibilityCustomActions(): List<UIAccessibilityCustomAction> =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityCustomActions) {
            node.accessibilityCustomActions
        }

    override fun accessibilityTraits(): UIAccessibilityTraits =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityTraits) {
            node.accessibilityTraits
        }

    override fun accessibilityValue(): String? =
        getOrElse(CachedAccessibilityPropertyKeys.accessibilityValue) {
            node.accessibilityValue
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

    private fun debugContainmentChain() = debugContainmentChain(this)

    fun debugLog(logger: AccessibilityDebugLogger, depth: Int) {
        val indent = " ".repeat(depth * 2)
        logger.apply {
            log("${indent}${key}")
            log("$indent  isAccessibilityElement: ${isAccessibilityElement()}")
            log("$indent  containmentChain: ${debugContainmentChain()}")
            log("$indent  accessibilityLabel: ${accessibilityLabel()}")
            log("$indent  accessibilityValue: ${accessibilityValue()}")
            log("$indent  accessibilityTraits: ${accessibilityTraits()}")
            log("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame())}")
            log("$indent  accessibilityIdentifier: ${accessibilityIdentifier()}")
            log("$indent  accessibilityCustomActions: ${accessibilityCustomActions()}")
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

    override fun frame(): CValue<CGRect> = accessibilityFrame()

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
                is UIView -> return component.window ?: component
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
internal interface AccessibilityDebugLogger {
    /**
     * Logs the given [message].
     */
    fun log(message: Any?)
}

private val accessibilityDebugLogger: AccessibilityDebugLogger? = null
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
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
internal class AccessibilityMediator(
    val view: UIView,
    val owner: SemanticsOwner,
    val coroutineContext: CoroutineContext,
    val performEscape: () -> Boolean,
    onKeyboardPresses: (Set<*>) -> Unit,
    val onScreenReaderActive: (Boolean) -> Unit,
) {
    private var focusMode: AccessibilityElementFocusMode = AccessibilityElementFocusMode.None
        set(value) {
            field = value
            accessibilityDebugLogger?.log("Focus mode: $focusMode")

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

    var isEnabled: Boolean = false
        set(value) {
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

    init {
        accessibilityDebugLogger?.log("AccessibilityMediator for $view created")

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

                if (isEnabled) {
                    if (isAccessibilityActive) {
                        scheduleAccessibilityDisablingAndCleanup()
                        val time = measureTime {
                            sync().postNotification()
                        }
                        accessibilityDebugLogger?.log("AccessibilityMediator.sync took $time")
                    }
                } else if (root.element != null) {
                    refocusKeyboardElementIfNeeded()
                    root.element = null
                    UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, null)
                }
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

            cleanUp()
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

            accessibilityDebugLogger?.log("PageScrolled")

            if (accessibilityElementsMap[focusedNode.semanticsKey] == null) {
                val element = findClosestElementToRect(rect = focusedRectInWindow)
                accessibilityDebugLogger?.log("LayoutChanged, result: $element")

                (element as? AccessibilityElement)?.let {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(element.key)
                }

                UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, element)
            }
        }
    }

    fun onSemanticsChange() {
        accessibilityDebugLogger?.log("onSemanticsChange")
        invalidationChannel.trySend(Unit)
    }

    fun onLayoutChange(nodeId: Int) {
        accessibilityDebugLogger?.log("onLayoutChange (nodeId=$nodeId)")
        invalidationChannel.trySend(Unit)
    }

    fun dispose() {
        job.cancel()
        disableAccessibilityJob?.cancel()

        refocusKeyboardElementIfNeeded()
        view.accessibilityElements = listOf<NSObject>()
        root.onKeyboardPresses = {}

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
            node.children.fastForEach { child ->
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

            val sortedChildren = node.sortByGeometryGroupings(visibleChildren)
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
                accessibilityDebugLogger?.log("$it removed")
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
            debugLogger.log("${indent}View")

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
            debugLogger.log("${indent}Root")
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
