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

package com.android.extensions.xr;


/** XR configuration information. */
@SuppressWarnings({"unchecked", "deprecation", "all"})
public class Config {

    Config() {
        throw new RuntimeException("Stub!");
    }

    /**
     * Returns the default pixelsPerMeter value for 2D surfaces. See
     * NodeTransaction.setPixelResolution() for the meaning of pixelsPerMeter.
     *
     * @param density The logical density of the display.
     * @return The default pixelsPerMeter value.
     */
    public float defaultPixelsPerMeter(float density) {
        throw new RuntimeException("Stub!");
    }
}
