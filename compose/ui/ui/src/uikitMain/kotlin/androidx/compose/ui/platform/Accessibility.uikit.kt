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

import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.platform.accessibility.AccessibilityScrollEventResult
import androidx.compose.ui.platform.accessibility.accessibilityCustomActions
import androidx.compose.ui.platform.accessibility.accessibilityLabel
import androidx.compose.ui.platform.accessibility.accessibilityTraits
import androidx.compose.ui.platform.accessibility.accessibilityValue
import androidx.compose.ui.platform.accessibility.canBeAccessibilityElement
import androidx.compose.ui.platform.accessibility.isRTL
import androidx.compose.ui.platform.accessibility.isScreenReaderFocusable
import androidx.compose.ui.platform.accessibility.scrollIfPossible
import androidx.compose.ui.platform.accessibility.scrollToCenterRectIfNeeded
import androidx.compose.ui.platform.accessibility.unclippedBoundsInWindow
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.uikit.density
import androidx.compose.ui.uikit.toDpRect
import androidx.compose.ui.uikit.utils.CMPAccessibilityElement
import androidx.compose.ui.unit.asCGRect
import androidx.compose.ui.unit.toDpRect
import androidx.compose.ui.unit.toRect
import androidx.compose.ui.viewinterop.InteropWrappingView
import androidx.compose.ui.viewinterop.NativeAccessibilityViewSemanticsKey
import kotlin.coroutines.CoroutineContext
import kotlin.math.max
import kotlin.math.min
import kotlin.time.measureTime
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGPointMake
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectGetMaxX
import platform.CoreGraphics.CGRectGetMaxY
import platform.CoreGraphics.CGRectGetMidX
import platform.CoreGraphics.CGRectGetMidY
import platform.CoreGraphics.CGRectGetMinX
import platform.CoreGraphics.CGRectGetMinY
import platform.CoreGraphics.CGRectIsEmpty
import platform.Foundation.NSNotFound
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityFocusedElement
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPageScrolledNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScreenChangedNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIEdgeInsetsInsetRect
import platform.UIKit.UIView
import platform.UIKit.accessibilityElementAtIndex
import platform.UIKit.accessibilityElementCount
import platform.UIKit.accessibilityElements
import platform.UIKit.accessibilityFrame
import platform.UIKit.isAccessibilityElement
import platform.darwin.NSInteger
import platform.darwin.NSObject

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()

/**
 * Enum class representing different kinds of accessibility invalidation.
 */
private enum class SemanticsTreeInvalidationKind {
    /**
     * The tree was changed, need to recompute the whole tree.
     */
    COMPLETE,

