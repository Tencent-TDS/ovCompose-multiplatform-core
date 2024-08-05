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
import androidx.compose.runtime.snapshots.SnapshotStateObserver
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
import androidx.compose.ui.platform.DefaultUiApplier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

private val NoOp: Any.() -> Unit = {}

private class AbstractInvocationError(
    name: String
) : Error("Abstract `$name` must be implemented by platform-specific subclass of `InteropViewHolder`")

/**
 * A holder that keeps references to user interop view and its group (container).
 * It's actual of `expect class [InteropViewFactoryHolder]`
 *
 * @see InteropViewFactoryHolder
 *
 * @param platformModifier The modifier that is specific to the platform.
 */
internal open class InteropViewHolder(
    val container: InteropContainer,
    val group: InteropViewGroup,
    private val compositeKeyHash: Int,
    measurePolicy: MeasurePolicy,
    isInteractive: Boolean,
    platformModifier: Modifier
) : ComposeNodeLifecycleCallback {
    private var onModifierChanged: ((Modifier) -> Unit)? = null

    /**
     * User-provided modifier that will be reapplied if changed.
     */
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

    private val snapshotObserver: SnapshotStateObserver
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
        isAttachedToWindow = false
    }

    override fun onRelease() {
        release()
        isAttachedToWindow = false
    }

    /**
     * Construct a [LayoutNode] that is linked to this [InteropViewHolder].
     */
    val layoutNode: LayoutNode by lazy {
        val layoutNode = LayoutNode()

        layoutNode.interopViewFactoryHolder = this

        val coreModifier = platformModifier
            .pointerInteropFilter(isInteractive = isInteractive, interopViewHolder = this)
            .trackInteropPlacement(this)
            .onGloballyPositioned { layoutCoordinates ->
                layoutAccordingTo(layoutCoordinates)
            }

        layoutNode.compositeKeyHash = compositeKeyHash
        layoutNode.modifier = modifier then coreModifier
        onModifierChanged = { layoutNode.modifier = it then coreModifier }

        layoutNode.density = density
        onDensityChanged = { layoutNode.density = it }

        layoutNode.measurePolicy = measurePolicy

        layoutNode
    }

    fun place() {
        container.place(this)
    }

    fun unplace() {
        container.unplace(this)
        snapshotObserver.clear(this)
    }

    // ===== Abstract methods to be implemented by platform-specific subclasses =====

    /**
     * Dispatches the pointer event to the interop view.
     */
    open fun dispatchToView(pointerEvent: PointerEvent) {
        throw AbstractInvocationError("fun dispatchToView(pointerEvent: PointerEvent)")
    }

    /**
     * Layout the interop view according to the given layout coordinates.
     */
    open fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates) {
        throw AbstractInvocationError("fun layoutAccordingTo(layoutCoordinates: LayoutCoordinates)")
    }

    /**
     * `expect fun` of expect class [InteropViewFactoryHolder] (aka this)
     * Returns the actual interop view instance.
     */
    open fun getInteropView(): InteropView? {
        throw AbstractInvocationError("fun getInteropView(): InteropView?")
    }

    companion object {
        private val OnCommitAffectingUpdate: (InteropViewHolder) -> Unit = {
            it.container.update { it.update() }
        }
    }
}

/**
 * Base class for any concrete implementation of [InteropViewHolder] that holds a specific type
 * of InteropView to be implemented by the platform-specific [TypedInteropViewHolder] subclass
 */
internal abstract class TypedInteropViewHolder<T : InteropView>(
    factory: () -> T,
    interopContainer: InteropContainer,
    group: InteropViewGroup,
    compositeKeyHash: Int,
    measurePolicy: MeasurePolicy,
    isInteractive: Boolean,
    platformModifier: Modifier
) : InteropViewHolder(
    interopContainer,
    group,
    compositeKeyHash,
    measurePolicy,
    isInteractive,
    platformModifier
) {
    protected val typedInteropView = factory()

    override fun getInteropView(): InteropView? {
        return typedInteropView
    }

    /**
     * A block containing the update logic for [T], to be forwarded to user.
     * Setting it will schedule an update immediately.
     * See [InteropViewHolder.update]
     */
    var updateBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            update = { typedInteropView.apply(updateBlock) }
        }

    /**
     * A block containing the reset logic for [T], to be forwarded to user.
     * It will be called if [LayoutNode] associated with this [InteropViewHolder] is reused to
     * avoid interop view reallocation.
     */
    var resetBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            reset = { typedInteropView.apply(resetBlock) }
        }

    /**
     * A block containing the release logic for [T], to be forwarded to user.
     * It will be called if [LayoutNode] associated with this [InteropViewHolder] is released.
     */
    var releaseBlock: (T) -> Unit = NoOp
        set(value) {
            field = value
            release = {
                typedInteropView.apply(releaseBlock)
            }
        }
}

