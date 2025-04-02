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

import static com.android.extensions.xr.node.InputEvent.ACTION_MOVE;
import static com.android.extensions.xr.node.InputEvent.DISPATCH_FLAG_NONE;
import static com.android.extensions.xr.node.InputEvent.POINTER_TYPE_DEFAULT;
import static com.android.extensions.xr.node.InputEvent.SOURCE_UNKNOWN;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.util.concurrent.MoreExecutors.directExecutor;

import androidx.annotation.NonNull;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.Entity;
import androidx.xr.scenecore.JxrPlatformAdapter.InputEventListener;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent;
import androidx.xr.scenecore.JxrPlatformAdapter.PointerCaptureComponent.StateListener;
import androidx.xr.scenecore.impl.extensions.XrExtensionsProvider;
import androidx.xr.scenecore.testing.FakeScheduledExecutorService;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeNode;

import com.android.extensions.xr.XrExtensions;
import com.android.extensions.xr.node.InputEvent;
import com.android.extensions.xr.node.Node;
import com.android.extensions.xr.node.ShadowInputEvent;
import com.android.extensions.xr.node.Vec3;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PointerCaptureComponentImplTest {

    // Static private implementation of fakes so that the last received state can be grabbed.
    private static class FakeStateListener implements StateListener {
        public int lastState = -1;

        @Override
        public void onStateChanged(int newState) {
            lastState = newState;
        }
    }

    private static class FakeInputEventListener implements InputEventListener {
        public JxrPlatformAdapter.InputEvent lastEvent = null;

        @Override
        public void onInputEvent(@NonNull JxrPlatformAdapter.InputEvent event) {
            lastEvent = event;
        }
    }

    private final FakeStateListener mStateListener = new FakeStateListener();

    private final FakeInputEventListener mInputListener = new FakeInputEventListener();

    private final XrExtensions mXrExtensions = XrExtensionsProvider.getXrExtensions();
    private final FakeScheduledExecutorService mFakeScheduler = new FakeScheduledExecutorService();
    private final Node mNode = mXrExtensions.createNode();
    private final FakeNode mFakeNode = new FakeNode(mNode);

    private final Entity mEntity =
            new AndroidXrEntity(mNode, mXrExtensions, new EntityManager(), mFakeScheduler) {};

    @Test
    public void onAttach_enablesPointerCapture() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);

        assertThat(component.onAttach(mEntity)).isTrue();

        assertThat(mFakeNode.getPointerCaptureStateCallback()).isNotNull();
    }

    @Test
    public void onAttach_setsUpInputEventPropagation() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        InputEvent fakeInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 0,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);
        mFakeNode.sendInputEvent(fakeInput);
        mFakeScheduler.runAll();

        assertThat(mInputListener.lastEvent).isNotNull();
    }

    // This should really be a test on AndroidXrEntity, but that does not have tests so it is here
    // for
    // the meantime.
    @Test
    public void onAttach_onlyPropagatesCapturedEvents() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        InputEvent fakeCapturedInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 100,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);

        InputEvent fakeInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 200,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        DISPATCH_FLAG_NONE,
                        ACTION_MOVE);

        mFakeNode.sendInputEvent(fakeCapturedInput);
        mFakeNode.sendInputEvent(fakeInput);

        mFakeScheduler.runAll();

        assertThat(mInputListener.lastEvent).isNotNull();
        assertThat(mInputListener.lastEvent.timestamp).isEqualTo(fakeCapturedInput.getTimestamp());
    }

    @Test
    public void onAttach_propagatesInputOnCorrectThread() {
        FakeScheduledExecutorService propagationExecutor = new FakeScheduledExecutorService();
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(
                        propagationExecutor, mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        InputEvent fakeCapturedInput =
                ShadowInputEvent.create(
                        SOURCE_UNKNOWN,
                        POINTER_TYPE_DEFAULT,
                        /* timestamp= */ 100,
                        /* origin= */ new Vec3(0, 0, 0),
                        /* direction= */ new Vec3(1, 1, 1),
                        InputEvent.DISPATCH_FLAG_CAPTURED_POINTER,
                        ACTION_MOVE);

        mFakeNode.sendInputEvent(fakeCapturedInput);

        assertThat(propagationExecutor.hasNext()).isFalse();
        // Run the scheduler associated with the Entity so that the component's executor has the
        // task
        // scheduled on it.
        mFakeScheduler.runAll();

        assertThat(propagationExecutor.hasNext()).isTrue();
    }

    @Test
    public void onAttach_setsUpCorrectStatePropagation() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        mFakeNode.getPointerCaptureStateCallback().accept(Node.POINTER_CAPTURE_STATE_PAUSED);
        assertThat(mStateListener.lastState)
                .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_PAUSED);

        mFakeNode.getPointerCaptureStateCallback().accept(Node.POINTER_CAPTURE_STATE_ACTIVE);
        assertThat(mStateListener.lastState)
                .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_ACTIVE);

        mFakeNode.getPointerCaptureStateCallback().accept(Node.POINTER_CAPTURE_STATE_STOPPED);
        assertThat(mStateListener.lastState)
                .isEqualTo(PointerCaptureComponent.POINTER_CAPTURE_STATE_STOPPED);
    }

    @Test
    public void onAttach_failsIfAlreadyAttached() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();
        assertThat(component.onAttach(mEntity)).isFalse();
    }

    @Test
    public void onAttach_failesIfEntityAlreadyHasAnAttachedPointerCaptureComponent() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        PointerCaptureComponentImpl component2 =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component2.onAttach(mEntity)).isFalse();
    }

    @Test
    public void onDetach_stopsPointerCapture() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        component.onDetach(mEntity);

        assertThat(mFakeNode.getPointerCaptureStateCallback()).isNull();
    }

    @Test
    public void onDetach_removesInputListener() {
        PointerCaptureComponentImpl component =
                new PointerCaptureComponentImpl(directExecutor(), mStateListener, mInputListener);
        assertThat(component.onAttach(mEntity)).isTrue();

        component.onDetach(mEntity);

        assertThat(mFakeNode.getListener()).isNull();
    }
}
