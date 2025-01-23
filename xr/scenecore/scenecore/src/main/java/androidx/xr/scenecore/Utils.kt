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

package androidx.xr.scenecore

import android.os.Handler
import android.os.Looper
import androidx.xr.runtime.math.Ray
import androidx.xr.scenecore.InputEvent.HitInfo
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions as RtDimensions
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent as RtInputEvent
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent.HitInfo as RtHitInfo
import androidx.xr.scenecore.JxrPlatformAdapter.MoveEvent as RtMoveEvent
import androidx.xr.scenecore.JxrPlatformAdapter.PixelDimensions as RtPixelDimensions
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent as RtResizeEvent
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities as RtSpatialCapabilities
import java.util.concurrent.Executor

internal class HandlerExecutor(val handler: Handler) : Executor {
    override fun execute(command: Runnable) {
        handler.post(command)
    }

    companion object {
        val mainThreadExecutor: Executor = HandlerExecutor(Handler(Looper.getMainLooper()))
    }
}

/** Extension function that converts a [Dimensions] to [RtDimensions]. */
internal fun Dimensions.toRtDimensions(): RtDimensions {
    return RtDimensions(width, height, depth)
}

/** Extension function that converts a [RtDimensions] to [Dimensions]. */
internal fun RtDimensions.toDimensions(): Dimensions {
    return Dimensions(width, height, depth)
}

/** Extension function that converts a [PixelDimensions] to [RtPixelDimensions]. */
internal fun PixelDimensions.toRtPixelDimensions(): RtPixelDimensions {
    return RtPixelDimensions(width, height)
}

/** Extension function that converts a [RtPixelDimensions] to [PixelDimensions]. */
internal fun RtPixelDimensions.toPixelDimensions(): PixelDimensions {
    return PixelDimensions(width, height)
}

/** Extension function that converts [Int] to [JxrPlatformAdapter.PlaneType]. */
internal fun Int.toRtPlaneType(): JxrPlatformAdapter.PlaneType {
    return when (this) {
        PlaneType.HORIZONTAL -> JxrPlatformAdapter.PlaneType.HORIZONTAL
        PlaneType.VERTICAL -> JxrPlatformAdapter.PlaneType.VERTICAL
        PlaneType.ANY -> JxrPlatformAdapter.PlaneType.ANY
        else -> error("Unknown Plane Type: $PlaneType")
    }
}

/** Extension function that converts [Int] to [JxrPlatformAdapter.PlaneSemantic]. */
internal fun Int.toRtPlaneSemantic(): JxrPlatformAdapter.PlaneSemantic {
    return when (this) {
        PlaneSemantic.WALL -> JxrPlatformAdapter.PlaneSemantic.WALL
        PlaneSemantic.FLOOR -> JxrPlatformAdapter.PlaneSemantic.FLOOR
        PlaneSemantic.CEILING -> JxrPlatformAdapter.PlaneSemantic.CEILING
        PlaneSemantic.TABLE -> JxrPlatformAdapter.PlaneSemantic.TABLE
        PlaneSemantic.ANY -> JxrPlatformAdapter.PlaneSemantic.ANY
        else -> error("Unknown Plane Semantic: $PlaneSemantic")
    }
}

/** Extension function that converts a [RtMoveEvent] to a [MoveEvent]. */
internal fun RtMoveEvent.toMoveEvent(entityManager: EntityManager): MoveEvent {

    disposedEntity?.let { entityManager.removeEntity(it) }
    return MoveEvent(
        moveState.toMoveState(),
        Ray(initialInputRay.origin, initialInputRay.direction),
        Ray(currentInputRay.origin, currentInputRay.direction),
        previousPose,
        currentPose,
        previousScale.x,
        currentScale.x,
        entityManager.getEntityForRtEntity(initialParent)!!,
        updatedParent?.let {
            entityManager.getEntityForRtEntity(it)
                ?: AnchorEntity.create(it as JxrPlatformAdapter.AnchorEntity, entityManager)
        },
    )
}

/** Extension function that converts a [RtHitInfo] to a [HitInfo]. */
internal fun RtHitInfo.toHitInfo(entityManager: EntityManager): HitInfo? {
    // TODO: b/377541143 - Replace instance equality check in EntityManager.
    val hitEntity = inputEntity?.let { entityManager.getEntityForRtEntity(it) }
    return if (hitEntity == null) {
        null
    } else {
        HitInfo(inputEntity = hitEntity, hitPosition = hitPosition, transform = transform)
    }
}

/** Extension function that converts a [RtInputEvent] to a [InputEvent]. */
internal fun RtInputEvent.toInputEvent(entityManager: EntityManager): InputEvent {
    return InputEvent(
        source.toInputEventSource(),
        pointerType.toInputEventPointerType(),
        timestamp,
        origin,
        direction,
        action.toInputEventAction(),
        hitInfo?.toHitInfo(entityManager),
        secondaryHitInfo?.toHitInfo(entityManager),
    )
}

/** Extension function that converts a [RtSpatialCapabilities] to a [SpatialCapabilities]. */
internal fun RtSpatialCapabilities.toSpatialCapabilities(): SpatialCapabilities {
    return SpatialCapabilities(capabilities.toSpatialCapability())
}

/** Extension function that converts a [RtResizeEvent] to a [ResizeEvent]. */
internal fun RtResizeEvent.toResizeEvent(): ResizeEvent {
    return ResizeEvent(resizeState.toResizeState(), newSize.toDimensions())
}

