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

package androidx.xr.scenecore.impl;

import androidx.xr.scenecore.JxrPlatformAdapter.GltfModelResource;

/**
 * Implementation of a RealityCore GltfModelResource.
 *
 * <p>This is used to create to load a glTF that can later be used when creating a GltfEntity.
 */
@SuppressWarnings({"deprecation", "UnnecessarilyFullyQualified"}) // TODO(b/373435470): Remove
final class GltfModelResourceImpl implements GltfModelResource {
    private final androidx.xr.extensions.asset.GltfModelToken mToken;

    GltfModelResourceImpl(androidx.xr.extensions.asset.GltfModelToken token) {
        mToken = token;
    }

    public androidx.xr.extensions.asset.GltfModelToken getExtensionModelToken() {
        return mToken;
    }
}
