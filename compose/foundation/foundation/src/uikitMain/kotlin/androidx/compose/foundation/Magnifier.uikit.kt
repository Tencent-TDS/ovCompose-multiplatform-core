/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.foundation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.interop.LocalLayerContainer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.debugInspectorInfo
import androidx.compose.ui.platform.inspectable
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.toSize
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.jetbrains.skiko.OS
import org.jetbrains.skiko.OSVersion
import org.jetbrains.skiko.available

/**
 * A function on elements that are magnified with a [magnifier] modifier that returns the position
 * of the center of the magnified content in the coordinate space of the root composable.
 */
internal val MagnifierPositionInRoot =
    SemanticsPropertyKey<() -> Offset>("MagnifierPositionInRoot")

@ExperimentalFoundationApi
actual fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset,
    zoom: Float,
    style: MagnifierStyle,
    onSizeChanged: ((DpSize) -> Unit)?
): Modifier = inspectable(
    // Publish inspector info even if magnification isn't supported.
    inspectorInfo = debugInspectorInfo {
        name = if (isPlatformMagnifierSupported()) "magnifier" else "magnifier (not supported)"
        properties["sourceCenter"] = sourceCenter
        properties["magnifierCenter"] = magnifierCenter
        properties["zoom"] = zoom
        properties["style"] = style
    }
) {
    if (isPlatformMagnifierSupported()) {
        magnifier(
            sourceCenter = sourceCenter,
            magnifierCenter = magnifierCenter,
            zoom = zoom,
            style = style,
            onSizeChanged = onSizeChanged,
            platformMagnifierFactory = PlatformMagnifierFactory.getForCurrentPlatform()
        )
    } else {
        // Magnifier is only supported in >=28. So avoid doing all the work to manage the magnifier
        // state if it's not needed.
        // TODO(b/202739980) Investigate supporting Magnifier on earlier versions.
        Modifier
    }
}

/**
 * @param platformMagnifierFactory Creates a [PlatformMagnifier] whenever the configuration changes.
 */
@OptIn(ExperimentalFoundationApi::class, InternalComposeUiApi::class)
// The InspectorInfo this modifier reports is for the above public overload, and intentionally
// doesn't include the platformMagnifierFactory parameter.
internal fun Modifier.magnifier(
    sourceCenter: Density.() -> Offset,
    magnifierCenter: Density.() -> Offset,
    zoom: Float,
    style: MagnifierStyle,
    onSizeChanged: ((DpSize) -> Unit)?,
    platformMagnifierFactory: PlatformMagnifierFactory
): Modifier = composed {
    val view = LocalLayerContainer.current
    val density = LocalDensity.current
    var anchorPositionInRoot: Offset by remember { mutableStateOf(Offset.Unspecified) }
    val updatedSourceCenter by rememberUpdatedState(sourceCenter)
    val updatedMagnifierCenter by rememberUpdatedState(magnifierCenter)
    val updatedZoom by rememberUpdatedState(zoom)
    val updatedOnSizeChanged by rememberUpdatedState(onSizeChanged)
    val sourceCenterInRoot by remember {
        derivedStateOf {
            val sourceCenterOffset = updatedSourceCenter(density)

            if (anchorPositionInRoot.isSpecified && sourceCenterOffset.isSpecified) {
                anchorPositionInRoot + sourceCenterOffset
            } else {
                Offset.Unspecified
            }
        }
    }
    val isMagnifierShown by remember { derivedStateOf { sourceCenterInRoot.isSpecified } }

    /**
     * Used to request that the magnifier updates its buffer when the layer is redrawn.
     */
    val onNeedsUpdate = remember {
        MutableSharedFlow<Unit>(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    // On platforms >=Q, the zoom level can be updated dynamically on an existing magnifier, so if
    // the zoom changes between recompositions we don't need to recreate the magnifier. On older
    // platforms, the zoom can only be set initially, so we use the zoom itself as a key so the
    // magnifier gets recreated if it changes.
    val zoomEffectKey = if (platformMagnifierFactory.canUpdateZoom) 0f else zoom

    // centerOffset, sourceToMagnifierOffset, and zoom are not in this key list because they can be
    // updated without re-creating the Magnifier.
    LaunchedEffect(
        view,
        density,
        zoomEffectKey,
        style,
        // This is a separate key because otherwise a change from Default to TextDefault won't
        // reconfigure. Note that this checks for reference equality, not structural equality, since
        // TextDefault == Default already.
        style == MagnifierStyle.TextDefault
    ) {
        val magnifier = platformMagnifierFactory.create(style, view, density, zoom)
        var previousSize = magnifier.size.also { newSize ->
            updatedOnSizeChanged?.invoke(
                with(density) {
                    newSize.toSize().toDpSize()
                }
            )
        }

        // Ask the magnifier to do another pixel copy whenever the nearest layer is redrawn.
        onNeedsUpdate
            .onEach { magnifier.updateContent() }
            .launchIn(this)

        try {
            // Update the modifier in a snapshotFlow so it will be restarted whenever any state read
            // by the update function changes.
            snapshotFlow {
                // Once the position is set, it's never null again, so we don't need to worry about
                // dismissing the magnifier if this expression changes value.
                if (isMagnifierShown) {
                    magnifier.update(
                        sourceCenter = sourceCenterInRoot,
                        magnifierCenter = updatedMagnifierCenter(density).let {
                            if (it.isSpecified) {
                                anchorPositionInRoot + it
                            } else {
                                Offset.Unspecified
                            }
                        },
                        zoom = updatedZoom
                    )

                    magnifier.size.let { size ->
                        if (size != previousSize) {
                            previousSize = size
                            updatedOnSizeChanged?.invoke(
                                with(density) {
                                    size.toSize().toDpSize()
                                }
                            )
                        }
                    }
                } else {
                    // Can't place the magnifier at an unspecified location, so just hide it.
                    magnifier.dismiss()
                }
            }.collect()
        } finally {
            // Dismiss the magnifier whenever it needs to be recreated or it's removed from the
            // composition.
            magnifier.dismiss()
        }
    }

    return@composed this
        .onGloballyPositioned {
            // The mutable state must store the Offset, not the LocalCoordinates, because the same
            // LocalCoordinates instance may be sent to this callback multiple times, not implement
            // equals, or be stable, and so won't invalidate the snapshotFlow.
            anchorPositionInRoot = it.positionInRoot()
        }
        .drawBehind {
            // Tell the magnifier to update itself every time the layer is re-drawn.
            // Note that this won't do anything if the magnifier is showing a different layer,
            // but it handles the case where the cursor is blinking so it's better than nothing.
            onNeedsUpdate.tryEmit(Unit)
        }
        .semantics {
            this[MagnifierPositionInRoot] = { sourceCenterInRoot }
        }
}

internal actual fun isPlatformMagnifierSupported() : Boolean =
    available(OS.Ios to OSVersion(major = 17))

