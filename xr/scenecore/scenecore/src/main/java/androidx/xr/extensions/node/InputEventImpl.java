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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

class InputEventImpl implements InputEvent {
    @NonNull private final com.android.extensions.xr.node.InputEvent mEvent;

    InputEventImpl(@NonNull com.android.extensions.xr.node.InputEvent event) {
        mEvent = event;
    }

    @Override
    public int getSource() {
        return mEvent.getSource();
    }

    @Override
    public int getPointerType() {
        return mEvent.getPointerType();
    }

    @Override
    public long getTimestamp() {
        return mEvent.getTimestamp();
    }

    @Override
    @NonNull
    public Vec3 getOrigin() {
        com.android.extensions.xr.node.Vec3 origin = mEvent.getOrigin();
        return new Vec3(origin.x, origin.y, origin.z);
    }

    @Override
    @NonNull
    public Vec3 getDirection() {
        com.android.extensions.xr.node.Vec3 direction = mEvent.getDirection();
        return new Vec3(direction.x, direction.y, direction.z);
    }

    @Override
    @Nullable
    public HitInfo getHitInfo() {
        com.android.extensions.xr.node.InputEvent.HitInfo info = mEvent.getHitInfo();
        return (info == null) ? null : new HitInfo(info);
    }

    @Override
    @Nullable
    public HitInfo getSecondaryHitInfo() {
        com.android.extensions.xr.node.InputEvent.HitInfo info = mEvent.getSecondaryHitInfo();
        return (info == null) ? null : new HitInfo(info);
    }

    @Override
    public int getDispatchFlags() {
        return mEvent.getDispatchFlags();
    }

    @Override
    public int getAction() {
        return mEvent.getAction();
    }

    static class HitInfo implements InputEvent.HitInfo {
        @NonNull private final com.android.extensions.xr.node.InputEvent.HitInfo info;

        HitInfo(@NonNull com.android.extensions.xr.node.InputEvent.HitInfo info) {
            this.info = info;
        }

        @Override
        public int getSubspaceImpressNodeId() {
            return info.getSubspaceImpressNodeId();
        }

        @Override
        @NonNull
        public Node getInputNode() {
            return NodeTypeConverter.toLibrary(info.getInputNode());
        }

        @Override
        @Nullable
        public Vec3 getHitPosition() {
            com.android.extensions.xr.node.Vec3 position = info.getHitPosition();
            return (position == null) ? null : new Vec3(position.x, position.y, position.z);
        }

        @Override
        @NonNull
        public Mat4f getTransform() {
            float[] transform = info.getTransform().getFlattenedMatrix();
            return new Mat4f(transform);
        }
    }
}
