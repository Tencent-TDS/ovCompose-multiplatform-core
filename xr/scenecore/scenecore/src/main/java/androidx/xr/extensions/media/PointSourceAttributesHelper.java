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

package androidx.xr.extensions.media;

import androidx.xr.extensions.node.NodeTypeConverter;

// TODO(bvanderlaan): Replace this with a MediaTypeConverter for consistency with other modules
class PointSourceAttributesHelper {

    private PointSourceAttributesHelper() {}

    static com.android.extensions.xr.media.PointSourceAttributes convertToFramework(
            PointSourceAttributes attributes) {

        return new com.android.extensions.xr.media.PointSourceAttributes.Builder()
                .setNode(NodeTypeConverter.toFramework(attributes.getNode()))
                .build();
    }

    static PointSourceAttributes convertToExtensions(
            com.android.extensions.xr.media.PointSourceAttributes fwkAttributes) {

        return new PointSourceAttributes.Builder()
                .setNode(NodeTypeConverter.toLibrary(fwkAttributes.getNode()))
                .build();
    }
}
