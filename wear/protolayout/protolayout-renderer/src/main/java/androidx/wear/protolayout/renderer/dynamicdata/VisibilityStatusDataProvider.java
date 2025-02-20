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
package androidx.wear.protolayout.renderer.dynamicdata;

import android.util.Log;

import androidx.wear.protolayout.expression.DynamicDataBuilders;
import androidx.wear.protolayout.expression.PlatformDataValues;
import androidx.wear.protolayout.expression.PlatformEventKeys;
import androidx.wear.protolayout.expression.pipeline.PlatformDataProvider;
import androidx.wear.protolayout.expression.pipeline.PlatformDataReceiver;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.concurrent.Executor;

/**
 * Provider used to notify and update all VisibilityStatus consumers with the new platform event
 * value of layout visibility status.
 */
@SuppressWarnings("LogTagLength") // To have clear tag description
class VisibilityStatusDataProvider implements PlatformDataProvider {
    private static final String TAG = "ProtoLayoutVisibilityStatus";

    private boolean mUpdatesEnabled = true;
    private @Nullable PlatformDataReceiver mReceiver;
    private @Nullable Executor mExecutor;
    private boolean mCurrentlyVisible;

    VisibilityStatusDataProvider(boolean initialVisibility) {
        mCurrentlyVisible = initialVisibility;
    }

    /** Registers the given receiver for receiving the visibility updates. */
    private void registerReceiver(
            @NonNull Executor executor, @NonNull PlatformDataReceiver receiver) {
        if (mReceiver != null) {
            Log.w(TAG, "Clearing previously set receiver for visibility status.");
            clearReceiver();
        }

        mReceiver = receiver;
        mExecutor = executor;
    }

    @Override
    public void setReceiver(@NonNull Executor executor, @NonNull PlatformDataReceiver receiver) {
        registerReceiver(executor, receiver);
        // Send the first value to the receiver, so that initial layout has the correct state.
        onVisibilityChanged(mCurrentlyVisible);
    }

    /** Clears the registered receiver. */
    @Override
    public void clearReceiver() {
        mReceiver = null;
        mExecutor = null;
    }

    /** Callback whenever the visibility status changes. */
    @SuppressWarnings("ExecutorTaskName")
    void onVisibilityChanged(boolean isVisible) {
        mCurrentlyVisible = isVisible;

        if (!mUpdatesEnabled) {
            return;
        }

        if (mExecutor == null) {
            return;
        }

        mExecutor.execute(
                () -> {
                    if (mReceiver == null) {
                        return;
                    }
                    mReceiver.onData(
                            PlatformDataValues.of(
                                    PlatformEventKeys.VISIBILITY_STATUS,
                                    DynamicDataBuilders.DynamicDataValue.fromBool(isVisible)));
                });
    }

    /** Sets whether this consumer can send updates on the registered receiver. */
    void setUpdatesEnabled(boolean updatesEnabled) {
        mUpdatesEnabled = updatesEnabled;
    }
}
