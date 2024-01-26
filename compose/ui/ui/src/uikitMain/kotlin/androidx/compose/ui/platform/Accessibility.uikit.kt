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
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsOwner
import androidx.compose.ui.semantics.SemanticsProperties
import kotlinx.cinterop.CValue
import kotlinx.coroutines.delay
import platform.CoreGraphics.CGRect
import platform.Foundation.NSNotFound
import platform.UIKit.accessibilityElements
import platform.UIKit.isAccessibilityElement
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.toCGRect
import androidx.compose.ui.uikit.utils.*
import androidx.compose.ui.unit.toSize
import kotlin.coroutines.CoroutineContext
import kotlin.time.measureTime
import kotlinx.cinterop.ExportObjCClass
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import platform.CoreGraphics.CGRectMake
import platform.UIKit.NSStringFromCGRect
import platform.UIKit.UIAccessibilityCustomAction
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilityLayoutChangedNotification
import platform.UIKit.UIAccessibilityPostNotification
import platform.UIKit.UIAccessibilityScrollDirection
import platform.UIKit.UIAccessibilityScrollDirectionDown
import platform.UIKit.UIAccessibilityScrollDirectionLeft
import platform.UIKit.UIAccessibilityScrollDirectionRight
import platform.UIKit.UIAccessibilityScrollDirectionUp
import platform.UIKit.UIAccessibilityTraitAdjustable
import platform.UIKit.UIAccessibilityTraitButton
import platform.UIKit.UIAccessibilityTraitHeader
import platform.UIKit.UIAccessibilityTraitImage
import platform.UIKit.UIAccessibilityTraitNone
import platform.UIKit.UIAccessibilityTraitNotEnabled
import platform.UIKit.UIAccessibilityTraitSelected
import platform.UIKit.UIAccessibilityTraitUpdatesFrequently
import platform.UIKit.UIAccessibilityTraits
import platform.UIKit.UIView
import platform.UIKit.accessibilityCustomActions
import platform.darwin.NSInteger
import platform.darwin.NSObject

private val DUMMY_UI_ACCESSIBILITY_CONTAINER = NSObject()

// TODO: Impl for UIKit interop views
// TODO: Impl for text input

private object DebugLogger {
    fun log(message: Any? = null) {
        message?.let {
            println("[a11y] $message")
        } ?: println()
    }
}

/**
 * Represents a projection of the Compose semantics node to the iOS world.
 *
 * The object itself is a node in a generated tree that matches 1-to-1 with the [SemanticsNode]
 * tree. The actual tree that is communicated to iOS accessibility services is synthesized from it
 * lazily in [AccessibilityContainer] class.
 *
 * @param semanticsNode The semantics node with initial data that this element should represent
 *  (can be changed later via [updateWithNewSemanticsNode])
 * @param mediator The mediator that is associated with iOS accessibility tree where this element
 * resides.
 */
