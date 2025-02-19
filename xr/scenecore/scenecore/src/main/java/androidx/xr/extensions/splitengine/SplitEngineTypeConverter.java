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

import static java.util.Objects.requireNonNull;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

/** This class is able to convert library types into platform types. */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class SplitEngineTypeConverter {

    private SplitEngineTypeConverter() {}

    /**
     * Converts a {@link com.android.extensions.xr.splitengine.SplitEngineBridge} to a library type.
     *
     * @param bridge The {@link com.android.extensions.xr.splitengine.SplitEngineBridge} to convert.
     * @return The library type of the {@link SplitEngineBridge}.
     */
    @NonNull
    public static SplitEngineBridge toLibrary(
            @NonNull com.android.extensions.xr.splitengine.SplitEngineBridge bridge) {
        requireNonNull(bridge);

        return new SplitEngineBridgeImpl(bridge);
    }

    /**
     * Converts a {@link SplitEngineBridge} to a framework type.
     *
     * @param bridge The {@link SplitEngineBridge} to convert.
     * @return The framework type of the {@link
     *     com.android.extensions.xr.splitengine.SplitEngineBridge}.
     */
    @NonNull
    public static com.android.extensions.xr.splitengine.SplitEngineBridge toFramework(
            @NonNull SplitEngineBridge bridge) {
        requireNonNull(bridge);

        return ((SplitEngineBridgeImpl) bridge).mBridge;
    }
}
