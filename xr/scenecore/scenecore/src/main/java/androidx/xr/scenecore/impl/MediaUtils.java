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

import androidx.annotation.RestrictTo;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatializerConstants;

import com.android.extensions.xr.media.PointSourceAttributes;
import com.android.extensions.xr.media.SoundFieldAttributes;
import com.android.extensions.xr.media.SpatializerExtensions;
import com.android.extensions.xr.node.Node;

/** Utils for the runtime media class conversions. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class MediaUtils {
    private MediaUtils() {}

    static PointSourceAttributes convertPointSourceAttributesToExtensions(
            JxrPlatformAdapter.PointSourceAttributes attributes) {

        Node node = ((AndroidXrEntity) attributes.getEntity()).getNode();

        return new PointSourceAttributes.Builder().setNode(node).build();
    }

    static SoundFieldAttributes convertSoundFieldAttributesToExtensions(
            JxrPlatformAdapter.SoundFieldAttributes attributes) {

        return new SoundFieldAttributes.Builder()
                .setAmbisonicsOrder(
                        convertAmbisonicsOrderToExtensions(attributes.getAmbisonicsOrder()))
                .build();
    }

    static int convertAmbisonicsOrderToExtensions(
            @SpatializerConstants.AmbisonicsOrder int ambisonicsOrder) {
        switch (ambisonicsOrder) {
            case SpatializerConstants.AMBISONICS_ORDER_FIRST_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_FIRST_ORDER;
            case SpatializerConstants.AMBISONICS_ORDER_SECOND_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_SECOND_ORDER;
            case SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER:
                return SpatializerExtensions.AMBISONICS_ORDER_THIRD_ORDER;
            default:
                throw new IllegalArgumentException(
                        "Invalid Sound Field ambisonics order: " + ambisonicsOrder);
        }
    }

    @SpatializerConstants.SourceType
    static int convertExtensionsToSourceType(int extensionsSourceType) {
        switch (extensionsSourceType) {
            case SpatializerExtensions.SOURCE_TYPE_BYPASS:
                return SpatializerConstants.SOURCE_TYPE_BYPASS;
            case SpatializerExtensions.SOURCE_TYPE_POINT_SOURCE:
                return SpatializerConstants.SOURCE_TYPE_POINT_SOURCE;
            case SpatializerExtensions.SOURCE_TYPE_SOUND_FIELD:
                return SpatializerConstants.SOURCE_TYPE_SOUND_FIELD;
            default:
                throw new IllegalArgumentException(
                        "Invalid Sound Spatializer source type: " + extensionsSourceType);
        }
    }
}
