/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.savedstate.serialization

import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceListSerializer
import androidx.savedstate.serialization.serializers.CharSequenceSerializer
import androidx.savedstate.serialization.serializers.DefaultJavaSerializableSerializer
import androidx.savedstate.serialization.serializers.DefaultParcelableSerializer
import androidx.savedstate.serialization.serializers.IBinderSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableListSerializer
import kotlinx.serialization.DeserializationStrategy

@Suppress("UNCHECKED_CAST", "IMPLICIT_CAST_TO_ANY")
internal actual fun <T> SavedStateDecoder.decodeFormatSpecificTypesOnPlatform(
    strategy: DeserializationStrategy<T>
): T? {
    return when (strategy.descriptor) {
        polymorphicCharSequenceDescriptor -> CharSequenceSerializer.deserialize(this)
        polymorphicParcelableDescriptor -> DefaultParcelableSerializer.deserialize(this)
        polymorphicJavaSerializableDescriptor -> DefaultJavaSerializableSerializer.deserialize(this)
        polymorphicIBinderDescriptor -> IBinderSerializer.deserialize(this)
        parcelableArrayDescriptor -> ParcelableArraySerializer.deserialize(this)
        parcelableListDescriptor -> ParcelableListSerializer.deserialize(this)
        charSequenceArrayDescriptor -> CharSequenceArraySerializer.deserialize(this)
        charSequenceListDescriptor -> CharSequenceListSerializer.deserialize(this)
        else -> null
    }
        as T?
}