/**
 * Create a [LayoutNode] factory that can be constructed from [TypedInteropViewHolder] built with
 * the [currentCompositeKeyHash]
 *
 * @see [AndroidView.android.kt:createAndroidViewNodeFactory]
 */
@Composable
private fun <T : InteropView> createInteropViewLayoutNodeFactory(
    factory: (compositeKeyHash: Int) -> TypedInteropViewHolder<T>
): () -> LayoutNode {
    val compositeKeyHash = currentCompositeKeyHash

    return {
        factory(compositeKeyHash).layoutNode
    }
}

/**
 * Entry point for creating a composable that wraps a platform specific interop view.
 * Platform implementations should call it and provide the appropriate factory, returning
 * a subclass of [TypedInteropViewHolder].
 *
 * @see [AndroidView.android.kt:AndroidView]
 */
@Composable
// @UiComposable TODO: ditch content from swing implementation and uncomment
internal fun <T : InteropView> InteropView(
    factory: (compositeKeyHash: Int) -> TypedInteropViewHolder<T>,
    modifier: Modifier,
    onReset: ((T) -> Unit)? = null,
    onRelease: (T) -> Unit = NoOp,
    update: (T) -> Unit = NoOp,
    content: (@Composable () -> Unit)? = null
) {
    val compositeKeyHash = currentCompositeKeyHash
    val materializedModifier = currentComposer.materialize(modifier)
    val density = LocalDensity.current
    val compositionLocalMap = currentComposer.currentCompositionLocalMap

    // TODO: there are other parameters on Android that we don't yet use:
    //  lifecycleOwner, savedStateRegistryOwner, layoutDirection
    if (onReset == null) {
        if (content == null) {
            ComposeNode<LayoutNode, DefaultUiApplier>(
                factory = createInteropViewLayoutNodeFactory(factory),
                update = {
                    updateParameters<T>(
                        compositionLocalMap,
                        materializedModifier,
                        density,
                        compositeKeyHash
                    )
                    set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                    set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
                }
            )
        } else {
            ComposeNode<LayoutNode, DefaultUiApplier>(
                factory = createInteropViewLayoutNodeFactory(factory),
                update = {
                    updateParameters<T>(
                        compositionLocalMap,
                        materializedModifier,
                        density,
                        compositeKeyHash
                    )
                    set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                    set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
                },
                content = content
            )
        }
    } else {
        if (content == null) {
            ReusableComposeNode<LayoutNode, DefaultUiApplier>(
                factory = createInteropViewLayoutNodeFactory(factory),
                update = {
                    updateParameters<T>(
                        compositionLocalMap,
                        materializedModifier,
                        density,
                        compositeKeyHash
                    )
                    set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
                    set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                    set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
                }
            )
        } else {
            ReusableComposeNode<LayoutNode, DefaultUiApplier>(
                factory = createInteropViewLayoutNodeFactory(factory),
                update = {
                    updateParameters<T>(
                        compositionLocalMap,
                        materializedModifier,
                        density,
                        compositeKeyHash
                    )
                    set(onReset) { requireViewFactoryHolder<T>().resetBlock = it }
                    set(update) { requireViewFactoryHolder<T>().updateBlock = it }
                    set(onRelease) { requireViewFactoryHolder<T>().releaseBlock = it }
                },
                content = content
            )
        }
    }
}

/**
 * Updates the parameters of the [LayoutNode] in the current [Updater] with the given values.
 * @see [AndroidView.android.kt:updateViewHolderParams]
 */
private fun <T : InteropView> Updater<LayoutNode>.updateParameters(
    compositionLocalMap: CompositionLocalMap,
    modifier: Modifier,
    density: Density,
    compositeKeyHash: Int
) {
    set(compositionLocalMap, SetResolvedCompositionLocals)
    set(modifier) { requireViewFactoryHolder<T>().modifier = it }
    set(density) { requireViewFactoryHolder<T>().density = it }
    set(compositeKeyHash, SetCompositeKeyHash)
}

/**
 * Returns the [TypedInteropViewHolder] associated with the current [LayoutNode].
 * Since the [TypedInteropViewHolder] is responsible for constructing the [LayoutNode], it
 * associates itself with the [LayoutNode] by setting the [LayoutNode.interopViewFactoryHolder]
 * property and it's safe to cast from [InteropViewHolder]
 */
@Suppress("UNCHECKED_CAST")
private fun <T : InteropView> LayoutNode.requireViewFactoryHolder(): TypedInteropViewHolder<T> {
    // This LayoutNode is created and managed internally here, so it's safe to cast
    return checkNotNull(interopViewFactoryHolder) as TypedInteropViewHolder<T>
}

