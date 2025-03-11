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

package androidx.xr.runtime.internal

import android.media.AudioTrack
import androidx.annotation.RestrictTo

/** Interface for a XR Runtime AudioTrackExtensionsWrapper */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public interface AudioTrackExtensionsWrapper {
    /**
     * Returns the PointSourceAttributes of the AudioTrack.
     *
     * @param track The AudioTrack to get the PointSourceAttributes from.
     * @return The PointSourceAttributes of the AudioTrack.
     */
    public fun getPointSourceAttributes(track: AudioTrack): PointSourceAttributes?

    /**
     * Returns the SoundFieldAttributes of the AudioTrack.
     *
     * @param track The AudioTrack to get the SoundFieldAttributes from.
     * @return The SoundFieldAttributes of the AudioTrack.
     */
    public fun getSoundFieldAttributes(track: AudioTrack): SoundFieldAttributes?

    /**
     * Returns the spatial source type of the AudioTrack.
     *
     * @param track The AudioTrack to get the spatial source type from.
     * @return The spatial source type of the AudioTrack.
     */
    @SpatializerConstants.SourceType public fun getSpatialSourceType(track: AudioTrack): Int

    /**
     * Sets the PointSourceAttributes of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the PointSourceAttributes on.
     * @param attributes The PointSourceAttributes to set.
     * @return The AudioTrack.Builder with the PointSourceAttributes set.
     */
    public fun setPointSourceAttributes(
        builder: AudioTrack.Builder,
        attributes: PointSourceAttributes,
    )

    /**
     * Sets the SoundFieldAttributes of the AudioTrack.
     *
     * @param builder The AudioTrack.Builder to set the SoundFieldAttributes on.
     * @param attributes The SoundFieldAttributes to set.
     * @return The AudioTrack.Builder with the SoundFieldAttributes set.
     */
    public fun setSoundFieldAttributes(
        builder: AudioTrack.Builder,
        attributes: SoundFieldAttributes
    )
}
