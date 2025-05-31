/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.interop.arkc

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateObserver
import androidx.compose.ui.Modifier
import androidx.compose.ui.arkui.pointerInteropPlaceholderFilter
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.interop.ArkUIInteropViewHierarchyChange
import androidx.compose.ui.interop.InteropContainer
import androidx.compose.ui.interop.LocalArkUIInteropContext
import androidx.compose.ui.interop.LocalBackArkUINativeInteropContainer
import androidx.compose.ui.interop.LocalForeArkUINativeInteropContainer
import androidx.compose.ui.interop.LocalTouchableArkUINativeInteropContainer
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.MeasurePolicy
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import kotlinx.atomicfu.atomic
import platform.arkui.ArkUI_NodeHandle

private val STUB_CALLBACK_WITH_RECEIVER: Any.() -> Unit = {}

@Composable
fun ForeArkUINodeHandle(
    factory: () -> ArkUI_NodeHandle,
    modifier: Modifier,
    update: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true
) {
    ArkUINativeView(
        factory = { ArkUINativeView(factory()) },
        modifier = modifier,
        update = { update(it.handle) },
        background = background,
        onRelease = { onRelease(it.handle) },
        interactive = interactive,
        container = InteropContainer.FORE
    )
}

@Composable
fun ArkUINodeHandle(
    factory: () -> ArkUI_NodeHandle,
    modifier: Modifier,
    update: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (ArkUI_NodeHandle) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
    container: InteropContainer = InteropContainer.BACK
) {
    ArkUINativeView(
        factory = { ArkUINativeView(factory()) },
        modifier = modifier,
        update = { update(it.handle) },
        background = background,
        onRelease = { onRelease(it.handle) },
        interactive = interactive,
        container = container
    )
}

/**
 * @param factory The block creating the [ArkUINativeView] to be composed.
 * @param modifier The modifier to be applied to the layout. Size should be specified in modifier.
 * Modifier may contains crop() modifier with different shapes.
 * @param update A callback to be invoked after the layout is inflated.
 * @param background A color of [ArkUINativeView] background wrapping the view created by [factory].
 * @param onRelease A callback invoked as a signal that this view instance has exited the
 * composition hierarchy entirely and will not be reused again. Any additional resources used by the
 * View should be freed at this time.
 * @param interactive If true, then user touches will be passed to this UIView
 */
@Composable
internal fun <T : ArkUINativeView> ArkUINativeView(
    factory: () -> T,
    modifier: Modifier,
    update: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    background: Color = Color.Unspecified,
    onRelease: (T) -> Unit = STUB_CALLBACK_WITH_RECEIVER,
    interactive: Boolean = true,
    container: InteropContainer = InteropContainer.BACK
) {
    val interopContainer: ArkUINativeInteropContainer = when (container) {
        InteropContainer.BACK -> LocalBackArkUINativeInteropContainer.current
        InteropContainer.FORE -> LocalForeArkUINativeInteropContainer.current
        InteropContainer.TOUCHABLE -> LocalTouchableArkUINativeInteropContainer.current
    }

    val embeddedInteropComponent = remember {
        EmbeddedInteropView(interopContainer = interopContainer, onRelease)
    }
    val density = LocalDensity.current.density
    val interopContext = LocalArkUIInteropContext.current

    var componentClipSize by remember { mutableStateOf(Size.Unspecified) }
    // A state to notify relayout. Any better way to handle this?
    var layoutKey by remember { mutableStateOf(0) }

    Place(layoutKey, modifier.onGloballyPositioned { coordinates ->
        val rootCoordinates = coordinates.findRootCoordinates()
        // Calculates the unclipped rect in the root coordinates.
        val unclippedRect = rootCoordinates.localBoundingBoxOf(coordinates, clipBounds = false)
        // Calculates the clipped rect in the root coordinates.
        val clippedRect = rootCoordinates.localBoundingBoxOf(coordinates, clipBounds = true)
        // When the component has been clipped, we need to check whether the clipping bounds have changed.
        interopContext.deferAction {
            with(embeddedInteropComponent) {
                // offset interop.
                wrappingView.offset = clippedRect.topLeft / density
                component.offset = (unclippedRect.topLeft - clippedRect.topLeft) / density

                // clip interop.
                if (clippedRect != unclippedRect || componentClipSize != Size.Unspecified) {
                    val clipSize = clippedRect.size.takeIf { !it.isEmpty() } ?: Size.Zero
                    if (clipSize != componentClipSize) {
                        componentClipSize = clipSize
                        wrappingView.isHidden = componentClipSize.isEmpty()
                        wrappingView.clipSize(componentClipSize / density)
                    }
                }
            }
        }
    }.let {
        when (container) {
            InteropContainer.BACK -> it.drawBehind {
                // draw transparent hole
                drawRect(Color.Transparent, blendMode = BlendMode.DstAtop)
            }
            InteropContainer.FORE, InteropContainer.TOUCHABLE -> it
        }
    }.trackArkUINativeInterop(view = embeddedInteropComponent.wrappingView).let {
        if (interactive) {
            it.pointerInteropPlaceholderFilter(embeddedInteropComponent.wrappingView)
        } else {
            it
        }
    }, measurePolicy = { _, constraints ->
        with(embeddedInteropComponent.wrappingView) {
            measure(constraints)
            val measuredSize = measuredSize
            layout(measuredSize.width, measuredSize.height) {}
        }
    })

    DisposableEffect(Unit) {
        embeddedInteropComponent.component = factory().also {
            embeddedInteropComponent.wrappingView.interopView = it
        }

        embeddedInteropComponent.wrappingView.onRequestReMeasure = {
            layoutKey++
        }

        embeddedInteropComponent.updater = Updater(embeddedInteropComponent.component, update) {
            interopContext.deferAction(action = it)
        }

        interopContext.deferAction(ArkUIInteropViewHierarchyChange.VIEW_ADDED) {
            embeddedInteropComponent.addToHierarchy()
        }

        onDispose {
            embeddedInteropComponent.wrappingView.interopView = null
            interopContext.deferAction(ArkUIInteropViewHierarchyChange.VIEW_REMOVED) {
                embeddedInteropComponent.removeFromHierarchy()
            }
        }
    }

    LaunchedEffect(background) {
        interopContext.deferAction {
            embeddedInteropComponent.setBackgroundColor(background)
        }
    }

    SideEffect {
        embeddedInteropComponent.updater.update = update
    }
}


