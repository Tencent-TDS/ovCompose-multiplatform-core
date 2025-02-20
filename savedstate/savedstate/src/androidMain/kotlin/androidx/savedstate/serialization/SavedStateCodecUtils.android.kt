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

package androidx.savedstate.serialization

import android.os.IBinder
import android.os.Parcelable
import java.io.Serializable as JavaSerializable
import kotlinx.serialization.PolymorphicSerializer
import kotlinx.serialization.serializer

internal val parcelableArrayDescriptor = serializer<Array<Parcelable>>().descriptor
internal val parcelableListDescriptor = serializer<List<Parcelable>>().descriptor
internal val charSequenceArrayDescriptor = serializer<Array<CharSequence>>().descriptor
internal val charSequenceListDescriptor = serializer<List<CharSequence>>().descriptor
internal val polymorphicCharSequenceDescriptor =
    PolymorphicSerializer(CharSequence::class).descriptor
internal val polymorphicParcelableDescriptor = PolymorphicSerializer(Parcelable::class).descriptor
internal val polymorphicJavaSerializableDescriptor =
    PolymorphicSerializer(JavaSerializable::class).descriptor
internal val polymorphicIBinderDescriptor = PolymorphicSerializer(IBinder::class).descriptor
