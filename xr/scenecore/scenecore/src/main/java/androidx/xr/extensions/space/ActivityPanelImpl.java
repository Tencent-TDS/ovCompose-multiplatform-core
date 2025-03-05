/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.xr.extensions.space;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.xr.extensions.node.Node;
import androidx.xr.extensions.node.NodeTypeConverter;

/** Implementation of {@link ActivityPanel}. */
final class ActivityPanelImpl implements ActivityPanel {
    @NonNull final com.android.extensions.xr.space.ActivityPanel mActivityPanel;

    ActivityPanelImpl(@NonNull com.android.extensions.xr.space.ActivityPanel panel) {
        mActivityPanel = panel;
    }

    @Override
    public void launchActivity(@NonNull Intent intent, @Nullable Bundle options) {
        mActivityPanel.launchActivity(intent, options);
    }

    @Override
    public void moveActivity(@NonNull Activity activity) {
        mActivityPanel.moveActivity(activity);
    }

    @Override
    @NonNull
    public Node getNode() {
        return NodeTypeConverter.toLibrary(mActivityPanel.getNode());
    }

    @Override
    public void setWindowBounds(@NonNull Rect windowBounds) {
        mActivityPanel.setWindowBounds(windowBounds);
    }

    @Override
    public void delete() {
        mActivityPanel.delete();
    }
}
