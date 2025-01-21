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
package androidx.compose.ui.platform

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.view.View
import android.view.View.FOCUS_BACKWARD
import android.view.View.FOCUS_FORWARD
import android.view.ViewGroup
import androidx.collection.MutableObjectList
import androidx.collection.ObjectList
import androidx.collection.mutableObjectIntMapOf
import androidx.collection.mutableScatterMapOf
import androidx.collection.mutableScatterSetOf
import androidx.core.view.isVisible
import java.util.Collections

/**
 * On devices before [Build.VERSION_CODES.O], [FocusFinder] orders Views incorrectly. This
 * implementation uses the current [FocusFinder] behavior, ordering Views correctly for
 * one-dimensional focus searches.
 *
 * This is copied and simplified from FocusFinder's source. There may be some code that doesn't look
 * quite right in Kotlin as it was copy/pasted with auto-translation.
 */
internal class FocusFinderCompat {
    companion object {
        private val FocusFinderThreadLocal =
            object : ThreadLocal<FocusFinderCompat>() {
                override fun initialValue(): FocusFinderCompat {
                    return FocusFinderCompat()
                }
            }

        /** Get the focus finder for this thread. */
        val instance: FocusFinderCompat
            get() = FocusFinderThreadLocal.get()!!
    }

    private val focusedRect: Rect = Rect()

    private val userSpecifiedFocusComparator =
        UserSpecifiedFocusComparator({ r, v ->
            if (isValidId(v.nextFocusForwardId)) v.findUserSetNextFocus(r, FOCUS_FORWARD) else null
        })

    private val tmpList = MutableObjectList<View>()

    // enforce thread local access
    private fun FocusFinder() {}

    /**
     * Find the next view to take focus in root's descendants, starting from the view that currently
     * is focused.
     *
     * @param root Contains focused. Cannot be null.
     * @param focused Has focus now.
     * @param direction Direction to look.
     * @return The next focusable view, or null if none exists.
     */
    fun findNextFocus1d(root: ViewGroup, focused: View, direction: Int): View? {
        val effectiveRoot = getEffectiveRoot(root, focused)
        var next = findNextUserSpecifiedFocus(effectiveRoot, focused, direction)
        if (next != null) {
            return next
        }
        val focusables = tmpList
        try {
            focusables.clear()
            effectiveRoot.addFocusableViews(focusables, direction)
            if (!focusables.isEmpty()) {
                next = findNextFocus(effectiveRoot, focused, direction, focusables)
            }
        } finally {
            focusables.clear()
        }
        return next
    }

    /**
     * Returns the "effective" root of a view. The "effective" root is the closest ancestor
     * within-which focus should cycle.
     *
     * For example: normal focus navigation would stay within a ViewGroup marked as
     * touchscreenBlocksFocus and keyboardNavigationCluster until a cluster-jump out.
     *
     * @return the "effective" root of {@param focused}
     */
    private fun getEffectiveRoot(root: ViewGroup, focused: View?): ViewGroup {
        if (focused == null || focused === root) {
            return root
        }
        var effective: ViewGroup? = null
        var nextParent = focused.parent
        while (nextParent is ViewGroup) {
            if (nextParent === root) {
                return effective ?: root
            }
            val vg = nextParent
            if (
                vg.touchscreenBlocksFocus &&
                    focused.context.packageManager.hasSystemFeature(
                        PackageManager.FEATURE_TOUCHSCREEN
                    )
            ) {
                // Don't stop and return here because the cluster could be nested and we only
                // care about the top-most one.
                effective = vg
            }
            nextParent = nextParent.parent
        }
        return root
    }

    private fun findNextUserSpecifiedFocus(root: ViewGroup, focused: View, direction: Int): View? {
        // check for user specified next focus
        var userSetNextFocus: View? = focused.findUserSetNextFocus(root, direction)
        var cycleCheck = userSetNextFocus
        var cycleStep = true // we want the first toggle to yield false
        while (userSetNextFocus != null) {
            if (
                userSetNextFocus.isFocusable &&
                    userSetNextFocus.visibility == View.VISIBLE &&
                    (!userSetNextFocus.isInTouchMode || userSetNextFocus.isFocusableInTouchMode)
            ) {
                return userSetNextFocus
            }
            userSetNextFocus = userSetNextFocus.findUserSetNextFocus(root, direction)
            if ((!cycleStep).also { cycleStep = it }) {
                cycleCheck = cycleCheck?.findUserSetNextFocus(root, direction)
                if (cycleCheck === userSetNextFocus) {
                    // found a cycle, user-specified focus forms a loop and none of the views
                    // are currently focusable.
                    break
                }
            }
        }
        return null
    }

