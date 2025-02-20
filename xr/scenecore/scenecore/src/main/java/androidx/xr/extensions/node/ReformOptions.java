/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.node;

import static androidx.xr.extensions.XrExtensions.IMAGE_TOO_OLD;

import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.Consumer;

import java.lang.annotation.Retention;
import java.util.concurrent.Executor;

/**
 * Configuration options for reform (move/resize) UX. To create a ReformOptions instance, call
 * {@code XrExtensions.createReformOptions()}.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface ReformOptions {
    // clang-format off
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            flag = true,
            value = {ALLOW_MOVE, ALLOW_RESIZE})
    @Retention(SOURCE)
    @interface AllowedReformTypes {}

    // clang-format on

    int ALLOW_MOVE = 1;
    int ALLOW_RESIZE = 2;

    /** Which reform actions are enabled. */
    @AllowedReformTypes
    int getEnabledReform();

    /** By default, only ALLOW_MOVE is enabled. */
    @NonNull
    ReformOptions setEnabledReform(@AllowedReformTypes int enabledReform);

    // clang-format off
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(
            flag = true,
            value = {
                FLAG_SCALE_WITH_DISTANCE,
                FLAG_ALLOW_SYSTEM_MOVEMENT,
                FLAG_POSE_RELATIVE_TO_PARENT
            })
    @Retention(SOURCE)
    public @interface ReformFlags {}

    // clang-format on

    int FLAG_SCALE_WITH_DISTANCE = 1;
    int FLAG_ALLOW_SYSTEM_MOVEMENT = 2;
    int FLAG_POSE_RELATIVE_TO_PARENT = 4;

    /** Behaviour flags. */
    @ReformFlags
    int getFlags();

    /** By default, the flags are set to 0. */
    @NonNull
    ReformOptions setFlags(@ReformFlags int flags);

    /**
     * Current size of the content, in meters. This is the local size (does not include any scale
     * factors)
     */
    @NonNull
    Vec3 getCurrentSize();

    /** By default, the current size is set to (1, 1, 1). */
    @NonNull
    ReformOptions setCurrentSize(@NonNull Vec3 currentSize);

    /** Minimum size of the content, in meters. This is a local size. */
    @NonNull
    Vec3 getMinimumSize();

    /** By default, the minimum size is set to (1, 1, 1). */
    @NonNull
    ReformOptions setMinimumSize(@NonNull Vec3 minimumSize);

    /** Maximum size of the content, in meters. This is a local size. */
    @NonNull
    Vec3 getMaximumSize();

    /** By default, the maximum size is set to (1, 1, 1). */
    @NonNull
    ReformOptions setMaximumSize(@NonNull Vec3 maximumSize);

    /** The aspect ratio of the content on resizing. <= 0.0f when there are no preferences. */
    float getFixedAspectRatio();

    /**
     * The aspect ratio determined by taking the panel's width over its height. An aspect ratio
     * value less than 0 will be ignored. A value <= 0.0f means there are no preferences.
     *
     * <p>This method does not immediately resize the entity. The new aspect ratio will be applied
     * the next time the user resizes the entity through the reform UI. During this resize
     * operation, the entity's current area will be preserved.
     *
     * <p>If a different resizing behavior is desired, such as fixing the width and adjusting the
     * height, the client can manually resize the entity to the preferred dimensions before calling
     * this method. No automatic resizing will occur when using the reform UI then.
     */
    @NonNull
    ReformOptions setFixedAspectRatio(float fixedAspectRatio);

    /** Returns the current value of forceShowResizeOverlay. */
    default boolean getForceShowResizeOverlay() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * If forceShowResizeOverlay is set to true, the resize overlay will always be show (until
     * forceShowResizeOverlay is changed to false). This can be used by apps to implement their own
     * resize affordances.
     */
    @NonNull
    default ReformOptions setForceShowResizeOverlay(boolean forceShowResizeOverlay) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /** Returns the callback that will receive reform events. */
    @NonNull
    Consumer<ReformEvent> getEventCallback();

    /** Sets the callback that will receive reform events. */
    @NonNull
    ReformOptions setEventCallback(@NonNull Consumer<ReformEvent> callback);

    /** Returns the executor that events will be handled on. */
    @NonNull
    Executor getEventExecutor();

    /** Sets the executor that events will be handled on. */
    @NonNull
    ReformOptions setEventExecutor(@NonNull Executor executor);

    // clang-format off
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @IntDef(value = {SCALE_WITH_DISTANCE_MODE_DEFAULT, SCALE_WITH_DISTANCE_MODE_DMM})
    @Retention(SOURCE)
    @interface ScaleWithDistanceMode {}

    // clang-format on

    // The values MUST be identical to the ScalingCurvePreset enum for Spaceflinger in
    // vendor/google/ix/sysui/proto/components/freeform_positioning_state.proto.
    int SCALE_WITH_DISTANCE_MODE_DMM = 2;
    int SCALE_WITH_DISTANCE_MODE_DEFAULT = 3;

    /** Returns the current value of scaleWithDistanceMode. */
    default @ScaleWithDistanceMode int getScaleWithDistanceMode() {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }

    /**
     * If scaleWithDistanceMode is set, and FLAG_SCALE_WITH_DISTANCE is also in use, the scale the
     * system suggests (or automatically applies when FLAG_ALLOW_SYSTEM_MOVEMENT is also in use)
     * follows scaleWithDistanceMode:
     *
     * <p>SCALE_WITH_DISTANCE_MODE_DEFAULT: The panel scales in the same way as home space mode.
     * SCALE_WITH_DISTANCE_MODE_DMM: The panel scales in a way that the user-perceived panel size
     * never changes.
     *
     * <p>When FLAG_SCALE_WITH_DISTANCE is not in use, scaleWithDistanceMode is ignored.
     */
    @NonNull
    default ReformOptions setScaleWithDistanceMode(
            @ScaleWithDistanceMode int scaleWithDistanceMode) {
        throw new UnsupportedOperationException(IMAGE_TOO_OLD);
    }
}
