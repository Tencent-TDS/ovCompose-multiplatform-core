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

package androidx.compose.ui.viewinterop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ComposeNode
import androidx.compose.runtime.ComposeNodeLifecycleCallback
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.ReusableComposeNode
import androidx.compose.runtime.Updater
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.currentCompositeKeyHash
import androidx.compose.ui.Modifier
import androidx.compose.ui.UiComposable
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.materialize
import androidx.compose.ui.node.ComposeUiNode.Companion.SetCompositeKeyHash
import androidx.compose.ui.node.ComposeUiNode.Companion.SetResolvedCompositionLocals
import androidx.compose.ui.node.LayoutNode
import androidx.compose.ui.node.OwnerScope
import androidx.compose.ui.node.OwnerSnapshotObserver
import androidx.compose.ui.platform.DefaultUiApplier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density

private val NoOp: Any.() -> Unit = {}

private class AbstractInvocationError(
    name: String
): Error("Abstract `$name` must be implemented by platform-specific subclass")

/**
 * A holder that keeps references to user interop view and its group (container).
 * It's actual of `expect class [InteropViewFactoryHolder]`
 *
 * @see InteropViewFactoryHolder
 */
internal open class InteropViewHolder(
    val container: InteropContainer,
    val group: InteropViewGroup,
    private val compositeKeyHash: Int,
) : ComposeNodeLifecycleCallback, OwnerScope {
    private var onModifierChanged: ((Modifier) -> Unit)? = null

    var modifier: Modifier = Modifier
        set(value) {
            if (value !== field) {
                field = value
                onModifierChanged?.invoke(value)
            }
        }

    private var hasUpdateBlock = false

    var update: () -> Unit = {}
        protected set(value) {
            field = value
            hasUpdateBlock = true
            runUpdate()
        }

    protected var reset: () -> Unit = {}

    protected var release: () -> Unit = {}

    private var onDensityChanged: ((Density) -> Unit)? = null
    var density: Density = Density(1f)
        set(value) {
            if (value !== field) {
                field = value
                onDensityChanged?.invoke(value)
            }
        }

    private var isAttachedToWindow: Boolean = true

    override val isValidOwnerScope: Boolean
        get() = isAttachedToWindow

    private val snapshotObserver: OwnerSnapshotObserver
        get() {
            return container.snapshotObserver
        }

    private val runUpdate: () -> Unit = {
        // If we're not attached, the observer isn't started, so don't bother running it.
        // onAttachedToWindow will run an update the next time the view is attached.
        if (hasUpdateBlock && isAttachedToWindow) {
            snapshotObserver.observeReads(this, OnCommitAffectingUpdate, update)
        }
    }

    override fun onReuse() {
        isAttachedToWindow = true
        reset()
    }

    override fun onDeactivate() {
        reset()
        isAttachedToWindow = false
    }

    override fun onRelease() {
        release()
        isAttachedToWindow = false
    }

    val layoutNode: LayoutNode = run {
        val holder = this

        val layoutNode = LayoutNode()

        layoutNode.interopViewFactoryHolder = holder

        val interopModifier = Modifier
            .semantics(mergeDescendants = true) {}
            .pointerInteropFilter(isInteractive = isInteractive, interopViewHolder = holder)
            .trackInteropPlacement(holder)
            .then(extraModifier)
            .onGloballyPositioned { layoutCoordinates ->
                layoutAccordingTo(layoutCoordinates)
            }

        layoutNode.compositeKeyHash = compositeKeyHash
        layoutNode.modifier = modifier then interopModifier
        onModifierChanged = { layoutNode.modifier = it then interopModifier }

        layoutNode.density = density
        onDensityChanged = { layoutNode.density = it }

        layoutNode.measurePolicy = measurePolicy

        layoutNode
    }

    fun place() {
        container.placeInteropView(this)
    }

    fun unplace() {
        container.unplaceInteropView(this)
    }

    // ===== Abstract methods to be implemented by platform-specific subclasses =====

    open val isInteractive: Boolean
        get() {
            throw AbstractInvocationError("val isInteractive: Boolean")
        }

    open val measurePolicy: MeasurePolicy
        get() {
            throw AbstractInvocationError("val measurePolicy: MeasurePolicy")
        }

    /**
     * Modifier containing platform-specific interop view modifiers chain, such as custom drawing,
     * native accessibility setup, etc.
     */
    open val extraModifier: Modifier
        get() {
            throw AbstractInvocationError("val platformModifier: Modifier")
        }

    open fun dispatchToView(pointerEvent: PointerEvent) {
        throw AbstractInvocationError("fun dispatchToView(pointerEvent: PointerEvent)")
    }

    open fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        throw AbstractInvocationError("fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates)")
    }

    open fun getInteropView(): InteropView? {
        throw AbstractInvocationError("fun getInteropView(): InteropView?")
    }

    companion object {
        private val OnCommitAffectingUpdate: (InteropViewHolder) -> Unit = {
            it.container.changeInteropViewLayout { it.update() }
        }
    }
}