    private fun findNextFocus(
        root: ViewGroup,
        focused: View,
        direction: Int,
        focusables: MutableObjectList<View>
    ): View? {
        val focusedRect = focusedRect
        // fill in interesting rect from focused
        focused.getFocusedRect(focusedRect)
        root.offsetDescendantRectToMyCoords(focused, focusedRect)

        return findNextFocusInRelativeDirection(focusables, root, focused, direction)
    }

    @SuppressLint("AsCollectionCall")
    private fun findNextFocusInRelativeDirection(
        focusables: MutableObjectList<View>,
        root: ViewGroup?,
        focused: View,
        direction: Int
    ): View? {
        try {
            // Note: This sort is stable.
            userSpecifiedFocusComparator.setFocusables(focusables, root!!)
            Collections.sort(focusables.asMutableList(), userSpecifiedFocusComparator)
        } finally {
            userSpecifiedFocusComparator.recycle()
        }

        val count = focusables.size
        if (count < 2) {
            return null
        }
        var next: View? = null
        val looped = BooleanArray(1)
        when (direction) {
            FOCUS_FORWARD -> next = getNextFocusable(focused, focusables, count, looped)
            FOCUS_BACKWARD -> next = getPreviousFocusable(focused, focusables, count, looped)
        }
        return next ?: focusables[count - 1]
    }

    private fun getNextFocusable(
        focused: View,
        focusables: ObjectList<View>,
        count: Int,
        outLooped: BooleanArray
    ): View? {
        if (count < 2) {
            return null
        }
        val position = focusables.lastIndexOf(focused)
        if (position >= 0 && position + 1 < count) {
            return focusables[position + 1]
        }
        outLooped[0] = true
        return focusables[0]
    }

    private fun getPreviousFocusable(
        focused: View?,
        focusables: ObjectList<View>,
        count: Int,
        outLooped: BooleanArray
    ): View? {
        if (count < 2) {
            return null
        }
        if (focused != null) {
            val position = focusables.indexOf(focused)
            if (position > 0) {
                return focusables[position - 1]
            }
        }
        outLooped[0] = true
        return focusables[count - 1]
    }

    private fun isValidId(id: Int): Boolean {
        return id != 0 && id != View.NO_ID
    }

    /**
     * Sorts views according to any explicitly-specified focus-chains. If there are no explicitly
     * specified focus chains (eg. no nextFocusForward attributes defined), this should be a no-op.
     */
    private class UserSpecifiedFocusComparator(private val mNextFocusGetter: NextFocusGetter) :
        Comparator<View?> {
        private val nextFoci = mutableScatterMapOf<View, View>()
        private val isConnectedTo = mutableScatterSetOf<View>()
        private val headsOfChains = mutableScatterMapOf<View, View>()
        private val originalOrdinal = mutableObjectIntMapOf<View>()
        private var root: View? = null

        fun interface NextFocusGetter {
            fun get(root: View, view: View): View?
        }

        fun recycle() {
            root = null
            headsOfChains.clear()
            isConnectedTo.clear()
            originalOrdinal.clear()
            nextFoci.clear()
        }

        fun setFocusables(focusables: ObjectList<View>, root: View) {
            this.root = root
            focusables.forEachIndexed { index, view -> originalOrdinal[view] = index }

            for (i in focusables.indices.reversed()) {
                val view = focusables[i]
                val next = mNextFocusGetter.get(root, view)
                if (next != null && originalOrdinal.containsKey(next)) {
                    nextFoci[view] = next
                    isConnectedTo.add(next)
                }
            }

            for (i in focusables.indices.reversed()) {
                val view = focusables[i]
                val next = nextFoci[view]
                if (next != null && !isConnectedTo.contains(view)) {
                    setHeadOfChain(view)
                }
            }
        }

        fun setHeadOfChain(head: View) {
            var newHead = head
            var view: View? = newHead
            while (view != null) {
                val otherHead = headsOfChains[view]
                if (otherHead != null) {
                    if (otherHead === newHead) {
                        return // This view has already had its head set properly
                    }
                    // A hydra -- multi-headed focus chain (e.g. A->C and B->C)
                    // Use the one we've already chosen instead and reset this chain.
                    view = newHead
                    newHead = otherHead
                }
                headsOfChains[view] = newHead
                view = nextFoci[view]
            }
        }

        override fun compare(first: View?, second: View?): Int {
            if (first === second) {
                return 0
            }
            if (first == null) {
                return -1
            }
            if (second == null) {
                return 1
            }
            // Order between views within a chain is immaterial -- next/previous is
            // within a chain is handled elsewhere.
            val firstHead = headsOfChains[first]
            val secondHead = headsOfChains[second]
            if (firstHead === secondHead && firstHead != null) {
                return if (first === firstHead) {
                    -1 // first is the head, it should be first
                } else if (second === firstHead) {
                    1 // second is the head, it should be first
                } else if (nextFoci[first] != null) {
                    -1 // first is not the end of the chain
                } else {
                    1 // first is end of chain
                }
            }
            val chainedFirst = firstHead ?: first
            val chainedSecond = secondHead ?: second

            return if (firstHead != null || secondHead != null) {
                // keep original order between chains
                if (originalOrdinal[chainedFirst] < originalOrdinal[chainedSecond]) -1 else 1
            } else {
                0
            }
        }
    }
}