@Composable
private fun Place(key: Int, modifier: Modifier, measurePolicy: MeasurePolicy) {
    key(key) {
        Layout({}, measurePolicy = measurePolicy, modifier = modifier)
    }
}

private abstract class EmbeddedInteropComponent<T : Any>(
    val interopContainer: ArkUINativeInteropContainer,
    val onRelease: (T) -> Unit
) {
    val wrappingView = InteropWrapper().apply {
        offset = Offset.Infinite
    }
    lateinit var component: T
    lateinit var updater: Updater<T>

    fun setBackgroundColor(color: Color) {
        if (color == Color.Unspecified) {
            wrappingView.backgroundColor = interopContainer.backgroundColor
        } else {
            wrappingView.backgroundColor = color
        }
    }

    abstract fun addToHierarchy()
    abstract fun removeFromHierarchy()

    protected fun addViewToHierarchy(view: ArkUINativeView) {
        view.handle.removeFromParent()
        wrappingView.insertChildAt(view, 0)
        interopContainer.addInteropView(wrappingView)
    }

    protected fun removeViewFromHierarchy(view: ArkUINativeView) {
        interopContainer.removeInteropView(wrappingView)
        wrappingView.removeChild(view)
        wrappingView.dispose()
        updater.dispose()
        onRelease(component)
    }
}

private class EmbeddedInteropView<T : ArkUINativeView>(
    interopContainer: ArkUINativeInteropContainer,
    onRelease: (T) -> Unit
) : EmbeddedInteropComponent<T>(interopContainer, onRelease) {
    override fun addToHierarchy() {
        addViewToHierarchy(component)
    }

    override fun removeFromHierarchy() {
        removeViewFromHierarchy(component)
    }
}

private class Updater<T : Any>(
    private val component: T,
    update: (T) -> Unit,

    /**
     * Updater will not execute the [update] method by itself, but will pass it to this lambda
     */
    private val deferAction: (() -> Unit) -> Unit,
) {
    private var isDisposed = false
    private val isUpdateScheduled = atomic(false)
    private val snapshotObserver = SnapshotStateObserver { command ->
        command()
    }

    private val scheduleUpdate = { _: T ->
        if (!isUpdateScheduled.getAndSet(true)) {
            deferAction {
                isUpdateScheduled.value = false
                if (!isDisposed) {
                    performUpdate()
                }
            }
        }
    }

    var update: (T) -> Unit = update
        set(value) {
            if (field != value) {
                field = value
                performUpdate()
            }
        }

    init {
        snapshotObserver.start()
        performUpdate()
    }

    private fun performUpdate() {
        // don't replace scheduleUpdate by lambda reference,
        // scheduleUpdate should always be the same instance
        snapshotObserver.observeReads(component, scheduleUpdate) {
            update(component)
        }
    }

    fun dispose() {
        snapshotObserver.stop()
        snapshotObserver.clear()
        isDisposed = true
    }
}