/**
 * Base class for any concrete implementation of [InteropViewHolder] that holds a specific type
 * of InteropView to be implemented by the platform-specific [TypedInteropViewHolder] subclass
 */
internal abstract class TypedInteropViewHolder<T : InteropView>(
    val factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropViewGroup,
    compositeKeyHash: Int,
) : InteropViewHolder(interopContainer, group, compositeKeyHash) {
    protected val interopView = factory()

    var updateBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            update = { interopView.apply(updateBlock) }
        }

    var resetBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            reset = { interopView.apply(resetBlock) }
        }

    var releaseBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            release = {
                interopView.apply(releaseBlock)
            }
        }
}

/**
 * Entry point for creating a composable that wraps a platform specific interop view.
 * Platform implementations should call it and provide the appropriate factory, returning
 * a subclass of [TypedInteropViewHolder].
 */
@Composable
@UiComposable
internal fun <T : InteropView> InteropView(
    factory: (compositeKeyHash: Int) -> TypedInteropViewHolder<T>,
    modifier: Modifier,
    onReset: ((T) -> Unit)? = null,
    onRelease: (T) -> Unit = NoOp,
    update: (T) -> Unit = NoOp,
) {
    val compositionLocalMap = currentComposer.currentCompositionLocalMap
    val materializedModifier = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val compositeKeyHash = currentCompositeKeyHash

    // TODO: there are other parameters on Android that we don't yet use:
    //  lifecycleOwner, savedStateRegistryOwner, layoutDirection

    if (onReset == null) {
        ComposeNode<LayoutNode, DefaultUiApplier>(
            factory = {
                factory(compositeKeyHash).layoutNode
            },
            update = {
                updateParameters(
                    compositionLocalMap,
                    materializedModifier,
                    density,
                    compositeKeyHash,
                    onReset,
                    update,
                    onRelease
                )
            }
        )
    } else {
        ReusableComposeNode<LayoutNode, DefaultUiApplier>(
            factory = {
                factory(compositeKeyHash).layoutNode
            },
            update = {
                updateParameters(
                    compositionLocalMap,
                    materializedModifier,
                    density,
                    compositeKeyHash,
                    onReset,
                    update,
                    onRelease
                )
            }
        )
    }
}

private fun <T : InteropView> Updater<LayoutNode>.updateParameters(
    compositionLocalMap: CompositionLocalMap,
    modifier: Modifier,
    density: Density,
    compositeKeyHash: Int,
    onResetOrNull: ((T) -> Unit)?,
    update: (T) -> Unit,
    onRelease: (T) -> Unit
) {
    set(compositionLocalMap, SetResolvedCompositionLocals)
    set(modifier) { requireViewFactoryHolder<T>().modifier = it }
    set(density) { requireViewFactoryHolder<T>().density = it }
    set(compositeKeyHash, SetCompositeKeyHash)

    onResetOrNull?.let { onReset ->
        set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
    }

    set(update) { requireViewFactoryHolder<T>().updateBlock = it }
    set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
}

// Based on AndroidView.android.kt:requireViewFactoryHolder
@Suppress("UNCHECKED_CAST")
private fun <T : InteropView> LayoutNode.requireViewFactoryHolder(): TypedInteropViewHolder<T> {
    // This LayoutNode is created and managed internally here, so it's safe to cast
    return checkNotNull(interopViewFactoryHolder) as TypedInteropViewHolder<T>
}