/**
 * If a user manually specified the next view id for a particular direction, use the root to look up
 * the view.
 *
 * @param root The root view of the hierarchy containing this view.
 * @param direction One of FOCUS_UP, FOCUS_DOWN, FOCUS_LEFT, FOCUS_RIGHT, FOCUS_FORWARD, or
 *   FOCUS_BACKWARD.
 * @return The user specified next view, or null if there is none.
 */
private fun View.findUserSetNextFocus(root: View, direction: Int): View? {
    when (direction) {
        FOCUS_FORWARD -> {
            val next = nextFocusForwardId
            if (next == View.NO_ID) return null
            return findViewInsideOutShouldExist(root, this, next)
        }
        FOCUS_BACKWARD -> {
            if (id == View.NO_ID) return null
            val startView: View = this
            // Since we have forward links but no backward links, we need to find the view that
            // forward links to this view. We can't just find the view with the specified ID
            // because view IDs need not be unique throughout the tree.
            return root.findViewByPredicateInsideOut(startView) { t ->
                (findViewInsideOutShouldExist(root, t, t.nextFocusForwardId) === startView)
            }
        }
    }
    return null
}

private fun findViewInsideOutShouldExist(root: View, start: View, id: Int): View? {
    return root.findViewByPredicateInsideOut(start) { it.id == id }
}

/**
 * Look for a child view that matches the specified predicate, starting with the specified view and
 * its descendants and then recursively searching the ancestors and siblings of that view until this
 * view is reached.
 *
 * This method is useful in cases where the predicate does not match a single unique view (perhaps
 * multiple views use the same id) and we are trying to find the view that is "closest" in scope to
 * the starting view.
 *
 * @param start The view to start from.
 * @param predicate The predicate to evaluate.
 * @return The first view that matches the predicate or null.
 */
private fun View.findViewByPredicateInsideOut(start: View, predicate: (View) -> Boolean): View? {
    var next = start
    var childToSkip: View? = null
    while (true) {
        val view = next.findViewByPredicateTraversal(predicate, childToSkip)
        if (view != null || next === this) {
            return view
        }

        val parent = next.parent
        if (parent == null || parent !is View) {
            return null
        }

        childToSkip = next
        next = parent
    }
}

/**
 * @param predicate The predicate to evaluate.
 * @param childToSkip If not null, ignores this child during the recursive traversal.
 * @return The first view that matches the predicate or null.
 */
private fun View.findViewByPredicateTraversal(
    predicate: (View) -> Boolean,
    childToSkip: View?
): View? {
    if (predicate(this)) {
        return this
    }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child !== childToSkip) {
                val v = child.findViewByPredicateTraversal(predicate, childToSkip)
                if (v != null) {
                    return v
                }
            }
        }
    }
    return null
}

private fun View.addFocusableViews(views: MutableObjectList<View>, direction: Int) {
    addFocusableViews(views, direction, isInTouchMode)
}

/**
 * Older versions of View don't add focusable Views in order. This is a corrected version that adds
 * them in the right order.
 */
private fun View.addFocusableViews(
    views: MutableObjectList<View>,
    direction: Int,
    inTouchMode: Boolean
) {
    if (
        isVisible &&
            isFocusable &&
            isEnabled &&
            width > 0 &&
            height > 0 &&
            (!inTouchMode || isFocusableInTouchMode)
    ) {
        views += this
    }
    if (this is ViewGroup) {
        for (i in 0 until childCount) {
            getChildAt(i).addFocusableViews(views, direction, inTouchMode)
        }
    }
}
