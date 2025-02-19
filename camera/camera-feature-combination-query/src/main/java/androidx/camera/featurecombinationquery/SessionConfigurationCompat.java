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

package androidx.camera.featurecombinationquery;

import android.hardware.camera2.params.OutputConfiguration;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

/**
 * A compat class to store the same minimal information as Camera2's
 * {@link android.hardware.camera2.params.SessionConfiguration}.
 * <p>
 * This class can be used with {@link CameraDeviceSetupCompat} to query combination support for
 * camera devices that do not support Camera2
 * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup}, and therefore cannot create
 * session parameters without calling {@link android.hardware.camera2.CameraManager#openCamera}.
 * <p>
 * This class <i>must not</i> be used for camera devices that support
 * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup}. Use
 * {@link android.hardware.camera2.CameraDevice.CameraDeviceSetup#createCaptureRequest} and
 * {@link android.hardware.camera2.params.SessionConfiguration} for those devices instead.
 */
public class SessionConfigurationCompat {
    private final List<OutputConfiguration> mOutputConfigs;
    private final SessionParametersCompat mSessionParams;

    private SessionConfigurationCompat(List<OutputConfiguration> outputConfigs,
            SessionParametersCompat sessionParams) {
        mOutputConfigs = outputConfigs;
        mSessionParams = sessionParams;
    }

    /**
     * @return an instance of the {@link Builder} class.
     */
    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @return the configured output configurations.
     */
    @NonNull
    public List<OutputConfiguration> getOutputConfigurations() {
        return mOutputConfigs;
    }

    /**
     * @return the associated session parameters.
     */
    @NonNull
    public SessionParametersCompat getSessionParameters() {
        return mSessionParams;
    }

    /**
     * Simple builder class for {@link SessionConfigurationCompat}. Use
     * {@link SessionConfigurationCompat#builder()} to obtain a builder object.
     */
    public static final class Builder {
        @NonNull
        private final ArrayList<OutputConfiguration> mOutputConfigs = new ArrayList<>();
        @NonNull
        private SessionParametersCompat mSessionParams = SessionParametersCompat.builder().build();

        private Builder() {
        }

        /**
         * Add an {@link OutputConfiguration} to the session configuration.
         *
         * @param outputConfig {@link OutputConfiguration} to add to the session configuration.
         * @return the current builder
         */
        @NonNull
        public Builder addOutputConfiguration(@NonNull OutputConfiguration outputConfig) {
            mOutputConfigs.add(outputConfig);
            return this;
        }

        /**
         * Sets the session parameters for the session configuration. Overwrites any previously set
         * session parameters.
         *
         * @param sessionParams session parameters to be associated with the session configuration
         * @return the current builder
         */
        @NonNull
        public Builder setSessionParameters(@NonNull SessionParametersCompat sessionParams) {
            mSessionParams = sessionParams;
            return this;
        }

        /**
         * Builds a {@link SessionConfigurationCompat}.
         * <p>
         * Note that the created {@link SessionConfigurationCompat} makes a shallow copy of the
         * {@link OutputConfiguration}s added via {@link #addOutputConfiguration}, and so
         * any mutations to the added {@link OutputConfiguration} objects will be reflected in
         * the created {@link SessionConfigurationCompat}.
         * <p>
         * This is quirk of implementation, and it is generally recommended to treat
         * {@link OutputConfiguration} objects as immutable once they have been added to a
         * {@link SessionConfigurationCompat.Builder}.
         *
         * @return a new {@link SessionConfigurationCompat} object.
         */
        @NonNull
        public SessionConfigurationCompat build() {
            return new SessionConfigurationCompat(List.copyOf(mOutputConfigs), mSessionParams);
        }

    }
}
