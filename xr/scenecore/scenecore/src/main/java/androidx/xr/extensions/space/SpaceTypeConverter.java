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

package androidx.xr.extensions.space;

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.xr.extensions.node.NodeTypeConverter;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SpaceTypeConverter {

    private SpaceTypeConverter() {}

    /**
     * Converts a {@link Bounds} to a framework type.
     *
     * @param bounds The {@link Bounds} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.space.Bounds}.
     */
    @NonNull
    public static com.android.extensions.xr.space.Bounds toFramework(@NonNull Bounds bounds) {
        requireNonNull(bounds);

        return new com.android.extensions.xr.space.Bounds(
                bounds.width, bounds.height, bounds.depth);
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.Bounds} to a library type.
     *
     * @param bounds The {@link com.android.extensions.xr.space.Bounds} to convert.
     * @return The library type of the {@link Bounds}.
     */
    @NonNull
    public static Bounds toLibrary(@NonNull com.android.extensions.xr.space.Bounds bounds) {
        requireNonNull(bounds);

        return new Bounds(bounds.getWidth(), bounds.getHeight(), bounds.getDepth());
    }

    /**
     * Converts a {@link SpatialCapabilities} to a framework type.
     *
     * @param capabilities The {@link SpatialCapabilities} to convert.
     * @return The framework type of the {@link
     *     com.android.extensions.xr.space.SpatialCapabilities}.
     */
    @NonNull
    public static com.android.extensions.xr.space.SpatialCapabilities toFramework(
            @NonNull SpatialCapabilitiesImpl capabilities) {
        requireNonNull(capabilities);

        return capabilities.mCapabilities;
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.SpatialCapabilities} to a library type.
     *
     * @param capabilities The {@link com.android.extensions.xr.space.SpatialCapabilities} to
     *     convert.
     * @return The library type of the {@link SpatialCapabilities}.
     */
    @NonNull
    public static SpatialCapabilitiesImpl toLibrary(
            @NonNull com.android.extensions.xr.space.SpatialCapabilities capabilities) {
        requireNonNull(capabilities);

        return new SpatialCapabilitiesImpl(capabilities);
    }

    /**
     * Converts a {@link HitTestResult} to a framework type.
     *
     * @param result The {@link HitTestResult} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.space.HitTestResult}.
     */
    @NonNull
    public static com.android.extensions.xr.space.HitTestResult toFramework(
            @NonNull HitTestResult result) {
        requireNonNull(result);

        com.android.extensions.xr.space.HitTestResult.Builder builder =
                new com.android.extensions.xr.space.HitTestResult.Builder(
                        result.distance,
                        NodeTypeConverter.toFramework(result.hitPosition),
                        result.virtualEnvironmentIsVisible,
                        result.surfaceType);

        if (result.surfaceNormal != null) {
            builder.setSurfaceNormal(NodeTypeConverter.toFramework(result.surfaceNormal));
        }

        return builder.build();
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.HitTestResult} to a library type.
     *
     * @param result The {@link com.android.extensions.xr.space.HitTestResult} to convert.
     * @return The library type of the {@link HitTestResult}.
     */
    @NonNull
    public static HitTestResult toLibrary(
            @NonNull com.android.extensions.xr.space.HitTestResult result) {
        requireNonNull(result);

        HitTestResult hitTestResult = new HitTestResult();
        hitTestResult.distance = result.getDistance();
        hitTestResult.hitPosition = NodeTypeConverter.toLibrary(result.getHitPosition());
        if (result.getSurfaceNormal() != null) {
            hitTestResult.surfaceNormal = NodeTypeConverter.toLibrary(result.getSurfaceNormal());
        }
        hitTestResult.surfaceType = result.getSurfaceType();
        hitTestResult.virtualEnvironmentIsVisible = result.getVirtualEnvironmentIsVisible();

        return hitTestResult;
    }

    /**
     * Converts a {@link ActivityPanel} to a framework type.
     *
     * @param panel The {@link ActivityPanel} to convert.
     * @return The framework type of the {@link com.android.extensions.xr.space.ActivityPanel}.
     */
    @NonNull
    public static com.android.extensions.xr.space.ActivityPanel toFramework(
            @NonNull ActivityPanelImpl panel) {
        requireNonNull(panel);

        return panel.mActivityPanel;
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.ActivityPanel} to a library type.
     *
     * @param panel The {@link com.android.extensions.xr.space.ActivityPanel} to convert.
     * @return The library type of the {@link ActivityPanel}.
     */
    @NonNull
    public static ActivityPanelImpl toLibrary(
            @NonNull com.android.extensions.xr.space.ActivityPanel panel) {
        requireNonNull(panel);

        return new ActivityPanelImpl(panel);
    }

    /**
     * Converts a {@link ActivityPanelLaunchParameters} to a framework type.
     *
     * @param params The {@link ActivityPanelLaunchParameters} to convert.
     * @return The framework type of the {@link
     *     com.android.extensions.xr.space.ActivityPanelLaunchParameters}.
     */
    @NonNull
    public static com.android.extensions.xr.space.ActivityPanelLaunchParameters toFramework(
            @NonNull ActivityPanelLaunchParameters params) {
        requireNonNull(params);

        return new com.android.extensions.xr.space.ActivityPanelLaunchParameters(
                params.getWindowBounds());
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.ActivityPanelLaunchParameters} to a library
     * type.
     *
     * @param params The {@link com.android.extensions.xr.space.ActivityPanelLaunchParameters} to
     *     convert.
     * @return The library type of the {@link ActivityPanelLaunchParameters}.
     */
    @NonNull
    public static ActivityPanelLaunchParameters toLibrary(
            @NonNull com.android.extensions.xr.space.ActivityPanelLaunchParameters params) {
        requireNonNull(params);

        return new ActivityPanelLaunchParameters(params.getWindowBounds());
    }

    /**
     * Converts a {@link SpatialState} to a library type.
     *
     * @param state The {@link SpatialState} to convert.
     * @return The library type of the {@link SpatialState}.
     */
    @NonNull
    public static SpatialState toLibrary(
            @NonNull com.android.extensions.xr.space.SpatialState state) {
        requireNonNull(state);

        return new SpatialStateImpl(state);
    }

    /**
     * Converts a {@link SpatialStateEvent} to a library type.
     *
     * @param event The {@link SpatialStateEvent} to convert.
     * @return The library type of the {@link SpatialStateEvent}.
     */
    @NonNull
    public static SpatialStateEvent toLibrary(
            @NonNull com.android.extensions.xr.space.SpatialStateEvent event) {
        requireNonNull(event);

        if (event instanceof com.android.extensions.xr.space.BoundsChangeEvent) {
            return toLibrary((com.android.extensions.xr.space.BoundsChangeEvent) event);
        } else if (event instanceof com.android.extensions.xr.space.EnvironmentControlChangeEvent) {
            return toLibrary((com.android.extensions.xr.space.EnvironmentControlChangeEvent) event);
        } else if (event
                instanceof com.android.extensions.xr.space.EnvironmentVisibilityChangeEvent) {
            return toLibrary(
                    (com.android.extensions.xr.space.EnvironmentVisibilityChangeEvent) event);
        } else if (event instanceof com.android.extensions.xr.space.SpatialCapabilityChangeEvent) {
            return toLibrary((com.android.extensions.xr.space.SpatialCapabilityChangeEvent) event);
        }

        // TODO(bvanderlaan): Handle this error better.
        throw new IllegalArgumentException("Unknown event type " + event);
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.BoundsChangeEvent} to a library type.
     *
     * @param event The {@link com.android.extensions.xr.space.BoundsChangeEvent} to convert.
     * @return The library type of the {@link BoundsChangeEvent}.
     */
    @NonNull
    public static SpatialStateEvent toLibrary(
            @NonNull com.android.extensions.xr.space.BoundsChangeEvent event) {
        requireNonNull(event);

        return new BoundsChangeEvent(toLibrary(event.getBounds()));
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.EnvironmentControlChangeEvent} to a library
     * type.
     *
     * @param event The {@link com.android.extensions.xr.space.EnvironmentControlChangeEvent} to
     *     convert.
     * @return The library type of the {@link EnvironmentControlChangeEvent}.
     */
    @NonNull
    public static SpatialStateEvent toLibrary(
            @NonNull com.android.extensions.xr.space.EnvironmentControlChangeEvent event) {
        requireNonNull(event);

        return new EnvironmentControlChangeEvent(event.getEnvironmentControlAllowed());
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.EnvironmentVisibilityChangeEvent} to a
     * library type.
     *
     * @param event The {@link com.android.extensions.xr.space.EnvironmentVisibilityChangeEvent} to
     *     convert.
     * @return The library type of the {@link EnvironmentVisibilityChangeEvent}.
     */
    @NonNull
    public static SpatialStateEvent toLibrary(
            @NonNull com.android.extensions.xr.space.EnvironmentVisibilityChangeEvent event) {
        requireNonNull(event);

        return new EnvironmentVisibilityChangeEvent(event.getEnvironmentState());
    }

    /**
     * Converts a {@link com.android.extensions.xr.space.SpatialCapabilityChangeEvent} to a library
     * type.
     *
     * @param event The {@link com.android.extensions.xr.space.SpatialCapabilityChangeEvent} to
     *     convert.
     * @return The library type of the {@link SpatialCapabilityChangeEvent}.
     */
    @NonNull
    public static SpatialStateEvent toLibrary(
            @NonNull com.android.extensions.xr.space.SpatialCapabilityChangeEvent event) {
        requireNonNull(event);

        return new SpatialCapabilityChangeEvent(toLibrary(event.getCurrentCapabilities()));
    }
}
