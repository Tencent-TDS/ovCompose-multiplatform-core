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

package androidx.xr.extensions.splitengine;

import androidx.annotation.RestrictTo;

/** Wrapper object around a native SplitEngineBridge. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class SplitEngineBridge {
    SplitEngineBridge() {}
    ;

    /**
     * Opaque token to split engine bridge
     *
     * <p>This will be read/set by native code inside Impress via JNI.
     *
     * <p>JNI does not respect access modifies so this private field is publicly accessible to the
     * native impress code.
     *
     * <p>This field is read from and written to by: *
     * vendor/google/imp/core/split_engine/android/view/split_engine_jni.cc *
     * google3/third_party/impress/core/split_engine/android/view/split_engine_jni.cc *
     * frameworks/base/libs/xr/Jetpack/jni/android_xr_splitengine_extension.cpp
     *
     * <p>The latter accesses this field through the implementation of the SplitEngineBridge
     * interface.
     */
    @SuppressWarnings("unused")
    private long mNativeHandle;
}
