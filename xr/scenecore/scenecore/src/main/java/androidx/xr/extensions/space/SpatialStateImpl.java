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

import android.util.Size;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.environment.EnvironmentTypeConverter;
import androidx.xr.extensions.environment.EnvironmentVisibilityState;
import androidx.xr.extensions.environment.PassthroughVisibilityState;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTypeConverter;

class SpatialStateImpl implements SpatialState {
    @NonNull private final com.android.extensions.xr.space.SpatialState mState;

    SpatialStateImpl(@NonNull com.android.extensions.xr.space.SpatialState state) {
        mState = state;
    }

    @Override
    public @NonNull Bounds getBounds() {
        return SpaceTypeConverter.toLibrary(mState.getBounds());
    }

    @Override
    public @NonNull SpatialCapabilities getSpatialCapabilities() {
        return SpaceTypeConverter.toLibrary(mState.getSpatialCapabilities());
    }

    @Override
    public @NonNull EnvironmentVisibilityState getEnvironmentVisibility() {
        return EnvironmentTypeConverter.toLibrary(mState.getEnvironmentVisibility());
    }

    @Override
    public @NonNull PassthroughVisibilityState getPassthroughVisibility() {
        return EnvironmentTypeConverter.toLibrary(mState.getPassthroughVisibility());
    }

    @Override
    public boolean isActiveSceneNode(@Nullable Node targetNode) {
        return mState.isActiveSceneNode(NodeTypeConverter.toFramework(targetNode));
    }

    @Override
    public boolean isActiveWindowLeashNode(@Nullable Node targetNode) {
        return mState.isActiveWindowLeashNode(NodeTypeConverter.toFramework(targetNode));
    }

    @Override
    public boolean isActiveEnvironmentNode(@Nullable Node targetNode) {
        return mState.isActiveEnvironmentNode(NodeTypeConverter.toFramework(targetNode));
    }

    @Override
    public boolean isEnvironmentInherited() {
        return mState.isEnvironmentInherited();
    }

    @Override
    public @NonNull Size getMainWindowSize() {
        return mState.getMainWindowSize();
    }

    @Override
    public float getPreferredAspectRatio() {
        return mState.getPreferredAspectRatio();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof SpatialStateImpl)) {
            return false;
        }

        SpatialStateImpl impl = (SpatialStateImpl) other;
        return mState.equals(impl.mState);
    }

    @Override
    public int hashCode() {
        return mState.hashCode();
    }

    @Override
    public String toString() {
        return mState.toString();
    }
}
