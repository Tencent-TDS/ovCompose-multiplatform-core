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

import android.os.Parcelable
import androidx.savedstate.serialization.serializers.CharSequenceArraySerializer
import androidx.savedstate.serialization.serializers.CharSequenceListSerializer
import androidx.savedstate.serialization.serializers.ParcelableArraySerializer
import androidx.savedstate.serialization.serializers.ParcelableListSerializer
import kotlinx.serialization.SerializationStrategy

@Suppress("UNCHECKED_CAST")
internal actual fun <T> SavedStateEncoder.encodeFormatSpecificTypesOnPlatform(
    strategy: SerializationStrategy<T>,
    value: T
): Boolean {
    when (strategy.descriptor) {
        parcelableArrayDescriptor ->
            ParcelableArraySerializer.serialize(this, value as Array<Parcelable>)
        parcelableListDescriptor ->
            ParcelableListSerializer.serialize(this, value as List<Parcelable>)
        charSequenceArrayDescriptor ->
            CharSequenceArraySerializer.serialize(this, value as Array<CharSequence>)
        charSequenceListDescriptor ->
            CharSequenceListSerializer.serialize(this, value as List<CharSequence>)
        else -> return false
    }
    return true
}
