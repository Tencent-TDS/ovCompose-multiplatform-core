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

package androidx.xr.extensions.environment;

import androidx.annotation.NonNull;

class PassthroughVisibilityStateImpl implements PassthroughVisibilityState {
    @NonNull final com.android.extensions.xr.environment.PassthroughVisibilityState mState;

    PassthroughVisibilityStateImpl(
            @NonNull com.android.extensions.xr.environment.PassthroughVisibilityState state) {
        mState = state;
    }

    @Override
    public @PassthroughVisibilityState.State int getCurrentState() {
        return mState.getCurrentState();
    }

    @Override
    public float getOpacity() {
        return mState.getOpacity();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || !(other instanceof PassthroughVisibilityStateImpl)) {
            return false;
        }
        PassthroughVisibilityStateImpl impl = (PassthroughVisibilityStateImpl) other;
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
