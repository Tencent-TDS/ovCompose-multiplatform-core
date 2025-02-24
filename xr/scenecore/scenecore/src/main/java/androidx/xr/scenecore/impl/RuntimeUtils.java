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

package androidx.xr.scenecore.impl;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.node.InputEvent.Action;
import androidx.xr.extensions.node.InputEvent.PointerType;
import androidx.xr.extensions.node.InputEvent.Source;
import androidx.xr.extensions.node.Mat4f;
import androidx.xr.extensions.node.Quatf;
import androidx.xr.extensions.node.ReformEvent;
import androidx.xr.extensions.node.Vec3;
import androidx.xr.runtime.math.Matrix4;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Quaternion;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose.Fov;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEvent.HitInfo;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneSemantic;
import androidx.xr.scenecore.JxrPlatformAdapter.PlaneType;
import androidx.xr.scenecore.JxrPlatformAdapter.ResizeEvent;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatialCapabilities;
import androidx.xr.scenecore.impl.perception.Plane;

final class RuntimeUtils {
    private RuntimeUtils() {}

    /** Convert JXRCore PlaneType to a PerceptionLibrary Plane.Type. */
    static Plane.Type getPlaneType(PlaneType planeType) {
        switch (planeType) {
            case HORIZONTAL:
                // TODO: b/329888869 - Allow Horizontal to work as both upward and downward facing.
                // To do
                // this it would have to return a collection.
                return Plane.Type.HORIZONTAL_UPWARD_FACING;
            case VERTICAL:
                return Plane.Type.VERTICAL;
            case ANY:
                return Plane.Type.ARBITRARY;
        }
        return Plane.Type.ARBITRARY;
    }

    /** Convert a Perception Plane.Type to a JXRCore PlaneType. */
    static PlaneType getPlaneType(Plane.Type planeType) {
        switch (planeType) {
            case HORIZONTAL_UPWARD_FACING:
            case HORIZONTAL_DOWNWARD_FACING:
                return PlaneType.HORIZONTAL;
            case VERTICAL:
                return PlaneType.VERTICAL;
            default:
                return PlaneType.ANY;
        }
    }

    /** Convert a JXRCore PlaneSemantic to a PerceptionLibrary Plane.Label. */
    static Plane.Label getPlaneLabel(PlaneSemantic planeSemantic) {
        switch (planeSemantic) {
            case WALL:
                return Plane.Label.WALL;
            case FLOOR:
                return Plane.Label.FLOOR;
            case CEILING:
                return Plane.Label.CEILING;
            case TABLE:
                return Plane.Label.TABLE;
            case ANY:
                return Plane.Label.UNKNOWN;
        }
        return Plane.Label.UNKNOWN;
    }

    /** Convert a PerceptionLibrary Plane.Label to a JXRCore PlaneSemantic. */
    static PlaneSemantic getPlaneSemantic(Plane.Label planeLabel) {
        switch (planeLabel) {
            case WALL:
                return PlaneSemantic.WALL;
            case FLOOR:
                return PlaneSemantic.FLOOR;
            case CEILING:
                return PlaneSemantic.CEILING;
            case TABLE:
                return PlaneSemantic.TABLE;
            default:
                return PlaneSemantic.ANY;
        }
    }

    @Nullable
    private static HitInfo getHitInfo(
            androidx.xr.extensions.node.InputEvent.HitInfo xrHitInfo, EntityManager entityManager) {
        if (xrHitInfo == null
                || xrHitInfo.getInputNode() == null
                || xrHitInfo.getTransform() == null) {
            return null;
        }
        // TODO: b/377541143 - Replace instance equality check in EntityManager.
        Entity hitEntity = entityManager.getEntityForNode(xrHitInfo.getInputNode());
        if (hitEntity == null) {
            return null;
        }
        return new HitInfo(
                entityManager.getEntityForNode(xrHitInfo.getInputNode()),
                (xrHitInfo.getHitPosition() == null)
                        ? null
                        : getVector3(xrHitInfo.getHitPosition()),
                getMatrix(xrHitInfo.getTransform()));
    }

