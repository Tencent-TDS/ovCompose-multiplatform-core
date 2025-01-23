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

package androidx.xr.extensions.node;

import static java.util.Objects.requireNonNull;

import android.os.Parcel;
import android.os.Parcelable;
import android.view.AttachedSurfaceControl;

import androidx.annotation.NonNull;
import androidx.xr.extensions.Consumer;

import java.io.Closeable;
import java.util.concurrent.Executor;

class NodeImpl implements Node {
    @NonNull final com.android.extensions.xr.node.Node mNode;

    NodeImpl(@NonNull com.android.extensions.xr.node.Node node) {
        requireNonNull(node);
        mNode = node;
    }

    @Override
    public void listenForInput(@NonNull Consumer<InputEvent> listener, @NonNull Executor executor) {
        mNode.listenForInput((event) -> listener.accept(new InputEventImpl(event)), executor);
    }

    @Override
    public void stopListeningForInput() {
        mNode.stopListeningForInput();
    }

    @Override
    public void setNonPointerFocusTarget(@NonNull AttachedSurfaceControl focusTarget) {
        mNode.setNonPointerFocusTarget(focusTarget);
    }

    @Override
    public void requestPointerCapture(
            @NonNull Consumer</* @PointerCaptureState */ Integer> stateCallback,
            @NonNull Executor executor) {
        mNode.requestPointerCapture((state) -> stateCallback.accept(state), executor);
    }

    @Override
    public void stopPointerCapture() {
        mNode.stopPointerCapture();
    }

    @Override
    @NonNull
    public Closeable subscribeToTransform(
            @NonNull Consumer<NodeTransform> transformCallback, @NonNull Executor executor) {
        return mNode.subscribeToTransform(
                (transform) -> {
                    transformCallback.accept(new NodeTransformImpl(transform));
                },
                executor);
    }

    @Override
    public int describeContents() {
        return mNode.describeContents();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
        out.writeParcelable(mNode, flags);
    }

    @Override
    public String toString() {
        return mNode.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null) return false;

        if (object instanceof NodeImpl) {
            return this.mNode.equals(((NodeImpl) object).mNode);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return mNode.hashCode();
    }

    public static final Parcelable.Creator<NodeImpl> CREATOR =
            new Parcelable.Creator<NodeImpl>() {
                @Override
                public NodeImpl createFromParcel(Parcel in) {
                    return new NodeImpl(
                            in.readParcelable(
                                    com.android.extensions.xr.node.Node.class.getClassLoader()));
                }

                @Override
                public NodeImpl[] newArray(int size) {
                    return new NodeImpl[size];
                }
            };
}
