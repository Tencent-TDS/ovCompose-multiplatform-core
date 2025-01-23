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

package androidx.savedstate

import androidx.kruth.assertThat
import androidx.savedstate.serialization.decodeFromSavedState
import androidx.savedstate.serialization.encodeToSavedState
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

internal object SavedStateCodecTestUtils {
    /* Test the following steps: 1. encode `T` to a `SavedState`, 2. parcelize it to a `Parcel`,
     * 3. un-parcelize it back to a `SavedState`, and 4. decode it back to a `T`. Step 2 and 3
     * are only performed on Android. Here's the whole process:
     *
     * (A)Serializable -1-> (B)SavedState -2-> (C)Parcel -3-> (D)SavedState -4-> (E)Serializable
     *
     * `checkEncoded` can be used to check the content of "B", and `checkDecoded` can be
     *  used to compare the instances of "E" and "A".
     */
    inline fun <reified T : Any> T.encodeDecode(
        serializer: KSerializer<T> = serializer<T>(),
        checkDecoded: (T, T) -> Unit = { decoded, original ->
            assertThat(decoded).isEqualTo(original)
        },
        checkEncoded: SavedStateReader.() -> Unit = { assertThat(size()).isEqualTo(0) }
    ) {
        val encoded = encodeToSavedState(serializer, this)
        encoded.read { checkEncoded() }

        val restored = platformEncodeDecode(encoded)

        val decoded = decodeFromSavedState(serializer, restored)
        checkDecoded(decoded, this)
    }
}

expect fun platformEncodeDecode(savedState: SavedState): SavedState