    /**
     * Converts an XR InputEvent to a JXRCore InputEvent.
     *
     * @param xrInputEvent an {@link androidx.xr.extensions.node.InputEvent} instance to be
     *     converted.
     * @param entityManager an {@link EntityManager} instance to look up entities.
     * @return a {@link androidx.xr.scenecore.JXRCoreRuntime.InputEvent} instance representing the
     *     input event.
     */
    static InputEvent getInputEvent(
            @NonNull androidx.xr.extensions.node.InputEvent xrInputEvent,
            @NonNull EntityManager entityManager) {
        Vector3 origin = getVector3(xrInputEvent.getOrigin());
        Vector3 direction = getVector3(xrInputEvent.getDirection());
        HitInfo hitInfo = null;
        HitInfo secondaryHitInfo = null;
        if (xrInputEvent.getHitInfo() != null) {
            hitInfo = getHitInfo(xrInputEvent.getHitInfo(), entityManager);
        }
        if (xrInputEvent.getSecondaryHitInfo() != null) {
            secondaryHitInfo = getHitInfo(xrInputEvent.getSecondaryHitInfo(), entityManager);
        }
        return new InputEvent(
                getInputEventSource(xrInputEvent.getSource()),
                getInputEventPointerType(xrInputEvent.getPointerType()),
                xrInputEvent.getTimestamp(),
                origin,
                direction,
                getInputEventAction(xrInputEvent.getAction()),
                hitInfo,
                secondaryHitInfo);
    }

    @InputEvent.Source
    static int getInputEventSource(@Source int xrInputEventSource) {
        switch (xrInputEventSource) {
            case androidx.xr.extensions.node.InputEvent.SOURCE_UNKNOWN:
                return InputEvent.SOURCE_UNKNOWN;
            case androidx.xr.extensions.node.InputEvent.SOURCE_HEAD:
                return InputEvent.SOURCE_HEAD;
            case androidx.xr.extensions.node.InputEvent.SOURCE_CONTROLLER:
                return InputEvent.SOURCE_CONTROLLER;
            case androidx.xr.extensions.node.InputEvent.SOURCE_HANDS:
                return InputEvent.SOURCE_HANDS;
            case androidx.xr.extensions.node.InputEvent.SOURCE_MOUSE:
                return InputEvent.SOURCE_MOUSE;
            case androidx.xr.extensions.node.InputEvent.SOURCE_GAZE_AND_GESTURE:
                return InputEvent.SOURCE_GAZE_AND_GESTURE;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Source: " + xrInputEventSource);
        }
    }

    @InputEvent.PointerType
    static int getInputEventPointerType(@PointerType int xrInputEventPointerType) {
        switch (xrInputEventPointerType) {
            case androidx.xr.extensions.node.InputEvent.POINTER_TYPE_DEFAULT:
                return InputEvent.POINTER_TYPE_DEFAULT;
            case androidx.xr.extensions.node.InputEvent.POINTER_TYPE_LEFT:
                return InputEvent.POINTER_TYPE_LEFT;
            case androidx.xr.extensions.node.InputEvent.POINTER_TYPE_RIGHT:
                return InputEvent.POINTER_TYPE_RIGHT;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Pointer Type: " + xrInputEventPointerType);
        }
    }

    @InputEvent.Action
    static int getInputEventAction(@Action int xrInputEventAction) {
        switch (xrInputEventAction) {
            case androidx.xr.extensions.node.InputEvent.ACTION_DOWN:
                return InputEvent.ACTION_DOWN;
            case androidx.xr.extensions.node.InputEvent.ACTION_UP:
                return InputEvent.ACTION_UP;
            case androidx.xr.extensions.node.InputEvent.ACTION_MOVE:
                return InputEvent.ACTION_MOVE;
            case androidx.xr.extensions.node.InputEvent.ACTION_CANCEL:
                return InputEvent.ACTION_CANCEL;
            case androidx.xr.extensions.node.InputEvent.ACTION_HOVER_MOVE:
                return InputEvent.ACTION_HOVER_MOVE;
            case androidx.xr.extensions.node.InputEvent.ACTION_HOVER_ENTER:
                return InputEvent.ACTION_HOVER_ENTER;
            case androidx.xr.extensions.node.InputEvent.ACTION_HOVER_EXIT:
                return InputEvent.ACTION_HOVER_EXIT;
            default:
                throw new IllegalArgumentException(
                        "Unknown Input Event Action: " + xrInputEventAction);
        }
    }

    @ResizeEvent.ResizeState
    static int getResizeEventState(@ReformEvent.ReformState int resizeState) {
        switch (resizeState) {
            case ReformEvent.REFORM_STATE_UNKNOWN:
                return ResizeEvent.RESIZE_STATE_UNKNOWN;
            case ReformEvent.REFORM_STATE_START:
                return ResizeEvent.RESIZE_STATE_START;
            case ReformEvent.REFORM_STATE_ONGOING:
                return ResizeEvent.RESIZE_STATE_ONGOING;
            case ReformEvent.REFORM_STATE_END:
                return ResizeEvent.RESIZE_STATE_END;
            default:
                throw new IllegalArgumentException("Unknown Resize State: " + resizeState);
        }
    }