/**
 * Extension function that converts a [Set] of [AnchorPlacement] to a [Set] of
 * [JxrPlatformAdapter.AnchorPlacement].
 */
internal fun Set<AnchorPlacement>.toRtAnchorPlacement(
    runtime: JxrPlatformAdapter
): Set<JxrPlatformAdapter.AnchorPlacement> {
    val rtAnchorPlacementSet = HashSet<JxrPlatformAdapter.AnchorPlacement>()
    for (placement in this) {
        val planeTypeFilter = placement.planeTypeFilter.map { it.toRtPlaneType() }.toMutableSet()
        val planeSemanticFilter =
            placement.planeSemanticFilter.map { it.toRtPlaneSemantic() }.toMutableSet()

        val rtAnchorPlacement =
            runtime.createAnchorPlacementForPlanes(planeTypeFilter, planeSemanticFilter)
        rtAnchorPlacementSet.add(rtAnchorPlacement)
    }
    return rtAnchorPlacementSet
}

/** Extension function that converts a [Int] to [MoveEvent.MoveState]. */
@MoveEvent.MoveState
internal fun Int.toMoveState(): Int {
    return when (this) {
        JxrPlatformAdapter.MoveEvent.MOVE_STATE_START -> MoveEvent.MOVE_STATE_START
        JxrPlatformAdapter.MoveEvent.MOVE_STATE_ONGOING -> MoveEvent.MOVE_STATE_ONGOING
        JxrPlatformAdapter.MoveEvent.MOVE_STATE_END -> MoveEvent.MOVE_STATE_END
        else -> error("Unknown Move State: $this")
    }
}

/** Extension function that converts a [Int] to [ResizeEvent.ResizeState]. */
@ResizeEvent.ResizeState
internal fun Int.toResizeState(): Int {
    return when (this) {
        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_UNKNOWN -> ResizeEvent.RESIZE_STATE_UNKNOWN
        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_START -> ResizeEvent.RESIZE_STATE_START
        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_ONGOING -> ResizeEvent.RESIZE_STATE_ONGOING
        JxrPlatformAdapter.ResizeEvent.RESIZE_STATE_END -> ResizeEvent.RESIZE_STATE_END
        else -> error("Unknown Resize State: $this")
    }
}

/** Extension function that converts a [Int] to [InputEvent.Source]. */
@InputEvent.Source
internal fun Int.toInputEventSource(): Int {
    return when (this) {
        JxrPlatformAdapter.InputEvent.SOURCE_UNKNOWN -> InputEvent.SOURCE_UNKNOWN
        JxrPlatformAdapter.InputEvent.SOURCE_HEAD -> InputEvent.SOURCE_HEAD
        JxrPlatformAdapter.InputEvent.SOURCE_CONTROLLER -> InputEvent.SOURCE_CONTROLLER
        JxrPlatformAdapter.InputEvent.SOURCE_HANDS -> InputEvent.SOURCE_HANDS
        JxrPlatformAdapter.InputEvent.SOURCE_MOUSE -> InputEvent.SOURCE_MOUSE
        JxrPlatformAdapter.InputEvent.SOURCE_GAZE_AND_GESTURE -> InputEvent.SOURCE_GAZE_AND_GESTURE
        else -> error("Unknown Input Event Source: $this")
    }
}

/** Extension function that converts a [Int] to [InputEvent.PointerType]. */
@InputEvent.PointerType
internal fun Int.toInputEventPointerType(): Int {
    return when (this) {
        JxrPlatformAdapter.InputEvent.POINTER_TYPE_DEFAULT -> InputEvent.POINTER_TYPE_DEFAULT
        JxrPlatformAdapter.InputEvent.POINTER_TYPE_LEFT -> InputEvent.POINTER_TYPE_LEFT
        JxrPlatformAdapter.InputEvent.POINTER_TYPE_RIGHT -> InputEvent.POINTER_TYPE_RIGHT
        else -> error("Unknown Input Event Pointer Type: $this")
    }
}

/** Extension function that converts a [Int] to [SpatialCapabilities.SpatialCapability]. */
@SpatialCapabilities.SpatialCapability
internal fun Int.toSpatialCapability(): Int {
    return this
}

/** Extension function that converts a [Int] to [InputEvent.Action]. */
@InputEvent.Action
internal fun Int.toInputEventAction(): Int {
    return when (this) {
        JxrPlatformAdapter.InputEvent.ACTION_DOWN -> InputEvent.ACTION_DOWN
        JxrPlatformAdapter.InputEvent.ACTION_UP -> InputEvent.ACTION_UP
        JxrPlatformAdapter.InputEvent.ACTION_MOVE -> InputEvent.ACTION_MOVE
        JxrPlatformAdapter.InputEvent.ACTION_CANCEL -> InputEvent.ACTION_CANCEL
        JxrPlatformAdapter.InputEvent.ACTION_HOVER_MOVE -> InputEvent.ACTION_HOVER_MOVE
        JxrPlatformAdapter.InputEvent.ACTION_HOVER_ENTER -> InputEvent.ACTION_HOVER_ENTER
        JxrPlatformAdapter.InputEvent.ACTION_HOVER_EXIT -> InputEvent.ACTION_HOVER_EXIT
        else -> error("Unknown Input Event Action: $this")
    }
}