    /**
     * Only bounds of the nodes were changed, need to recompute the bounds of the affected subtrees.
     */
    BOUNDS
}

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
    val accessibilityFrame: CValue<CGRect>

    val accessibilityLabel: String? get() = null
    val accessibilityHint: String? get() = null
    val accessibilityValue: String? get() = null
    val accessibilityTraits: UIAccessibilityTraits get() = UIAccessibilityTraitNone
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

    /**
     * Represents a projection of the Compose semantics node to the iOS world.
     * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode].
     */
    class Semantics(
        private val semanticsNode: SemanticsNode,
        private val mediator: AccessibilityMediator,
        private val ignoreSemanticChildren: Boolean = false
    ) : AccessibilityNode {
        private val cachedConfig = semanticsNode.copyWithMergingEnabled().config

        override val key: AccessibilityElementKey
            get() = semanticsNode.semanticsKey

        override val isAccessibilityElement: Boolean
            get() = semanticsNode.isScreenReaderFocusable()

        override val accessibilityFrame: CValue<CGRect>
            get() = mediator.convertToAppWindowCGRect(semanticsNode.boundsInWindow)

        override val accessibilityInteropView: InteropWrappingView?
            get() = if (ignoreSemanticChildren) {
                null
            } else {
                cachedConfig.getOrNull(NativeAccessibilityViewSemanticsKey)
            }

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
            mediator.debugLogger?.apply {
                log(null)
                log("Focused on:")
                log(cachedConfig)
            }
            mediator.setFocusTarget(key)
        }

        override fun accessibilityElementDidLoseFocus() {
            mediator.clearFocusTargetIfNeeded(key)
        }

        override fun accessibilityScrollToVisible(): Boolean {
            return semanticsNode.parent?.scrollToCenterRectIfNeeded(
                rect = semanticsNode.unclippedBoundsInWindow,
                safeAreaRectInWindow = mediator.safeAreaRectInWindow
            ) ?: false
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
    }

    /**
     * Unlike Android, UIAccessibilityElement can't be a container and an element at the same time.
     * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
     * UIAccessibilityContainer methods. To implement this behavior, flatting the container node
     * with all its children. [Container] is used to indicate element that contains container
     * semantic node with all its children.
     */
    class Container(
        containerNode: SemanticsNode,
        mediator: AccessibilityMediator
    ) : AccessibilityNode {
        override val key: AccessibilityElementKey = containerNode.containerKey

        override val isAccessibilityElement = false

        override val accessibilityFrame: CValue<CGRect> =
            mediator.convertToAppWindowCGRect(containerNode.boundsInWindow)
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
private class AccessibilityElement(
    private var node: AccessibilityNode,
    children: List<AccessibilityElement>
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    /**
     * A cache for the properties that are computed from the [SemanticsNode.config] and are communicated
     * to iOS Accessibility services.
     */
    private val cachedProperties = mutableMapOf<CachedAccessibilityPropertyKey<*>, Any?>()

    private var allChildren = children + nodeSemanticsElements()

    val key: AccessibilityElementKey get() = node.key

    var parent: Any? = null

    /**
     * Indicates whether this element is still present in the tree.
     */
    var isAlive = true
        private set

    init {
        children.forEach { it.parent = this }
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
        this.parent = null
        this.node = node
        children.forEach { it.parent = this }
        this.allChildren = children + nodeSemanticsElements()
        this.cachedProperties.clear()
    }

    fun dispose() {
        check(isAlive) {
            "AccessibilityElement is already disposed"
        }

        isAlive = false
        parent = null
        allChildren = emptyList()
        cachedProperties.clear()
    }

    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        val i = index.toInt()
        if (i in allChildren.indices) {
            return allChildren[i]
        }
        return null
    }

    override fun accessibilityElementCount(): NSInteger {
        return allChildren.count().toLong()
    }

    override fun indexOfAccessibilityElement(element: Any): NSInteger {
        val index = allChildren.indexOf(element).toLong()
        return index.takeIf { it >= 0 } ?: NSNotFound
    }

    override fun accessibilityContainer(): Any? = parent

    /**
     * Returns the value for the given [key] from the cache if it's present, otherwise computes the
     * value using the given [block] and caches it.
     */
    @Suppress("UNCHECKED_CAST") // cast is safe because the set value is constrained by the key T
    private inline fun <T>getOrElse(key: CachedAccessibilityPropertyKey<T>, crossinline block: () -> T): T {
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
        return node.isAccessibilityElement
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

    override fun accessibilityFrame(): CValue<CGRect> =
        // No need to cache accessibility frame because it invalidates much frequently
        // then requests by the iOS Accessibility
        node.accessibilityFrame

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
}

private class NodesSyncResult(
    val newElementToFocus: Any?,
    val isScreenChange: Boolean
)

/**
 * A sealed class that represents the options for syncing the Compose SemanticsNode tree with the iOS UIAccessibility tree.
 */
@ExperimentalComposeApi
enum class AccessibilitySyncOptions {
    /**
     * Never sync the tree.
     */
    Never,

    /**
     * Sync the tree only when the accessibility services are running.
     */
    WhenRequiredByAccessibilityServices,

    /**
     * Always sync the tree, can be quite handy for debugging and testing.
     * Be aware that there is a significant overhead associated with doing it that can degrade
     * the visual performance of the app.
     */
    Always
}

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
// private val accessibilityDebugLogger = object : AccessibilityDebugLogger {
//     override fun log(message: Any?) {
//         if (message == null) {
//             println()
//         } else {
//             println("[a11y]: $message")
//         }
//     }
// }

private sealed interface AccessibilityElementFocusMode {
    val targetElementKey: AccessibilityElementKey?

    /**
     * Do not change focus. Notifies about significant changes on a screen to let iOS Accessibility
     * decide about the next focused element.
     */
    data object Initial : AccessibilityElementFocusMode {
        override val targetElementKey: AccessibilityElementKey? = null
    }

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
    coroutineContext: CoroutineContext,
    val performEscape: () -> Boolean
): NSObject() {

    private var focusMode: AccessibilityElementFocusMode = AccessibilityElementFocusMode.Initial
        set(value) {
            field = value
            debugLogger?.log("Focus mode: $focusMode")
        }

    /**
     * The kind of invalidation that determines what kind of logic will be executed in the next sync.
     * `COMPLETE` invalidation means that the whole tree should be recomputed, `BOUNDS` means that only
     * the bounds of the nodes should be recomputed. A list of changed performed by `BOUNDS` path
     * is a strict subset of `COMPLETE`, so in the end of sync it will be reset to `BOUNDS`.
     * Executing sync assumes that at least one kind of invalidation happened, if it was triggered
     * by [onSemanticsChange] it will be automatically promoted to `COMPLETE`.
     */
    private var invalidationKind = SemanticsTreeInvalidationKind.COMPLETE

    /**
     * A set of node ids that had their bounds invalidated after the last sync.
     */
    private val invalidationChannel = Channel<Unit>(1, onBufferOverflow = BufferOverflow.DROP_LATEST)

    /**
     * Remembered [AccessibilityDebugLogger] after last sync, if logging is enabled according to
     * [AccessibilitySyncOptions].
     */
    var debugLogger: AccessibilityDebugLogger? = null
        private set

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    private var rootElement: AccessibilityElement? = null

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
            }
        }

    val safeAreaRectInWindow: Rect get() {
        val rectInWindow = view.convertRect(
            rect = UIEdgeInsetsInsetRect(view.bounds, view.safeAreaInsets),
            toView = null
        )
        return rectInWindow.toDpRect().toRect(view.density)
    }

    init {
        accessibilityDebugLogger?.log("AccessibilityMediator for $view created")

        view.accessibilityElements = listOf<NSObject>()
        coroutineScope.launch {
            // The main loop that listens for invalidations and performs the tree syncing
            // Will exit on CancellationException from within await on `invalidationChannel.receive()`
            // when [job] is cancelled
            while (true) {
                invalidationChannel.receive()

                while (invalidationChannel.tryReceive().isSuccess) {
                    // Do nothing, just consume the channel
                    // Workaround for the channel buffering two invalidations despite the capacity of 1
                }

                debugLogger = accessibilityDebugLogger.takeIf { isEnabled }

                if (isEnabled) {
                    val result: NodesSyncResult

                    val time = measureTime {
                        result = sync(invalidationKind)
                    }

                    debugLogger?.log("AccessibilityMediator.sync took $time")
                    debugLogger?.log("LayoutChanged, newElementToFocus: ${result.newElementToFocus}")

                    val notificationName = if (result.isScreenChange) {
                        UIAccessibilityScreenChangedNotification
                    } else {
                        UIAccessibilityLayoutChangedNotification
                    }
                    UIAccessibilityPostNotification(notificationName, result.newElementToFocus)
                } else {
                    if (view.accessibilityElements?.isEmpty() != true) {
                        view.accessibilityElements = listOf<NSObject>()
                        UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, null)
                    }
                }

                invalidationKind = SemanticsTreeInvalidationKind.BOUNDS
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val hasPendingInvalidations: Boolean get() = !invalidationChannel.isEmpty

    fun convertToAppWindowCGRect(rect: Rect): CValue<CGRect> {
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

            debugLogger?.log("PageScrolled")

            if (accessibilityElementsMap[focusedNode.semanticsKey] == null) {
                val element = findClosestElementToRect(rect = focusedRectInWindow)
                debugLogger?.log("LayoutChanged, result: $element")

                (element as? AccessibilityElement)?.let {
                    focusMode = AccessibilityElementFocusMode.KeepFocus(element.key)
                }

                UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, element)
            }
        }
    }

    fun onSemanticsChange() {
        debugLogger?.log("onSemanticsChange")

        invalidationKind = SemanticsTreeInvalidationKind.COMPLETE
        invalidationChannel.trySend(Unit)
    }

    fun onLayoutChange(nodeId: Int) {
        debugLogger?.log("onLayoutChange (nodeId=$nodeId)")

        // TODO: Properly implement layout invalidation, taking into account that semantics
        //  can also change after the `onLayoutChange` event.
        if (accessibilityElementsMap[AccessibilityElementKey.Semantics(nodeId)] == null) {
            // Forcing tree recalculation when a node with unknown nodeId occurred.
            invalidationKind = SemanticsTreeInvalidationKind.COMPLETE
        }

        invalidationChannel.trySend(Unit)
    }

    fun dispose() {
        job.cancel()
        view.accessibilityElements = listOf<NSObject>()

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }
        rootElement = null
        accessibilityElementsMap.clear()
    }

    private fun createOrUpdateAccessibilityElement(
        node: AccessibilityNode,
        children: List<AccessibilityElement>
    ): AccessibilityElement {
        accessibilityElementsMap[node.key]?.let {
            it.update(node = node, children = children)
            return it
        }
        return AccessibilityElement(node = node, children = children).also {
            accessibilityElementsMap[node.key] = it
        }
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

        fun traverseSemanticsNode(node: SemanticsNode): AccessibilityElement {
            presentIds.add(node.semanticsKey)
            val semanticsChildren = node
                .replacedChildren
                .filter { it.isValid }
                .sortedByAccessibilityOrder(node.isRTL)

            val children = semanticsChildren.map(::traverseSemanticsNode)

            return if (node.canBeAccessibilityElement() && semanticsChildren.isNotEmpty()) {
                presentIds.add(node.containerKey)

                // Unlike Android, iOS Accessibility engine does not traverse inside accessibility
                // elements which marked as accessible (focusable).
                // To align behavior, flatting the node with all its children and arranging them
                // inside the synthetic container node.
                val containerElement = createOrUpdateAccessibilityElement(
                    node = AccessibilityNode.Semantics(
                        semanticsNode = node,
                        mediator = this,
                        ignoreSemanticChildren = true
                    ),
                    children = emptyList()
                )
                createOrUpdateAccessibilityElement(
                    node = AccessibilityNode.Container(containerNode = node, mediator = this),
                    children = listOf(containerElement) + children
                )
            } else {
                createOrUpdateAccessibilityElement(
                    node = AccessibilityNode.Semantics(semanticsNode = node, mediator = this),
                    children = children
                )
            }
        }

        val rootAccessibilityElement = traverseSemanticsNode(rootNode)

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            val isPresent = it in presentIds

            if (!isPresent) {
                debugLogger?.log("$it removed")
                checkNotNull(accessibilityElementsMap[it]).dispose()
            }

            isPresent
        }

        rootAccessibilityElement.parent = view
        return rootAccessibilityElement
    }

    /**
     * Syncs the accessibility tree with the current semantics tree.
     */
    private fun sync(invalidationKind: SemanticsTreeInvalidationKind): NodesSyncResult {
        return when (invalidationKind) {
            SemanticsTreeInvalidationKind.COMPLETE -> completeSync()
            SemanticsTreeInvalidationKind.BOUNDS -> updateFocusedElement()
        }
    }

    /**
     * Performs a complete sync of the accessibility tree with the current semantics tree.
     */
    private fun completeSync(): NodesSyncResult {
        val rootSemanticsNode = owner.unmergedRootSemanticsNode

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        rootElement = traverseSemanticsTree(rootSemanticsNode)
        view.accessibilityElements = listOfNotNull(rootElement)

        debugLogger?.let {
            debugTraverse(it, view)
        }

        return updateFocusedElement()
    }

    private fun updateFocusedElement(): NodesSyncResult {
        return when (val mode = focusMode) {
            AccessibilityElementFocusMode.Initial -> {
                focusMode = AccessibilityElementFocusMode.None
                NodesSyncResult(newElementToFocus = null, isScreenChange = true)
            }

            AccessibilityElementFocusMode.None -> {
                NodesSyncResult(newElementToFocus = null, isScreenChange = false)
            }

            is AccessibilityElementFocusMode.KeepFocus -> {
                val focusedElement = UIAccessibilityFocusedElement(null)
                val element = accessibilityElementsMap[mode.key]
                if (element != null && !CGRectIsEmpty(element.accessibilityFrame())) {
                    NodesSyncResult(element.takeIf { it !== focusedElement }, isScreenChange = false)
                } else if (focusedElement is AccessibilityElement) {
                    val newFocusedElement = rootElement?.let { findFocusableElement(it) }

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

        rootElement?.let {
            @Suppress("CAST_NEVER_SUCCEEDS")
            findElement(it as NSObject, centerPoint)
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
                currentObject = constCurrentObject.parent
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
 * Sort the elements in their visual order using their bounds:
 * - from top to bottom,
 * - from left to right or from right to left, depending on language direction
 *
 * The sort is needed because [SemanticsNode.replacedChildren] order doesn't match the
 * expected order of the children in the accessibility tree.
 *
 * TODO: investigate if it's a bug, or some assumptions about the order are wrong.
 */
private fun List<SemanticsNode>.sortedByAccessibilityOrder(isRTL: Boolean): List<SemanticsNode> {
    return sortedWith { lhs, rhs ->
        val result = lhs.boundsInWindow.topLeft.y.compareTo(rhs.boundsInWindow.topLeft.y)

        if (result == 0) {
            lhs.boundsInWindow.topLeft.x.compareTo(rhs.boundsInWindow.topLeft.x).let {
                if (isRTL) -it else it
            }
        } else {
            result
        }
    }
}

/**
 * Returns true if corresponding [LayoutNode] is placed and attached, false otherwise.
 */
private val SemanticsNode.isValid: Boolean
    get() = layoutNode.isPlaced && layoutNode.isAttached
