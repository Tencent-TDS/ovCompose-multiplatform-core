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

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.media.SoundPool;

import androidx.xr.extensions.node.Node;
import androidx.xr.scenecore.JxrPlatformAdapter;
import androidx.xr.scenecore.JxrPlatformAdapter.SoundPoolExtensionsWrapper;
import androidx.xr.scenecore.JxrPlatformAdapter.SpatializerConstants;
import androidx.xr.scenecore.testing.FakeXrExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSoundPoolExtensions;
import androidx.xr.scenecore.testing.FakeXrExtensions.FakeSpatialAudioExtensions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class SoundPoolExtensionsWrapperImplTest {

    private static final int TEST_SOUND_ID = 0;
    private static final float TEST_VOLUME = 0F;
    private static final int TEST_PRIORITY = 0;
    private static final int TEST_LOOP = 0;
    private static final float TEST_RATE = 0F;

    FakeXrExtensions mFakeXrExtensions;
    FakeSpatialAudioExtensions mFakeSpatialAudioExtensions;
    FakeSoundPoolExtensions mFakeSoundPoolExtensions;

    @Before
    public void setUp() {
        mFakeXrExtensions = new FakeXrExtensions();
        mFakeSpatialAudioExtensions = mFakeXrExtensions.fakeSpatialAudioExtensions;
        mFakeSoundPoolExtensions = mFakeSpatialAudioExtensions.soundPoolExtensions;
    }

    @Test
    public void playWithPointSource_callsExtensionsPlayWithPointSource() {
        int expected = 123;

        Node fakeNode = new FakeXrExtensions().createNode();
        AndroidXrEntity entity = mock(AndroidXrEntity.class);
        when(entity.getNode()).thenReturn(fakeNode);
        JxrPlatformAdapter.PointSourceAttributes rtAttributes =
                new JxrPlatformAdapter.PointSourceAttributes(entity);

        SoundPool soundPool = new SoundPool.Builder().build();

        mFakeSoundPoolExtensions.setPlayAsPointSourceResult(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mFakeSoundPoolExtensions);
        int actual =
                wrapper.play(
                        soundPool,
                        TEST_SOUND_ID,
                        rtAttributes,
                        TEST_VOLUME,
                        TEST_PRIORITY,
                        TEST_LOOP,
                        TEST_RATE);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void playWithSoundField_callsExtensionsPlayWithSoundField() {
        int expected = 312;

        SoundPool soundPool = new SoundPool.Builder().build();

        mFakeSoundPoolExtensions.setPlayAsSoundFieldResult(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mFakeSoundPoolExtensions);
        JxrPlatformAdapter.SoundFieldAttributes attributes =
                new JxrPlatformAdapter.SoundFieldAttributes(
                        JxrPlatformAdapter.SpatializerConstants.AMBISONICS_ORDER_THIRD_ORDER);

        int actual =
                wrapper.play(
                        soundPool,
                        TEST_SOUND_ID,
                        attributes,
                        TEST_VOLUME,
                        TEST_PRIORITY,
                        TEST_LOOP,
                        TEST_RATE);

        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void getSpatialSourceType_returnsFromExtensions() {
        int expected = SpatializerConstants.SOURCE_TYPE_SOUND_FIELD;
        SoundPool soundPool = new SoundPool.Builder().build();

        mFakeSoundPoolExtensions.setSourceType(expected);
        SoundPoolExtensionsWrapper wrapper =
                new SoundPoolExtensionsWrapperImpl(mFakeSoundPoolExtensions);
        int actualSourceType = wrapper.getSpatialSourceType(soundPool, /* streamId= */ 0);
        assertThat(actualSourceType).isEqualTo(SpatializerConstants.SOURCE_TYPE_SOUND_FIELD);
    }
}
