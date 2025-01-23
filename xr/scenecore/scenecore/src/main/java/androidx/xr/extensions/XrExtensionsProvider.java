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

package androidx.xr.extensions;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** Provides the OEM implementation of {@link XrExtensions}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class XrExtensionsProvider {
    private XrExtensionsProvider() {}

    /**
     * Returns the OEM implementation of {@link XrExtensions}.
     *
     * @return The OEM implementation of {@link XrExtensions} or throws an exception if no
     *     implementation is found.
     */
    @Nullable
    public static XrExtensions getXrExtensions() {
        try {
            return XrExtensionsInstance.getInstance();
        } catch (NoClassDefFoundError e) {
            return null;
        }
    }

    private static class XrExtensionsInstance {
        private XrExtensionsInstance() {}

        private static class XrExtensionsHolder {
            public static final XrExtensions INSTANCE =
                    new AndroidXrExtensions(new com.android.extensions.xr.XrExtensions());
        }

        public static XrExtensions getInstance() {
            return XrExtensionsHolder.INSTANCE;
        }
    }
}
