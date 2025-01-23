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

package androidx.xr.scenecore.impl;

import android.util.Log;

import androidx.xr.runtime.math.Pose;
import androidx.xr.scenecore.JxrPlatformAdapter.ActivityPose;
import androidx.xr.scenecore.JxrPlatformAdapter.Dimensions;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.LoggingEntity;
import androidx.xr.scenecore.common.BaseEntity;

import java.util.List;
import java.util.concurrent.Executor;

/** Implementation of a RealityCore Entity that logs its function calls. */
class LoggingEntityImpl extends BaseEntity implements LoggingEntity {

    private static final String TAG = "RealityCoreRuntime";

    LoggingEntityImpl() {
        Log.i(TAG, "Creating LoggingEntity.");
    }

    @Override
    public Pose getPose() {
        Log.i(TAG, "Getting Logging Entity pose: " + super.getPose());
        return super.getPose();
    }

    @Override
    public void setPose(Pose pose) {
        Log.i(TAG, "Setting Logging Entity pose to: " + pose);
        super.setPose(pose);
    }

    @Override
    public Pose getActivitySpacePose() {
        Log.i(TAG, "Getting Logging Entity activitySpacePose.");
        return new Pose();
    }

    @Override
    public Pose transformPoseTo(Pose pose, ActivityPose destination) {
        Log.i(
                TAG,
                "Transforming pose "
                        + pose
                        + " to be relative to the destination ActivityPose: "
                        + destination);
        return new Pose();
    }

    @Override
    public void addChild(Entity child) {
        Log.i(TAG, "Adding child Entity: " + child);
        super.addChild(child);
    }

    @Override
    public void addChildren(List<Entity> children) {
        Log.i(TAG, "Adding child Entities: " + children);
        super.addChildren(children);
    }

    @Override
    public Entity getParent() {
        Log.i(TAG, "Getting Logging Entity parent: " + super.getParent());
        return super.getParent();
    }

    @Override
    public void setParent(Entity parent) {
        if (!(parent instanceof LoggingEntityImpl)) {
            Log.e(TAG, "Parent of a LoggingEntity must be a Logging entity");
            return;
        }
        Log.i(TAG, "Setting Logging Entity parent to: " + parent);
        super.setParent(parent);
    }

    @Override
    public List<Entity> getChildren() {
        Log.i(TAG, "Getting Logging Entity children: " + super.getChildren());
        return super.getChildren();
    }

    @Override
    public void setSize(Dimensions dimensions) {
        Log.i(TAG, "Set size to " + dimensions);
    }

    @Override
    public void addInputEventListener(Executor executor, InputEventListener consumer) {
        Log.i(TAG, "Add input consumer " + consumer + " executor " + executor);
    }

    @Override
    public void removeInputEventListener(InputEventListener consumer) {
        Log.i(TAG, "Remove input consumer " + consumer);
    }

    @Override
    public void dispose() {
        Log.i(TAG, "dispose");
    }
}