    static Matrix4 getMatrix(Mat4f xrMatrix) {
        float[] matrixData = new float[16];
        for (int i = 0; i < 4; i++) {
            System.arraycopy(xrMatrix.m[i], 0, matrixData, i * 4, 4);
        }
        return new Matrix4(matrixData);
    }

    static Pose getPose(Vec3 position, Quatf quatf) {
        return new Pose(
                new Vector3(position.x, position.y, position.z),
                new Quaternion(quatf.x, quatf.y, quatf.z, quatf.w));
    }

    static Vector3 getVector3(Vec3 vec3) {
        return new Vector3(vec3.x, vec3.y, vec3.z);
    }

    /**
     * Converts from a perception pose type.
     *
     * @param perceptionPose a {@code androidx.xr.scenecore.impl.perception.Pose} instance
     *     representing the pose.
     */
    static Pose fromPerceptionPose(androidx.xr.scenecore.impl.perception.Pose perceptionPose) {
        Vector3 translation =
                new Vector3(perceptionPose.tx(), perceptionPose.ty(), perceptionPose.tz());
        Quaternion rotation =
                new Quaternion(
                        perceptionPose.qx(),
                        perceptionPose.qy(),
                        perceptionPose.qz(),
                        perceptionPose.qw());
        return new Pose(translation, rotation);
    }

    /**
     * Converts from a pose to a perception pose type.
     *
     * @param pose a {@code androidx.xr.runtime.math.Pose} instance representing the pose.
     */
    static androidx.xr.scenecore.impl.perception.Pose poseToPerceptionPose(Pose pose) {
        return new androidx.xr.scenecore.impl.perception.Pose(
                pose.getTranslation().getX(),
                pose.getTranslation().getY(),
                pose.getTranslation().getZ(),
                pose.getRotation().getX(),
                pose.getRotation().getY(),
                pose.getRotation().getZ(),
                pose.getRotation().getW());
    }

    /**
     * Converts to a JXRCore FOV from a perception FOV type.
     *
     * @param perceptionFov a {@code androidx.xr.scenecore.impl.perception.Fov} instance
     *     representing the FOV.
     */
    static Fov fovFromPerceptionFov(androidx.xr.scenecore.impl.perception.Fov perceptionFov) {
        return new Fov(
                perceptionFov.getAngleLeft(),
                perceptionFov.getAngleRight(),
                perceptionFov.getAngleUp(),
                perceptionFov.getAngleDown());
    }

    /**
     * Converts to a perception FOV from a JXRCore FOV type.
     *
     * @param fov a {@code androidx.xr.scenecore.JxrPlatformAdapter.CameraViewActivityPose.Fov}
     *     instance representing the FOV.
     */
    static androidx.xr.scenecore.impl.perception.Fov perceptionFovFromFov(Fov fov) {
        return new androidx.xr.scenecore.impl.perception.Fov(
                fov.angleLeft, fov.angleRight, fov.angleUp, fov.angleDown);
    }

