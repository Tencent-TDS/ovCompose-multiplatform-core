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

package androidx.xr.scenecore

import androidx.annotation.RestrictTo
import androidx.xr.scenecore.JxrPlatformAdapter.ExrImageResource as RtExrImage

/** Interface for image formats in SceneCore. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX) public interface Image

/**
 * ExrImage represents an EXR Image resource in SceneCore. EXR images are used by the [Environment]
 * for drawing skyboxes.
 */
// TODO(b/319269278): Make this and GltfModel derive from a common Resource base class which has
//                    async helpers.
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class ExrImage internal constructor(public val image: RtExrImage) : Image {

    internal companion object {
        internal fun create(runtime: JxrPlatformAdapter, name: String): ExrImage {
            val exrImageFuture = runtime.loadExrImageByAssetName(name)
            // TODO: b/323022003 - Implement async loading of [ExrImage].
            return ExrImage(exrImageFuture!!.get())
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ExrImage

        // Perform a structural equality check on the underlying image.
        if (image != other.image) return false

        return true
    }

    override fun hashCode(): Int {
        return image.hashCode()
    }
}
