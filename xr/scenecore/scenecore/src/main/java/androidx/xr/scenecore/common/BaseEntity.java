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

package androidx.xr.scenecore.common;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.xr.runtime.math.Pose;
import androidx.xr.runtime.math.Vector3;
import androidx.xr.scenecore.JxrPlatformAdapter.Component;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;

import java.util.ArrayList;
import java.util.List;

/** Implementation of a subset of core RealityCore Entity functionality. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class BaseEntity extends BaseActivityPose implements Entity {
    private final List<Entity> mChildren = new ArrayList<>();
    private final List<Component> mComponentList = new ArrayList<>();
    private BaseEntity mParent;
    private Pose mPose = new Pose();
    private Vector3 mScale = new Vector3(1.0f, 1.0f, 1.0f);
    private float mAlpha = 1.0f;
    private boolean mHidden = false;

    protected void addChildInternal(@NonNull Entity child) {
        if (mChildren.contains(child)) {
            Log.w("RealityCoreRuntime", "Trying to add child who is already a child.");
        }
        mChildren.add(child);
    }

    protected void removeChildInternal(@NonNull Entity child) {
        if (!mChildren.contains(child)) {
            Log.w("RealityCoreRuntime", "Trying to remove child who is not a child.");
            return;
        }
        mChildren.remove(child);
    }

    @Override
    public void addChild(@NonNull Entity child) {
        child.setParent(this);
    }

    @Override
    public void addChildren(@NonNull List<Entity> mChildren) {
        for (Entity child : mChildren) {
            child.setParent(this);
        }
    }

    @Override
    @Nullable
    public Entity getParent() {
        return mParent;
    }

    @Override
    public void setParent(@Nullable Entity parent) {
        if ((parent != null) && !(parent instanceof BaseEntity)) {
            Log.e("RealityCoreRuntime", "Cannot set non-BaseEntity as a parent of a BaseEntity");
            return;
        }
        if (this.mParent != null) {
            this.mParent.removeChildInternal(this);
        }
        this.mParent = (BaseEntity) parent;
        if (this.mParent != null) {
            this.mParent.addChildInternal(this);
        }
    }

    @Override
    @NonNull
    public List<Entity> getChildren() {
        return mChildren;
    }

    @Override
    public void setContentDescription(@NonNull String text) {
        // TODO(b/320202321): Finish A11y Text integration.
        Log.i("BaseEntity", "setContentDescription: " + text);
    }

    @Override
    @NonNull
    public Pose getPose() {
        return mPose;
    }

    @Override
    public void setPose(@NonNull Pose pose) {
        this.mPose = pose;
    }

    @Override
    @NonNull
    public Pose getActivitySpacePose() {
        // Any parentless "space" entities (such as the root and anchor entities) are expected to
        // override this method non-recursively so that this error is never thrown.
        if (mParent == null) {
            throw new IllegalStateException("Cannot get pose in ActivitySpace with a null parent");
        }

        return mParent.getActivitySpacePose()
                .compose(
                        new Pose(
                                this.mPose.getTranslation().times(mParent.getWorldSpaceScale()),
                                this.mPose.getRotation()));
    }

    @Override
    @NonNull
    public Vector3 getScale() {
        return mScale;
    }

    @Override
    public void setScale(@NonNull Vector3 scale) {
        this.mScale = scale;
    }

    // Purely sets the value of the scale.
    protected final void setScaleInternal(@NonNull Vector3 scale) {
        this.mScale = scale;
    }

    @Override
    public float getAlpha() {
        return mAlpha;
    }

    @Override
    public void setAlpha(float alpha) {
        this.mAlpha = alpha;
    }

    @Override
    public float getActivitySpaceAlpha() {
        if (mParent == null) {
            return mAlpha;
        }
        return mParent.getActivitySpaceAlpha() * mAlpha;
    }

    @Override
    @NonNull
    public Vector3 getWorldSpaceScale() {
        if (mParent == null) {
            throw new IllegalStateException("Cannot get scale in WorldSpace with a null parent");
        }
        return mParent.getWorldSpaceScale().times(this.mScale);
    }

    @Override
    @NonNull
    public Vector3 getActivitySpaceScale() {
        if (mParent == null) {
            throw new IllegalStateException("Cannot get scale in ActivitySpace with a null parent");
        }
        return mParent.getActivitySpaceScale().times(this.mScale);
    }

    @Override
    public boolean isHidden(boolean includeParents) {
        if (!includeParents || mParent == null) {
            return mHidden;
        }
        return mHidden || mParent.isHidden(true);
    }

    @Override
    public void setHidden(boolean hidden) {
        this.mHidden = hidden;
    }

    @Override
    public void dispose() {
        // Create a copy to avoid concurrent modification issues since the children detach
        // themselves
        // from their parents as they are disposed.
        List<Entity> childrenToDispose = new ArrayList<>(mChildren);
        for (Entity child : childrenToDispose) {
            child.dispose();
        }
    }

    @Override
    public boolean addComponent(@NonNull Component component) {
        if (component.onAttach(this)) {
            mComponentList.add(component);
            return true;
        }
        return false;
    }

    @Override
    public void removeComponent(@NonNull Component component) {
        if (mComponentList.contains(component)) {
            component.onDetach(this);
            mComponentList.remove(component);
        }
    }

    @Override
    public void removeAllComponents() {
        for (Component component : mComponentList) {
            component.onDetach(this);
        }
        mComponentList.clear();
    }
}
