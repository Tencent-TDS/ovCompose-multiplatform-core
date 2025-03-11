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

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.xr.extensions.Consumer;

import java.util.concurrent.Executor;

class ReformOptionsImpl implements ReformOptions {
    @NonNull final com.android.extensions.xr.node.ReformOptions mOptions;

    ReformOptionsImpl(@NonNull com.android.extensions.xr.node.ReformOptions options) {
        requireNonNull(options);
        mOptions = options;
    }

    static class ReformEventImpl implements ReformEvent {
        @NonNull final com.android.extensions.xr.node.ReformEvent mEvent;

        ReformEventImpl(@NonNull com.android.extensions.xr.node.ReformEvent event) {
            requireNonNull(event);
            mEvent = event;
        }

        @Override
        public int getType() {
            return mEvent.getType();
        }

        @Override
        public int getState() {
            return mEvent.getState();
        }

        @Override
        public int getId() {
            return mEvent.getId();
        }

        @Override
        @NonNull
        public Vec3 getInitialRayOrigin() {
            return NodeTypeConverter.toLibrary(mEvent.getInitialRayOrigin());
        }

        @Override
        @NonNull
        public Vec3 getInitialRayDirection() {
            return NodeTypeConverter.toLibrary(mEvent.getInitialRayDirection());
        }

        @Override
        @NonNull
        public Vec3 getCurrentRayOrigin() {
            return NodeTypeConverter.toLibrary(mEvent.getCurrentRayOrigin());
        }

        @Override
        @NonNull
        public Vec3 getCurrentRayDirection() {
            return NodeTypeConverter.toLibrary(mEvent.getCurrentRayDirection());
        }

        @Override
        @NonNull
        public Vec3 getProposedPosition() {
            return NodeTypeConverter.toLibrary(mEvent.getProposedPosition());
        }

        @Override
        @NonNull
        public Quatf getProposedOrientation() {
            return NodeTypeConverter.toLibrary(mEvent.getProposedOrientation());
        }

        @Override
        @NonNull
        public Vec3 getProposedScale() {
            return NodeTypeConverter.toLibrary(mEvent.getProposedScale());
        }

        @Override
        @NonNull
        public Vec3 getProposedSize() {
            return NodeTypeConverter.toLibrary(mEvent.getProposedSize());
        }
    }

    @Override
    public int getEnabledReform() {
        return mOptions.getEnabledReform();
    }

    @Override
    @NonNull
    public ReformOptions setEnabledReform(int enabledReform) {
        mOptions.setEnabledReform(enabledReform);
        return this;
    }

    @Override
    public int getFlags() {
        return mOptions.getFlags();
    }

    @Override
    @NonNull
    public ReformOptions setFlags(@ReformFlags int flags) {
        mOptions.setFlags(flags);
        return this;
    }

    @Override
    public @NonNull Vec3 getCurrentSize() {
        return NodeTypeConverter.toLibrary(mOptions.getCurrentSize());
    }

    @Override
    @NonNull
    public ReformOptions setCurrentSize(@NonNull Vec3 currentSize) {
        mOptions.setCurrentSize(NodeTypeConverter.toFramework(currentSize));
        return this;
    }

    @Override
    public @NonNull Vec3 getMinimumSize() {
        return NodeTypeConverter.toLibrary(mOptions.getMinimumSize());
    }

    @Override
    @NonNull
    public ReformOptions setMinimumSize(@NonNull Vec3 minimumSize) {
        mOptions.setMinimumSize(NodeTypeConverter.toFramework(minimumSize));
        return this;
    }

    @Override
    public @NonNull Vec3 getMaximumSize() {
        return NodeTypeConverter.toLibrary(mOptions.getMaximumSize());
    }

    @Override
    @NonNull
    public ReformOptions setMaximumSize(@NonNull Vec3 maximumSize) {
        mOptions.setMaximumSize(NodeTypeConverter.toFramework(maximumSize));
        return this;
    }

    @Override
    public float getFixedAspectRatio() {
        return mOptions.getFixedAspectRatio();
    }

    @Override
    @NonNull
    public ReformOptions setFixedAspectRatio(float fixedAspectRatio) {
        mOptions.setFixedAspectRatio(fixedAspectRatio);
        return this;
    }

    @Override
    public boolean getForceShowResizeOverlay() {
        return mOptions.getForceShowResizeOverlay();
    }

    @Override
    @NonNull
    public ReformOptions setForceShowResizeOverlay(boolean forceShowResizeOverlay) {
        mOptions.setForceShowResizeOverlay(forceShowResizeOverlay);
        return this;
    }

    @Override
    public @NonNull Consumer<ReformEvent> getEventCallback() {
        com.android.extensions.xr.function.Consumer<com.android.extensions.xr.node.ReformEvent>
                callback = mOptions.getEventCallback();

        return (event) -> {
            callback.accept(NodeTypeConverter.toFramework(event));
        };
    }

    @Override
    @NonNull
    public ReformOptions setEventCallback(@NonNull Consumer<ReformEvent> callback) {
        com.android.extensions.xr.function.Consumer<com.android.extensions.xr.node.ReformEvent>
                platformConsumer =
                        new com.android.extensions.xr.function.Consumer<
                                com.android.extensions.xr.node.ReformEvent>() {
                            @Override
                            public void accept(com.android.extensions.xr.node.ReformEvent event) {
                                callback.accept(NodeTypeConverter.toLibrary(event));
                            }
                        };

        mOptions.setEventCallback(platformConsumer);
        return this;
    }

    @Override
    public @NonNull Executor getEventExecutor() {
        return mOptions.getEventExecutor();
    }

    @Override
    @NonNull
    public ReformOptions setEventExecutor(@NonNull Executor executor) {
        mOptions.setEventExecutor(executor);
        return this;
    }

    @Override
    public @ScaleWithDistanceMode int getScaleWithDistanceMode() {
        return mOptions.getScaleWithDistanceMode();
    }

    @Override
    @NonNull
    public ReformOptions setScaleWithDistanceMode(
            @ScaleWithDistanceMode int scaleWithDistanceMode) {
        mOptions.setScaleWithDistanceMode(scaleWithDistanceMode);
        return this;
    }
}