@ExportObjCClass
private class AccessibilityElement(
    private var semanticsNode: SemanticsNode,
    private val mediator: AccessibilityMediator,

    // The super call below is needed because this constructor is designated in the Obj-C class,
    // the real container will be resolved dynamically by [accessibilityContainer] and
    // [resolveAccessibilityContainer]
) : CMPAccessibilityElement(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    val semanticsNodeId: Int
        get() = semanticsNode.id

    val hasChildren: Boolean
        get() = children.isNotEmpty()

    val childrenCount: NSInteger
        get() = children.size.toLong()

    var parent: AccessibilityElement? = null
        private set

    var isAlive = true
        private set

    private var children = mutableListOf<AccessibilityElement>()

    /**
     * Constructed lazily if :
     * - The element has children of its own
     * or
     * - The element is representing the root node
     */
    private val synthesizedAccessibilityContainer by lazy {
        AccessibilityContainer(
            wrappedElement = this,
            mediator = mediator
        )
    }

    init {
        update(null, semanticsNode)
    }

    /**
     * Returns accessibility element communicated to iOS Accessibility services for the given [index].
     * Takes a child at [index].
     * If the child has its own children, then the element at the given index is the synthesized container
     * for the child. Otherwise, the element at the given index is the child itself.
     */
    fun childAccessibilityElementAtIndex(index: NSInteger): Any? {
        val i = index.toInt()

        return if (i in children.indices) {
            val child = children[i]

            if (child.hasChildren) {
                return child.accessibilityContainer
            } else {
                child
            }
        } else {
            null
        }
    }

    /**
     * Reverse of [childAccessibilityElementAtIndex]
     * Tries to match the given [element] with the actual hierarchy resolution callback from
     * iOS Accessibility services. If the element is found, returns its index in the children list.
     * Otherwise, returns null.
     */
    fun indexOfChildAccessibilityElement(element: Any): NSInteger? {
        for (index in 0 until children.size) {
            val child = children[index]

            // There are two exclusive cases here.
            // 1. The element is a container, and it's the same as the container of the child.
            // 2. The element is an actual accessibility element, and it's the same as one of the child.
            // The first case is true if the child has children itself, and hence [AccessibilityContainer] was communicated to iOS.
            // The second case is true if the child doesn't have children, and hence itself was communicated to iOS.

            if (child.hasChildren) {
                // accessibilityContainerOfObject retrieves the container of the given element in
                // ObjC via dispatching `accessibilityContainer` to type erased `id` object.
                // In [AccessibilityElement] it will resolve to the synthesized container.
                if (element == child.accessibilityContainer) {
                    return index.toLong()
                }
            } else {
                if (element == child) {
                    return index.toLong()
                }
            }
        }

        return null
    }

    // TODO: remove if unneeded
    fun dispose() {
        check(isAlive) {
            "AccessibilityElement is already disposed"
        }

        isAlive = false
    }

    override fun accessibilityLabel(): String? {
        val editableText = semanticsNode.config.getOrNull(SemanticsProperties.EditableText)?.text

        if (editableText != null) {
            return editableText
        }

        val text =
            semanticsNode.config.getOrNull(SemanticsProperties.Text)?.joinToString("\n") { it.text }

        return text
    }

    override fun accessibilityActivate(): Boolean {
        if (!isAlive || !semanticsNode.isValid) {
            return false
        }

        val onClick = semanticsNode.config.getOrNull(SemanticsActions.OnClick) ?: return false
        val action = onClick.action ?: return false

        return action()
    }

    /**
     * This function is the final one called during the accessibility tree resolution for iOS services
     * and is invoked from underlying Obj-C library. If this node has children, then we return its
     * synthesized container, otherwise we look up the parent and return its container.
     */
    override fun resolveAccessibilityContainer(): Any? {
        if (!isAlive) {
            DebugLogger.log("resolveAccessibilityContainer failed because $semanticsNodeId was removed from the tree")
            return null
        }

        return if (hasChildren || semanticsNodeId == mediator.rootSemanticsNodeId) {
            synthesizedAccessibilityContainer
        } else {
            parent?.accessibilityContainer
        }
    }

    override fun accessibilityElementDidBecomeFocused() {
        super.accessibilityElementDidBecomeFocused()

        DebugLogger.log()
        DebugLogger.log("Focused on:")
        DebugLogger.log(semanticsNode.config)

        if (!isAlive) {
            return
        }

        scrollToIfPossible()
    }

    /**
     * Try to perform a scroll on any ancestor of this element if the element is not fully visible.
     */
    // TODO: still clunky
    // TODO: scroll if the element is last in the scrollable container.
    private fun scrollToIfPossible() {
        val scrollableAncestor = semanticsNode.scrollableByAncestor ?: return
        val scrollableAncestorRect = scrollableAncestor.boundsInWindow

        val unclippedRect = semanticsNode.unclippedBoundsInWindow

        DebugLogger.log("scrollableAncestorRect: $scrollableAncestorRect")
        DebugLogger.log("unclippedRect: $unclippedRect")

        // TODO: consider safe areas?
        // TODO: is RTL working properly?
        if (unclippedRect.top < scrollableAncestorRect.top) {
            // The element is above the screen, scroll up
            parent?.scrollByIfPossible(0f, unclippedRect.top - scrollableAncestorRect.top)
            return
        } else if (unclippedRect.bottom > scrollableAncestorRect.bottom) {
            // The element is below the screen, scroll down
            parent?.scrollByIfPossible(0f, unclippedRect.bottom - scrollableAncestorRect.bottom)
            return
        } else if (unclippedRect.left < scrollableAncestorRect.left) {
            // The element is to the left of the screen, scroll left
            parent?.scrollByIfPossible(unclippedRect.left - scrollableAncestorRect.left, 0f)
            return
        } else if (unclippedRect.right > scrollableAncestorRect.right) {
            // The element is to the right of the screen, scroll right
            parent?.scrollByIfPossible(unclippedRect.right - scrollableAncestorRect.right, 0f)
            return
        }
    }

    private fun scrollByIfPossible(dx: Float, dy: Float) {
        if (!isAlive) {
            return
        }

        // if has scrollBy action, invoke it, otherwise try to scroll the parent
        val action = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action

        if (action != null) {
            action(dx, dy)
        } else {
            parent?.scrollByIfPossible(dx, dy)
        }
    }

    private fun scrollIfPossible(direction: UIAccessibilityScrollDirection): Boolean {
        val (width, height) = semanticsNode.size

        val dimensionScale = 0.5f

        // TODO: post notification about the scroll

        when (direction) {
            UIAccessibilityScrollDirectionUp -> {
                var result = semanticsNode.config.getOrNull(SemanticsActions.PageUp)?.action?.invoke()

                if (result != null) {
                    return result
                }

                result = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0F,
                    -height.toFloat() * dimensionScale
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionDown -> {
                var result = semanticsNode.config.getOrNull(SemanticsActions.PageDown)?.action?.invoke()

                if (result != null) {
                    return result
                }

                result = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    0f,
                    height.toFloat() * dimensionScale
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionLeft -> {
                var result = semanticsNode.config.getOrNull(SemanticsActions.PageLeft)?.action?.invoke()

                if (result != null) {
                    return result
                }

                // TODO: check RTL support
                result = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    -width.toFloat() * dimensionScale,
                    0f,
                )

                if (result != null) {
                    return result
                }
            }

            UIAccessibilityScrollDirectionRight -> {
                var result = semanticsNode.config.getOrNull(SemanticsActions.PageRight)?.action?.invoke()

                if (result != null) {
                    return result
                }

                // TODO: check RTL support
                result = semanticsNode.config.getOrNull(SemanticsActions.ScrollBy)?.action?.invoke(
                    width.toFloat() * dimensionScale,
                    0f,
                )

                if (result != null) {
                    return result
                }
            }

            else -> {
                // TODO: UIAccessibilityScrollDirectionPrevious, UIAccessibilityScrollDirectionNext
            }
        }

        return parent?.scrollIfPossible(direction) ?: false
    }

    override fun accessibilityScroll(direction: UIAccessibilityScrollDirection): Boolean {
        if (!isAlive) {
            return false
        }

        return scrollIfPossible(direction)
    }

    override fun isAccessibilityElement(): Boolean {
        val isInvisibleToUser =
            semanticsNode.config.getOrNull(SemanticsProperties.InvisibleToUser) != null
        val isTraversalGroup =
            semanticsNode.config.getOrNull(SemanticsProperties.IsTraversalGroup) ?: false

        return !(isInvisibleToUser || isTraversalGroup)
    }

    /**
     * Compose doesn't communicate fine-grain changes in semantics tree, thus all changes in the particular
     * persistent object to match the latest resolved SemanticsNode should be done via full-scan of all properties
     * of previous and current SemanticsNode.
     *
     * TODO: is it possible to optimize this? Can we calculate the actual `UIAccessibility` properties
     *   lazily? How should we notify iOS about the changes in the properties without direct changes
     *   in `UIAccessibility` properties (are they KVO-observed by iOS? how can we trigger it?)
     */
    fun updateWithNewSemanticsNode(newSemanticsNode: SemanticsNode) {
        check(semanticsNode.id == newSemanticsNode.id)
        update(semanticsNode, newSemanticsNode)
        semanticsNode = newSemanticsNode
    }

    private fun update(oldNode: SemanticsNode? = null, newNode: SemanticsNode) {
        // TODO: check that the field for the properties that were present in the old node but not in
        //  the new one are cleared

        var testTag: String? = null

        val accessibilityValueStrings = mutableListOf<String>()
        var accessibilityTraits = UIAccessibilityTraitNone

        fun addTrait(trait: UIAccessibilityTraits) {
            accessibilityTraits = accessibilityTraits or trait
        }

        fun <T> getNewValue(key: SemanticsPropertyKey<T>): T = semanticsNode.config[key]

        // Iterate through all semantic properties and map them to values that are expected by iOS Accessibility services for the node with given semantics
        semanticsNode.config.forEach { pair ->
            when (val key = pair.key) {
                // == Properties ==

//                SemanticsProperties.InvisibleToUser -> {
//
//                }

                // Used lazily in [accessibilityScroll]
                /*
                SemanticsProperties.VerticalScrollAxisRange -> {
                }

                SemanticsProperties.HorizontalScrollAxisRange -> {
                }
                */

                SemanticsProperties.LiveRegion -> {
                    // TODO: proper implementation
                    addTrait(UIAccessibilityTraitUpdatesFrequently)
                }

                SemanticsProperties.TestTag -> {
                    testTag = getNewValue(key)
                }

                SemanticsProperties.Disabled -> {
                    addTrait(UIAccessibilityTraitNotEnabled)
                }

                SemanticsProperties.Heading -> {
                    addTrait(UIAccessibilityTraitHeader)
                }

                SemanticsProperties.StateDescription -> {
                    val state = getNewValue(key)
                    accessibilityValueStrings.add(state)
                }

                SemanticsProperties.ToggleableState -> {
                    val state = getNewValue(key)

                    when (state) {
                        ToggleableState.On -> {
                            addTrait(UIAccessibilityTraitSelected)
                            accessibilityValueStrings.add("On")
                        }

                        ToggleableState.Off -> {
                            accessibilityValueStrings.add("Off")
                        }

                        ToggleableState.Indeterminate -> {
                            accessibilityValueStrings.add("Indeterminate")
                        }
                    }
                }

                SemanticsProperties.Role -> {
                    val role = getNewValue(key)

                    when (role) {
                        Role.Button, Role.RadioButton, Role.Checkbox, Role.Switch -> {
                            addTrait(UIAccessibilityTraitButton)
                        }

                        Role.DropdownList -> {
                            addTrait(UIAccessibilityTraitAdjustable)
                        }

                        Role.Image -> {
                            addTrait(UIAccessibilityTraitImage)
                        }
                    }
                }

                // == Actions ==

                // Used lazily in [accessibilityScroll]
                /*
                SemanticsActions.PageUp -> {
                }

                SemanticsActions.PageDown -> {
                }

                SemanticsActions.PageLeft -> {
                }

                SemanticsActions.PageRight -> {
                }

                SemanticsActions.ScrollBy -> {
                }

                SemanticsActions.ScrollToIndex -> {
                }
                */

                SemanticsActions.CustomActions -> {
                    val actions = getNewValue(key)
                    accessibilityCustomActions = actions.map {
                        UIAccessibilityCustomAction(
                            name = it.label,
                            actionHandler = { _ ->
                                it.action.invoke()
                            }
                        )
                    }
                }
            }
        }

        this.accessibilityTraits = accessibilityTraits

        accessibilityIdentifier = testTag ?: "$semanticsNodeId"
        accessibilityFrame = mediator.convertRectToWindowSpaceCGRect(semanticsNode.boundsInWindow)
    }

    private fun removeFromParent() {
        val parent = parent ?: return

        val removed = parent.children.remove(this)
        check(removed) {
            "Corrupted tree. Can't remove child from parent, because it's not present in the parent's children list"
        }

        this.parent = null
    }

    fun removeAllChildren() {
        for (child in children) {
            child.parent = null
        }

        children.clear()
    }

    fun addChild(element: AccessibilityElement) {
        // If child was moved from another parent, remove it from there first
        // Perhaps this is excessive, but I can't prove, that situation where an
        // [AccessibilityElement] is contained in multiple parents is impossible, and that it won't
        // lead to issues
        element.removeFromParent()

        children.add(element)
        element.parent = this@AccessibilityElement
    }

    fun debugPrint(depth: Int) {
        val indent = " ".repeat(depth * 2)

        val container = resolveAccessibilityContainer() as AccessibilityContainer
        val indexOfSelf = container.indexOfAccessibilityElement(this)

        check(indexOfSelf != NSNotFound)
        check(container.accessibilityElementAtIndex(indexOfSelf) == this)

        DebugLogger.log("${indent}AccessibilityElement_$semanticsNodeId")
        DebugLogger.log("$indent  containmentChain: ${debugContainmentChain(this)}")
        DebugLogger.log("$indent  isAccessibilityElement: $isAccessibilityElement")
        DebugLogger.log("$indent  accessibilityLabel: $accessibilityLabel")
        DebugLogger.log("$indent  accessibilityValue: $accessibilityValue")
        DebugLogger.log("$indent  accessibilityTraits: $accessibilityTraits")
        DebugLogger.log("$indent  accessibilityFrame: ${NSStringFromCGRect(accessibilityFrame)}")
        DebugLogger.log("$indent  accessibilityIdentifier: $accessibilityIdentifier")
        DebugLogger.log("$indent  accessibilityCustomActions: $accessibilityCustomActions")
    }
}

/**
 * UIAccessibilityElement can't be a container and an element at the same time.
 * If [isAccessibilityElement] is true, iOS accessibility services won't access the object
 * UIAccessibilityContainer methods.
 * Thus, semantics tree like
 * ```
 * SemanticsNode_A
 *     SemanticsNode_B
 *         SemanticsNode_C
 * ```
 * Is expected by iOS Accessibility services to be represented as:
 * ```
 * AccessibilityContainer_A
 *     AccessibilityElement_A
 *     AccessibilityContainer_B
 *         AccessibilityElement_B
 *         AccessibilityElement_C
 * ```
 * The actual internal representation of the tree is:
 * ```
 * AccessibilityElement_A
 *   AccessibilityElement_B
 *      AccessibilityElement_C
 * ```
 * But the object we put into the accessibility root set is the synthesized [AccessibilityContainer]
 * for AccessibilityElement_A. The methods that are be called from iOS Accessibility services will
 * lazily resolve the hierarchy from the internal one to expected.
 *
 * This is needed, because the actual [SemanticsNode]s can be inserted and removed dynamically, so building
 * the whole container hierarchy in advance and maintaining it proactively will make the code even more
 * hard to follow than it is now.
 *
 * This implementation is inspired by Flutter's
 * https://github.com/flutter/engine/blob/main/shell/platform/darwin/ios/framework/Source/SemanticsObject.h
 *
 */
@ExportObjCClass
private class AccessibilityContainer(
    /**
     * The element wrapped by this container
     */
    private val wrappedElement: AccessibilityElement,
    private val mediator: AccessibilityMediator,

    // The super call below is needed because this constructor is designated in the Obj-C class,
    // the real parent container will be resolved dynamically by [accessibilityContainer]
) : CMPAccessibilityContainer(DUMMY_UI_ACCESSIBILITY_CONTAINER) {
    val semanticsNodeId by wrappedElement::semanticsNodeId
    private val isAlive by wrappedElement::isAlive

    /**
     * This function will be called by iOS Accessibility services to traverse the hierarchy of all
     * accessibility elements starting with the root one.
     *
     * The zero element is always the element wrapped by this container due to the restriction of
     * an object not being able to be a container and an element at the same time.
     */
    override fun accessibilityElementAtIndex(index: NSInteger): Any? {
        if (!isAlive) {
            DebugLogger.log("accessibilityElementAtIndex(NSInteger) called after $semanticsNodeId was removed from the tree")
            return null
        }

        if (index == 0L) {
            return wrappedElement
        }

        return wrappedElement.childAccessibilityElementAtIndex(index - 1)
    }

    override fun accessibilityFrame(): CValue<CGRect> {
        if (!isAlive) {
            return CGRectMake(0.0, 0.0, 0.0, 0.0)
        }

        // Same as wrapped element
        // iOS makes children of a container unreachable, if their frame is outside of
        // the container's frame
        return wrappedElement.accessibilityFrame
    }

    /**
     * The number of elements in the container:
     * The wrapped element itself + the number of children
     */
    override fun accessibilityElementCount(): NSInteger {
        if (!isAlive) {
            DebugLogger.log("accessibilityElementCount() called after $semanticsNodeId was removed from the tree")
            return 0
        }

        return wrappedElement.childrenCount + 1
    }

    /**
     * Reverse lookup of [accessibilityElementAtIndex]
     */
    override fun indexOfAccessibilityElement(element: Any): NSInteger {
        if (!isAlive) {
            DebugLogger.log("indexOfAccessibilityElement(Any) called after $semanticsNodeId was removed from the tree")
            return NSNotFound
        }

        if (element == wrappedElement) {
            return 0
        }

        return wrappedElement.indexOfChildAccessibilityElement(element)?.let { index ->
            index + 1
        } ?: NSNotFound
    }

    override fun accessibilityContainer(): Any? {
        if (!isAlive) {
            DebugLogger.log("accessibilityContainer() called after $semanticsNodeId was removed from the tree")
            return null
        }

        return if (semanticsNodeId == mediator.rootSemanticsNodeId) {
            mediator.view
        } else {
            wrappedElement.parent?.accessibilityContainer
        }
    }

    fun debugPrint(depth: Int) {
        val indent = " ".repeat(depth * 2)
        DebugLogger.log("${indent}AccessibilityContainer_${semanticsNodeId}")
    }
}

private sealed interface NodesSyncResult {
    object NoChanges : NodesSyncResult
    data class Success(val newElementToFocus: Any?) : NodesSyncResult
}

/**
 * A class responsible for mediating between the tree of specific SemanticsOwner and the iOS accessibility tree.
 */
internal class AccessibilityMediator(
    val view: UIView,
    private val owner: SemanticsOwner,
    coroutineContext: CoroutineContext,
    private val checkIfForcedToSyncAccessibility: () -> Boolean,
) {
    private var isAlive = true

    var rootSemanticsNodeId: Int = -1

    /**
     * A value of true indicates that the Compose accessible tree is dirty, meaning that compose
     * semantics tree was modified since last sync, false otherwise.
     */
    private var isCurrentComposeAccessibleTreeDirty = false

    /**
     * Job to cancel tree syncing when the mediator is disposed.
     */
    private val job = Job()

    /**
     * CoroutineScope to launch the tree syncing job on.
     */
    private val coroutineScope = CoroutineScope(coroutineContext + job)

    /**
     * A map of all [SemanticsNode.id] currently present in the tree to corresponding
     * [AccessibilityElement].
     */
    private val accessibilityElementsMap = mutableMapOf<Int, AccessibilityElement>()

    init {
        println("AccessibilityMediator created")
        val updateIntervalMillis = 50L
        // TODO: this approach was copied from desktop implementation, obviously it has a [updateIntervalMillis] lag
        //  between the actual change in the semantics tree and the change in the accessibility tree.
        //  should we use some other approach?
        coroutineScope.launch {
            while (isAlive) {
                var result: NodesSyncResult

                if (UIAccessibilityIsVoiceOverRunning() || checkIfForcedToSyncAccessibility()) {
                    val time = measureTime {
                        result = syncNodes()
                    }

                    when (val immutableResult = result) {
                        is NodesSyncResult.NoChanges -> {
                            // Do nothing
                        }

                        is NodesSyncResult.Success -> {
                            DebugLogger.log("syncNodes took $time")
                            UIAccessibilityPostNotification(UIAccessibilityLayoutChangedNotification, immutableResult.newElementToFocus)
                        }
                    }
                }

                delay(updateIntervalMillis)
            }
        }
    }

    fun onSemanticsChange() {
        DebugLogger.log("onSemanticsChange")
        isCurrentComposeAccessibleTreeDirty = true
    }

    fun convertRectToWindowSpaceCGRect(rect: Rect): CValue<CGRect> {
        val window = view.window ?: return CGRectMake(0.0, 0.0, 0.0, 0.0)

        val localSpaceCGRect = rect.toCGRect(window.screen.scale)
        return window.convertRect(localSpaceCGRect, fromView = view)
    }

    fun dispose() {
        check(isAlive) { "AccessibilityMediator is already disposed" }

        job.cancel()
        isAlive = false
        view.accessibilityElements = null

        for (element in accessibilityElementsMap.values) {
            element.dispose()
        }
    }

    private fun createOrUpdateAccessibilityElementForSemanticsNode(node: SemanticsNode): AccessibilityElement {
        val element = accessibilityElementsMap[node.id]

        if (element != null) {
            element.updateWithNewSemanticsNode(node)
            return element
        }

        val newElement = AccessibilityElement(
            semanticsNode = node,
            mediator = this
        )

        accessibilityElementsMap[node.id] = newElement

        return newElement
    }

    /**
     * Traverses semantics tree starting from rootNode and returns an accessibility object which will
     * be put into iOS view's [accessibilityElements] property.
     *
     * Inserts new elements to [accessibilityElementsMap], updates the old ones, and removes the elements
     * that are not present in the tree anymore.
     */
    private fun traverseSemanticsTree(rootNode: SemanticsNode): Any {
        // TODO: should we move [presentIds] to the class scope to avoid reallocation?
        val presentIds = mutableSetOf<Int>()

        fun traverseSemanticsNode(node: SemanticsNode): AccessibilityElement {
            presentIds.add(node.id)
            val element = createOrUpdateAccessibilityElementForSemanticsNode(node)

            element.removeAllChildren()
            val childSemanticsNodesInAccessibilityOrder = node
                .replacedChildren
                .filter {
                    it.isValid
                }
                .sortedByAccesibilityOrder()

            for (childNode in childSemanticsNodesInAccessibilityOrder) {
                val childElement = traverseSemanticsNode(childNode)
                element.addChild(childElement)
            }

            return element
        }

        val rootAccessibilityElement = traverseSemanticsNode(rootNode)

        // Filter out [AccessibilityElement] in [accessibilityElementsMap] that are not present in the tree anymore
        accessibilityElementsMap.keys.retainAll {
            val isPresent = it in presentIds

            if (!isPresent) {
                DebugLogger.log("$it removed")
                checkNotNull(accessibilityElementsMap[it]).dispose()
            }

            isPresent
        }

        return checkNotNull(rootAccessibilityElement.resolveAccessibilityContainer()) {
            "Root element must always have an enclosing container"
        }
    }

    /**
     * Syncs the accessibility tree with the current semantics tree.
     * TODO: Does a full tree traversal on every sync. Explore new Google solution in 1.6, that should
     *   perform affected subtree traversal instead.
     */
    private fun syncNodes(): NodesSyncResult {
        // TODO: investigate what happens if the user has an accessibility focus on the element that
        //  is removed from the tree:
        //  - Does it use the index path of containers traversal to restore the focus?
        //  - Does it use the accessibility identifier of the element to restore the focus?
        //  - Does it hold the reference to the focused element? Should we
        //      take some action to reset focus on the element, that got deleted in such case?

        // TODO: investigate what needs to be done to reflect that this hiearchy is probably covered
        //   by overlay/popup/dialogue

        if (!isCurrentComposeAccessibleTreeDirty) {
            return NodesSyncResult.NoChanges
        }

        val rootSemanticsNode = owner.rootSemanticsNode
        rootSemanticsNodeId = rootSemanticsNode.id

        // Copied from desktop implementation, why is it there? 🤔
        if (!rootSemanticsNode.layoutNode.isPlaced) {
            return NodesSyncResult.NoChanges
        }

        DebugLogger.log("syncNodes")
        isCurrentComposeAccessibleTreeDirty = false

        check(!view.isAccessibilityElement) {
            "Root view must not be an accessibility element"
        }

        view.accessibilityElements = listOf(
            traverseSemanticsTree(rootSemanticsNode)
        )

        debugTraverse(view)
        // TODO: return refocused element if the old focus is not present in the new tree
        return NodesSyncResult.Success(null)
    }
}

/**
 * Traverse the accessibility tree starting from [accessibilityObject] using the same(assumed) logic
 * as iOS Accessibility services, and prints its debug data.
 */
private fun debugTraverse(accessibilityObject: Any, depth: Int = 0) {
    val indent = " ".repeat(depth * 2)

    when (accessibilityObject) {
        is UIView -> {
            DebugLogger.log("${indent}View")

            accessibilityObject.accessibilityElements?.let { elements ->
                for (element in elements) {
                    element?.let {
                        debugTraverse(element, depth + 1)
                    }
                }
            }
        }

        is AccessibilityElement -> {
            accessibilityObject.debugPrint(depth)
        }

        is AccessibilityContainer -> {
            accessibilityObject.debugPrint(depth)

            val count = accessibilityObject.accessibilityElementCount()
            for (index in 0 until count) {
                val element = accessibilityObject.accessibilityElementAtIndex(index)
                element?.let {
                    debugTraverse(element, depth + 1)
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
                currentObject = constCurrentObject.resolveAccessibilityContainer()
            }

            is UIView -> {
                strings.add("View")
                currentObject = null
            }

            is AccessibilityContainer -> {
                strings.add("AccessibilityContainer_${constCurrentObject.semanticsNodeId}")
                currentObject = constCurrentObject.accessibilityContainer()
            }

            else -> {
                throw IllegalStateException("Unexpected accessibility object type: ${accessibilityObject::class}")
            }
        }
    }

    return strings.joinToString(" -> ")
}

/**
 * Sort the elements in their visual order using their bounds:
 * - from top to bottom,
 * - from left to right // TODO: consider RTL layout
 *
 * The sort is needed because [SemanticsNode.replacedChildren] order doesn't match the
 * expected order of the children in the accessibility tree.
 *
 * TODO: investigate if it's a bug, or some assumptions about the order are wrong.
 */
private fun List<SemanticsNode>.sortedByAccesibilityOrder(): List<SemanticsNode> {
    return sortedWith { lhs, rhs ->
        val result = lhs.boundsInWindow.topLeft.y.compareTo(rhs.boundsInWindow.topLeft.y)

        if (result == 0) {
            lhs.boundsInWindow.topLeft.x.compareTo(rhs.boundsInWindow.topLeft.x)
        } else {
            result
        }
    }
}

private val SemanticsNode.unclippedBoundsInWindow: Rect
    get() = Rect(positionInWindow, size.toSize())

private val SemanticsNode.isValid: Boolean
    get() = layoutNode.isPlaced && layoutNode.isAttached
/**
 * Closest ancestor that has [SemanticsActions.ScrollBy] action
 */
private val SemanticsNode.scrollableByAncestor: SemanticsNode?
    get() {
        var current = parent

        while (current != null) {
            if (current.config.getOrNull(SemanticsActions.ScrollBy) != null) {
                return current
            }

            current = current.parent
        }

        return null
    }