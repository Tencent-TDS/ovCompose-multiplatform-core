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

package com.android.extensions.xr.asset;

import androidx.annotation.RestrictTo;

/**
 * Animation configuration to be played on a glTF model.
 *
 * @deprecated SceneCore doesn't need this anymore as it does the same with Split Engine.
 */
@SuppressWarnings({"unchecked", "deprecation", "all"})
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
@Deprecated
public interface GltfAnimation {
    /**
     * State of a glTF animation.
     *
     * @deprecated No longer needed by SceneCore.
     */
    @SuppressWarnings({"unchecked", "deprecation", "all"})
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
    @Deprecated
    enum State {
        /**
         * Will stop the glTF animation that is currently playing or looping.
         *
         * @deprecated No longer needed by SceneCore.
         */
        @Deprecated
        STOP,
        /**
         * Will restart the glTF animation if it's currently playing, looping, or is stopped.
         *
         * @deprecated No longer needed by SceneCore.
         */
        @Deprecated
        PLAY,
        /**
         * Will restart and loop the glTF animation if it's currently playing, looping, or is
         * stopped.
         *
         * @deprecated No longer needed by SceneCore.
         */
        @Deprecated
        LOOP;
    }
}