    /**
     * Converts from the Extensions spatial capabilities to the runtime spatial capabilities.
     *
     * @param extCapabilities a {@link androidx.xr.extensions.space.SpatialCapabilities} instance to
     *     be converted.
     */
    static SpatialCapabilities convertSpatialCapabilities(
            androidx.xr.extensions.space.SpatialCapabilities extCapabilities) {
        @SpatialCapabilities.SpatialCapability int capabilities = 0;
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_UI_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_UI;
        }
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_3D_CONTENTS_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_3D_CONTENT;
        }
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities.PASSTHROUGH_CONTROL_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_PASSTHROUGH_CONTROL;
        }
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities.APP_ENVIRONMENTS_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_APP_ENVIRONMENT;
        }
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities.SPATIAL_AUDIO_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_SPATIAL_AUDIO;
        }
        if (extCapabilities.get(
                androidx.xr.extensions.space.SpatialCapabilities
                        .SPATIAL_ACTIVITY_EMBEDDING_CAPABLE)) {
            capabilities |= SpatialCapabilities.SPATIAL_CAPABILITY_EMBED_ACTIVITY;
        }

        return new SpatialCapabilities(capabilities);
    }

    /**
     * Converts from the Extensions environment visibility state to the runtime environment
     * visibility state.
     *
     * @param environmentState a {@link
     *     androidx.xr.extensions.environment.EnvironmentVisibilityState} instance to be converted.
     */
    static boolean getIsSpatialEnvironmentPreferenceActive(
            @EnvironmentVisibilityState.State int environmentState) {
        return environmentState == EnvironmentVisibilityState.APP_VISIBLE;
    }

    static float getPassthroughOpacity(PassthroughVisibilityState passthroughVisibilityState) {
        int passthroughState = passthroughVisibilityState.getCurrentState();
        if (passthroughState == PassthroughVisibilityState.DISABLED) {
            return 0.0f;
        } else {
            float opacity = passthroughVisibilityState.getOpacity();
            if (opacity > 0.0f) {
                return opacity;
            } else {
                // When passthrough is enabled, the opacity should be greater than zero.
                Log.e(
                        "RuntimeUtils",
                        "Passthrough is enabled, but active opacity value is "
                                + opacity
                                + ". Opacity should be greater than zero when Passthrough is"
                                + " enabled.");
                return 1.0f;
            }
        }
    }

    /**
     * Converts from JXR Core's TextureSampler to Impress' API bindings TextureSampler.
     *
     * @param sampler a {@link androidx.xr.scenecore.TextureSampler} instance to be converted.
     */
    static com.google.ar.imp.apibindings.TextureSampler getTextureSampler(
            @NonNull androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler sampler) {
        return new com.google.ar.imp.apibindings.TextureSampler.Builder()
                .setMinFilter(getMinFilter(sampler.getMinFilter()))
                .setMagFilter(getMagFilter(sampler.getMagFilter()))
                .setWrapModeS(getWrapMode(sampler.getWrapModeS()))
                .setWrapModeT(getWrapMode(sampler.getWrapModeT()))
                .setWrapModeR(getWrapMode(sampler.getWrapModeR()))
                .setCompareMode(getCompareModeValue(sampler.getCompareMode()))
                .setCompareFunc(getCompareFuncValue(sampler.getCompareFunc()))
                .setAnisotropyLog2(sampler.getAnisotropyLog2())
                .build();
    }

    private static com.google.ar.imp.apibindings.TextureSampler.WrapMode getWrapMode(
            @androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.WrapMode int wrapMode) {
        switch (wrapMode) {
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.CLAMP_TO_EDGE:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.CLAMP_TO_EDGE;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.REPEAT:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.REPEAT;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.MIRRORED_REPEAT:
                return com.google.ar.imp.apibindings.TextureSampler.WrapMode.MIRRORED_REPEAT;
            default:
                throw new IllegalArgumentException("Unknown WrapMode value: " + wrapMode);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.MinFilter getMinFilter(
            @androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.MinFilter int minFilter) {
        switch (minFilter) {
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.NEAREST;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.NEAREST_MIPMAP_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter
                        .NEAREST_MIPMAP_NEAREST;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.LINEAR_MIPMAP_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR_MIPMAP_NEAREST;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.NEAREST_MIPMAP_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.NEAREST_MIPMAP_LINEAR;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.LINEAR_MIPMAP_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR;
            default:
                throw new IllegalArgumentException("Unknown MinFilter value: " + minFilter);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.MagFilter getMagFilter(
            @androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.MagFilter int magFilter) {
        switch (magFilter) {
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.MAG_NEAREST:
                return com.google.ar.imp.apibindings.TextureSampler.MagFilter.NEAREST;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.MAG_LINEAR:
                return com.google.ar.imp.apibindings.TextureSampler.MagFilter.LINEAR;
            default:
                throw new IllegalArgumentException("Unknown MagFilter value: " + magFilter);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.CompareMode getCompareModeValue(
            @androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.CompareMode int compareMode) {
        switch (compareMode) {
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.NONE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareMode.NONE;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.COMPARE_TO_TEXTURE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareMode.COMPARE_TO_TEXTURE;
            default:
                throw new IllegalArgumentException("Unknown CompareMode value: " + compareMode);
        }
    }

    private static com.google.ar.imp.apibindings.TextureSampler.CompareFunc getCompareFuncValue(
            @androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.CompareFunc int compareFunc) {
        switch (compareFunc) {
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.LE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.LE;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.GE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.GE;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.L:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.L;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.G:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.G;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.E:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.E;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.NE:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.NE;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.A:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.A;
            case androidx.xr.scenecore.JxrPlatformAdapter.TextureSampler.N:
                return com.google.ar.imp.apibindings.TextureSampler.CompareFunc.N;
            default:
                throw new IllegalArgumentException("Unknown CompareFunc value: " + compareFunc);
        }
    }
}
