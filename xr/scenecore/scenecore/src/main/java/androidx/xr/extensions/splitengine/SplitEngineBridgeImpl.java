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

import android.util.Log;

import androidx.annotation.NonNull;

import java.lang.reflect.Field;

/** Implementation of SplitEngineBridge which delegates to native Impress implementation. */
class SplitEngineBridgeImpl extends SplitEngineBridge {
    final com.android.extensions.xr.splitengine.SplitEngineBridge mBridge;

    SplitEngineBridgeImpl(@NonNull com.android.extensions.xr.splitengine.SplitEngineBridge bridge) {
        mBridge = bridge;
        setNativeHandle();
    }

    /**
     * Set the shared memory split engine bridge handle.
     *
     * <p>The {@link mNativeHandle} field is defined as a private field on the extensions library
     * which we would like to not change for backward compatibility. This field is read by
     * application code and needs to contain the handle provided by the platform JNI component which
     * is accessible from {@link com.android.extensions.xr.splitengine.SplitEngineBridge}. In order
     * to be able to access this private field we need to use reflection.
     *
     * <p>This method will access the handle from the platform instance of SplitEngineBridge and
     * store it in the private {@link mNativeHandle} field defined by the library API.
     */
    private void setNativeHandle() {
        try {
            Field privateField = SplitEngineBridge.class.getDeclaredField("mNativeHandle");
            privateField.setAccessible(true);

            privateField.set(this, mBridge.getNativeHandle());
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e("SplitEngineBridge", "Failed to set native handle", e);
        }
    }
}
