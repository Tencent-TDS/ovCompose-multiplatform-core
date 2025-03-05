/*
 * Copyright 2017 The Android Open Source Project
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

package android.support.mediacompat.client;

import static android.media.AudioManager.STREAM_MUSIC;
import static android.support.mediacompat.testlib.MediaSessionConstants.RELEASE;
import static android.support.mediacompat.testlib.MediaSessionConstants.RELEASE_AND_THEN_SET_PLAYBACK_STATE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SEND_SESSION_EVENT;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_CAPTIONING_ENABLED;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_EXTRAS;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_FLAGS;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_METADATA;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_STATE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_TO_LOCAL;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_PLAYBACK_TO_REMOTE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_QUEUE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_QUEUE_TITLE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_RATING_TYPE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_REPEAT_MODE;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_SESSION_ACTIVITY;
import static android.support.mediacompat.testlib.MediaSessionConstants.SET_SHUFFLE_MODE;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_ACTION;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_CURRENT_VOLUME;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_ERROR_CODE;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_ERROR_MSG;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_FLAGS;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_KEY;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MAX_VOLUME;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_ID_1;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_MEDIA_ID_2;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_QUEUE_ID_1;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_QUEUE_ID_2;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_SESSION_EVENT;
import static android.support.mediacompat.testlib.MediaSessionConstants.TEST_VALUE;
import static android.support.mediacompat.testlib.VersionConstants.KEY_SERVICE_VERSION;
import static android.support.mediacompat.testlib.util.IntentUtil.SERVICE_PACKAGE_NAME;
import static android.support.mediacompat.testlib.util.IntentUtil.callMediaSessionMethod;
import static android.support.mediacompat.testlib.util.TestUtil.assertBundleEquals;

import static androidx.test.core.app.ApplicationProvider.getApplicationContext;
import static androidx.test.platform.app.InstrumentationRegistry.getArguments;
import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.AudioManager;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.mediacompat.testlib.util.PollingCheck;
import android.util.Log;

import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.filters.MediumTest;
import androidx.test.filters.SdkSuppress;
import androidx.test.filters.SmallTest;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Test {@link android.support.v4.media.session.MediaControllerCompat.Callback}. */
@SuppressWarnings("deprecation")
@RunWith(AndroidJUnit4.class)
public class MediaControllerCompatCallbackTest {

    private static final String TAG = "MediaControllerCompatCallbackTest";

    // The maximum time to wait for an operation, that is expected to happen.
    private static final long TIME_OUT_MS = 3000L;
    private static final int MAX_AUDIO_INFO_CHANGED_CALLBACK_COUNT = 10;

    private static final ComponentName TEST_BROWSER_SERVICE = new ComponentName(
            SERVICE_PACKAGE_NAME,
            "android.support.mediacompat.service.StubMediaBrowserServiceCompat");

    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Object mWaitLock = new Object();

    private String mServiceVersion;

    // MediaBrowserCompat object to get the session token.
    private android.support.v4.media.MediaBrowserCompat mMediaBrowser;
    private ConnectionCallback mConnectionCallback = new ConnectionCallback();

    private android.support.v4.media.session.MediaSessionCompat.Token mSessionToken;
    private android.support.v4.media.session.MediaControllerCompat mController;
    private MediaControllerCallback mMediaControllerCallback = new MediaControllerCallback();

    @Before
    public void setUp() throws Exception {
        // The version of the service app is provided through the instrumentation arguments.
        mServiceVersion = getArguments().getString(KEY_SERVICE_VERSION, "");
        Log.d(TAG, "Service app version: " + mServiceVersion);

        getInstrumentation()
                .runOnMainSync(
                        new Runnable() {
                            @Override
                            public void run() {
                                mMediaBrowser =
                                        new android.support.v4.media.MediaBrowserCompat(
                                                getInstrumentation().getTargetContext(),
                                                TEST_BROWSER_SERVICE,
                                                mConnectionCallback,
                                                new Bundle());
                            }
                        });

        synchronized (mConnectionCallback.mWaitLock) {
            mMediaBrowser.connect();
            mConnectionCallback.mWaitLock.wait(TIME_OUT_MS);
            if (!mMediaBrowser.isConnected()) {
                fail("Browser failed to connect!");
            }
        }
        mSessionToken = mMediaBrowser.getSessionToken();
        mController =
                new android.support.v4.media.session.MediaControllerCompat(
                        getApplicationContext(), mSessionToken);
        mController.registerCallback(mMediaControllerCallback, mHandler);
    }

    @After
    public void tearDown() throws Exception {
        if (mMediaBrowser != null && mMediaBrowser.isConnected()) {
            mMediaBrowser.disconnect();
        }
    }

    @Test
    @SmallTest
    public void testGetPackageName() {
        assertEquals(SERVICE_PACKAGE_NAME, mController.getPackageName());
    }

    @Test
    @SmallTest
    public void testIsSessionReady() throws Exception {
        // mController already has the extra binder since it was created with the session token
        // which holds the extra binder.
        assertTrue(mController.isSessionReady());
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setExtras}. */
    @Test
    @MediumTest
    public void testSetExtras() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();

            Bundle extras = new Bundle();
            extras.putString(TEST_KEY, TEST_VALUE);
            callMediaSessionMethod(SET_EXTRAS, extras, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnExtraChangedCalled);

            assertBundleEquals(extras, mMediaControllerCallback.mExtras);
            assertBundleEquals(extras, mController.getExtras());
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setFlags}. */
    @SuppressWarnings("deprecation")
    @Test
    @SmallTest
    public void testSetFlags() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();

            callMediaSessionMethod(SET_FLAGS, TEST_FLAGS, getApplicationContext());
            new PollingCheck(TIME_OUT_MS) {
                @Override
                public boolean check() {
                    int expectedFlags = TEST_FLAGS;
                    expectedFlags |=
                            android.support.v4.media.session.MediaSessionCompat
                                    .FLAG_HANDLES_MEDIA_BUTTONS;
                    expectedFlags |=
                            android.support.v4.media.session.MediaSessionCompat
                                    .FLAG_HANDLES_TRANSPORT_CONTROLS;
                    return expectedFlags == mController.getFlags();
                }
            }.run();
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setMetadata}. */
    @Test
    @MediumTest
    public void testSetMetadata() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            android.support.v4.media.RatingCompat rating =
                    android.support.v4.media.RatingCompat.newHeartRating(true);
            android.support.v4.media.MediaMetadataCompat metadata =
                    new android.support.v4.media.MediaMetadataCompat.Builder()
                            .putString(TEST_KEY, TEST_VALUE)
                            .putRating(
                                    android.support.v4.media.MediaMetadataCompat
                                            .METADATA_KEY_RATING,
                                    rating)
                            .build();

            callMediaSessionMethod(SET_METADATA, metadata, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnMetadataChangedCalled);

            android.support.v4.media.MediaMetadataCompat metadataOut =
                    mMediaControllerCallback.mMediaMetadata;
            assertNotNull(metadataOut);
            assertEquals(TEST_VALUE, metadataOut.getString(TEST_KEY));

            metadataOut = mController.getMetadata();
            assertNotNull(metadataOut);
            assertEquals(TEST_VALUE, metadataOut.getString(TEST_KEY));

            assertNotNull(
                    metadataOut.getRating(
                            android.support.v4.media.MediaMetadataCompat.METADATA_KEY_RATING));
            android.support.v4.media.RatingCompat ratingOut =
                    metadataOut.getRating(
                            android.support.v4.media.MediaMetadataCompat.METADATA_KEY_RATING);
            assertEquals(rating.getRatingStyle(), ratingOut.getRatingStyle());
            assertEquals(rating.getPercentRating(), ratingOut.getPercentRating(), 0.0f);
        }
    }

    /**
     * Tests {@link android.support.v4.media.session.MediaSessionCompat#setMetadata} with artwork
     * bitmaps.
     */
    @Test
    @SmallTest
    public void testSetMetadataWithArtworks() throws Exception {
        // TODO: Add test with a large bitmap.
        // Using large bitmap makes other tests that are executed after this fail.
        final Bitmap bitmapSmall = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888);

        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            android.support.v4.media.MediaMetadataCompat metadata =
                    new android.support.v4.media.MediaMetadataCompat.Builder()
                            .putString(TEST_KEY, TEST_VALUE)
                            .putBitmap(
                                    android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART,
                                    bitmapSmall)
                            .build();

            callMediaSessionMethod(SET_METADATA, metadata, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnMetadataChangedCalled);

            android.support.v4.media.MediaMetadataCompat metadataOut =
                    mMediaControllerCallback.mMediaMetadata;
            assertNotNull(metadataOut);
            assertEquals(TEST_VALUE, metadataOut.getString(TEST_KEY));

            Bitmap bitmapSmallOut =
                    metadataOut.getBitmap(
                            android.support.v4.media.MediaMetadataCompat.METADATA_KEY_ART);
            assertNotNull(bitmapSmallOut);
            assertEquals(bitmapSmall.getHeight(), bitmapSmallOut.getHeight());
            assertEquals(bitmapSmall.getWidth(), bitmapSmallOut.getWidth());
            assertEquals(bitmapSmall.getConfig(), bitmapSmallOut.getConfig());

            bitmapSmallOut.recycle();
        }
        bitmapSmall.recycle();
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setPlaybackState}. */
    @Test
    @SmallTest
    public void testSetPlaybackState() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            android.support.v4.media.session.PlaybackStateCompat state =
                    new android.support.v4.media.session.PlaybackStateCompat.Builder()
                            .setActions(TEST_ACTION)
                            .setErrorMessage(TEST_ERROR_CODE, TEST_ERROR_MSG)
                            .build();

            callMediaSessionMethod(SET_PLAYBACK_STATE, state, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnPlaybackStateChangedCalled);

            android.support.v4.media.session.PlaybackStateCompat stateOut =
                    mMediaControllerCallback.mPlaybackState;
            assertNotNull(stateOut);
            assertEquals(TEST_ACTION, stateOut.getActions());
            assertEquals(TEST_ERROR_CODE, stateOut.getErrorCode());
            assertEquals(TEST_ERROR_MSG, stateOut.getErrorMessage().toString());

            stateOut = mController.getPlaybackState();
            assertNotNull(stateOut);
            assertEquals(TEST_ACTION, stateOut.getActions());
            assertEquals(TEST_ERROR_CODE, stateOut.getErrorCode());
            assertEquals(TEST_ERROR_MSG, stateOut.getErrorMessage().toString());
        }
    }

    /**
     * Tests {@link android.support.v4.media.session.MediaSessionCompat#setQueue} and {@link
     * android.support.v4.media.session.MediaSessionCompat#setQueueTitle}.
     */
    @Test
    @SmallTest
    public void testSetQueueAndSetQueueTitle() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            List<android.support.v4.media.session.MediaSessionCompat.QueueItem> queue =
                    new ArrayList<>();

            android.support.v4.media.MediaDescriptionCompat description1 =
                    new android.support.v4.media.MediaDescriptionCompat.Builder()
                            .setMediaId(TEST_MEDIA_ID_1)
                            .build();
            android.support.v4.media.MediaDescriptionCompat description2 =
                    new android.support.v4.media.MediaDescriptionCompat.Builder()
                            .setMediaId(TEST_MEDIA_ID_2)
                            .build();
            android.support.v4.media.session.MediaSessionCompat.QueueItem item1 =
                    new android.support.v4.media.session.MediaSessionCompat.QueueItem(
                            description1, TEST_QUEUE_ID_1);
            android.support.v4.media.session.MediaSessionCompat.QueueItem item2 =
                    new android.support.v4.media.session.MediaSessionCompat.QueueItem(
                            description2, TEST_QUEUE_ID_2);
            queue.add(item1);
            queue.add(item2);

            callMediaSessionMethod(SET_QUEUE, queue, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnQueueChangedCalled);

            callMediaSessionMethod(SET_QUEUE_TITLE, TEST_VALUE, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnQueueTitleChangedCalled);

            assertEquals(TEST_VALUE, mMediaControllerCallback.mTitle);
            assertQueueEquals(queue, mMediaControllerCallback.mQueue);

            assertEquals(TEST_VALUE, mController.getQueueTitle());
            assertQueueEquals(queue, mController.getQueue());

            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(SET_QUEUE, null, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnQueueChangedCalled);

            callMediaSessionMethod(SET_QUEUE_TITLE, null, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnQueueTitleChangedCalled);

            assertNull(mMediaControllerCallback.mTitle);
            assertNull(mMediaControllerCallback.mQueue);
            assertNull(mController.getQueueTitle());
            assertNull(mController.getQueue());
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setSessionActivity}. */
    @Test
    @SmallTest
    public void testSessionActivity() throws Exception {
        synchronized (mWaitLock) {
            Intent intent = new Intent("MEDIA_SESSION_ACTION");
            final int requestCode = 555;
            final PendingIntent pi =
                    PendingIntent.getActivity(getApplicationContext(), requestCode, intent,
                            Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0);

            callMediaSessionMethod(SET_SESSION_ACTIVITY, pi, getApplicationContext());
            new PollingCheck(TIME_OUT_MS) {
                @Override
                public boolean check() {
                    return pi.equals(mController.getSessionActivity());
                }
            }.run();
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setCaptioningEnabled}. */
    @Test
    @SmallTest
    public void testSetCaptioningEnabled() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(SET_CAPTIONING_ENABLED, true, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnCaptioningEnabledChangedCalled);
            assertEquals(true, mMediaControllerCallback.mCaptioningEnabled);
            assertEquals(true, mController.isCaptioningEnabled());

            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(SET_CAPTIONING_ENABLED, false, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnCaptioningEnabledChangedCalled);
            assertEquals(false, mMediaControllerCallback.mCaptioningEnabled);
            assertEquals(false, mController.isCaptioningEnabled());
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setRepeatMode}. */
    @Test
    @MediumTest
    public void testSetRepeatMode() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            final int repeatMode =
                    android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_ALL;
            callMediaSessionMethod(SET_REPEAT_MODE, repeatMode, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnRepeatModeChangedCalled);
            assertEquals(repeatMode, mMediaControllerCallback.mRepeatMode);
            assertEquals(repeatMode, mController.getRepeatMode());
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#setShuffleMode}. */
    @Test
    @MediumTest
    public void testSetShuffleMode() throws Exception {
        final int shuffleMode =
                android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_ALL;
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(SET_SHUFFLE_MODE, shuffleMode, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnShuffleModeChangedCalled);
            assertEquals(shuffleMode, mMediaControllerCallback.mShuffleMode);
            assertEquals(shuffleMode, mController.getShuffleMode());
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#sendSessionEvent}. */
    @Test
    @SmallTest
    public void testSendSessionEvent() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();

            Bundle arguments = new Bundle();
            arguments.putString("event", TEST_SESSION_EVENT);

            Bundle extras = new Bundle();
            extras.putString(TEST_KEY, TEST_VALUE);
            arguments.putBundle("extras", extras);
            callMediaSessionMethod(SEND_SESSION_EVENT, arguments, getApplicationContext());

            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnSessionEventCalled);
            assertEquals(TEST_SESSION_EVENT, mMediaControllerCallback.mEvent);
            assertBundleEquals(extras, mMediaControllerCallback.mExtras);
        }
    }

    /** Tests {@link android.support.v4.media.session.MediaSessionCompat#release}. */
    @Test
    @SmallTest
    public void testRelease() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(RELEASE, null, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);
            assertTrue(mMediaControllerCallback.mOnSessionDestroyedCalled);
        }
    }

    /**
     * Tests {@link android.support.v4.media.session.MediaSessionCompat#setPlaybackToLocal} and
     * {@link android.support.v4.media.session.MediaSessionCompat#setPlaybackToRemote}.
     */
    @LargeTest
    public void testPlaybackToLocalAndRemote() throws Exception {
        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            android.support.v4.media.session.ParcelableVolumeInfo volumeInfo =
                    new android.support.v4.media.session.ParcelableVolumeInfo(
                            android.support.v4.media.session.MediaControllerCompat.PlaybackInfo
                                    .PLAYBACK_TYPE_REMOTE,
                            STREAM_MUSIC,
                            androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED,
                            TEST_MAX_VOLUME,
                            TEST_CURRENT_VOLUME);

            callMediaSessionMethod(SET_PLAYBACK_TO_REMOTE, volumeInfo, getApplicationContext());
            android.support.v4.media.session.MediaControllerCompat.PlaybackInfo info = null;
            for (int i = 0; i < MAX_AUDIO_INFO_CHANGED_CALLBACK_COUNT; ++i) {
                mMediaControllerCallback.mOnAudioInfoChangedCalled = false;
                mWaitLock.wait(TIME_OUT_MS);
                assertTrue(mMediaControllerCallback.mOnAudioInfoChangedCalled);
                info = mMediaControllerCallback.mPlaybackInfo;
                if (info != null
                        && info.getCurrentVolume() == TEST_CURRENT_VOLUME
                        && info.getMaxVolume() == TEST_MAX_VOLUME
                        && info.getVolumeControl()
                                == androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED
                        && info.getPlaybackType()
                                == android.support.v4.media.session.MediaControllerCompat
                                        .PlaybackInfo.PLAYBACK_TYPE_REMOTE) {
                    break;
                }
            }
            assertNotNull(info);
            assertEquals(
                    android.support.v4.media.session.MediaControllerCompat.PlaybackInfo
                            .PLAYBACK_TYPE_REMOTE,
                    info.getPlaybackType());
            assertEquals(TEST_MAX_VOLUME, info.getMaxVolume());
            assertEquals(TEST_CURRENT_VOLUME, info.getCurrentVolume());
            assertEquals(
                    androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED,
                    info.getVolumeControl());

            info = mController.getPlaybackInfo();
            assertNotNull(info);
            assertEquals(
                    android.support.v4.media.session.MediaControllerCompat.PlaybackInfo
                            .PLAYBACK_TYPE_REMOTE,
                    info.getPlaybackType());
            assertEquals(TEST_MAX_VOLUME, info.getMaxVolume());
            assertEquals(TEST_CURRENT_VOLUME, info.getCurrentVolume());
            assertEquals(
                    androidx.media.VolumeProviderCompat.VOLUME_CONTROL_FIXED,
                    info.getVolumeControl());

            // test setPlaybackToLocal
            mMediaControllerCallback.mOnAudioInfoChangedCalled = false;
            callMediaSessionMethod(SET_PLAYBACK_TO_LOCAL, AudioManager.STREAM_RING,
                    getApplicationContext());

            // In API 21 and 22, onAudioInfoChanged is not called.
            if (Build.VERSION.SDK_INT == 21 || Build.VERSION.SDK_INT == 22) {
                Thread.sleep(TIME_OUT_MS);
            } else {
                mWaitLock.wait(TIME_OUT_MS);
                assertTrue(mMediaControllerCallback.mOnAudioInfoChangedCalled);
            }

            info = mController.getPlaybackInfo();
            assertNotNull(info);
            assertEquals(
                    android.support.v4.media.session.MediaControllerCompat.PlaybackInfo
                            .PLAYBACK_TYPE_LOCAL,
                    info.getPlaybackType());
        }
    }

    @Test
    @SmallTest
    public void testGetRatingType() {
        assertEquals(
                "Default rating type of a session must be RatingCompat.RATING_NONE",
                android.support.v4.media.RatingCompat.RATING_NONE,
                mController.getRatingType());

        callMediaSessionMethod(
                SET_RATING_TYPE,
                android.support.v4.media.RatingCompat.RATING_5_STARS,
                getApplicationContext());
        new PollingCheck(TIME_OUT_MS) {
            @Override
            public boolean check() {
                return android.support.v4.media.RatingCompat.RATING_5_STARS
                        == mController.getRatingType();
            }
        }.run();
    }

    @Test
    @SmallTest
    @SdkSuppress(minSdkVersion = 21)
    public void testOnSessionReadyCalled_extraBinderIsReadyWhenRegisteringCallback()
            throws Exception {
        mController = null;
        final SessionReadyCallback callback = new SessionReadyCallback();
        synchronized (callback.mWaitLock) {
            getInstrumentation()
                    .runOnMainSync(
                            new Runnable() {
                                @SuppressWarnings("deprecation")
                                @Override
                                public void run() {
                                    try {
                                        mController =
                                                new android.support.v4.media.session
                                                        .MediaControllerCompat(
                                                        getInstrumentation().getTargetContext(),
                                                        /* This token has an extra binder in it */
                                                        mSessionToken);
                                        mController.registerCallback(callback, new Handler());
                                        assertTrue(mController.isSessionReady());
                                        assertFalse(callback.mOnSessionReadyCalled);
                                    } catch (Exception e) {
                                        fail();
                                    }
                                }
                            });
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSessionReadyCalled);
            assertTrue(mController.isSessionReady());
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 21)
    public void testOnSessionReadyCalled_extraBinderIsNotReadyWhenRegisteringCallback()
            throws Exception {
        mController = null;
        final SessionReadyCallback callback = new SessionReadyCallback();
        final android.support.v4.media.session.MediaSessionCompat.Token tokenWithoutExtraBinder =
                android.support.v4.media.session.MediaSessionCompat.Token.fromToken(
                        mSessionToken.getToken());
        synchronized (callback.mWaitLock) {
            getInstrumentation()
                    .runOnMainSync(
                            new Runnable() {
                                @SuppressWarnings("deprecation")
                                @Override
                                public void run() {
                                    try {
                                        // onSessionReady() should be posted when the controller
                                        // receives the
                                        // extra binder from session.
                                        mController =
                                                new android.support.v4.media.session
                                                        .MediaControllerCompat(
                                                        getInstrumentation().getTargetContext(),
                                                        tokenWithoutExtraBinder);
                                        mController.registerCallback(callback, new Handler());
                                        // Since mController.isSessionReady() can be both
                                        // true/false,
                                        // we don't check the return value at this point.
                                        assertFalse(callback.mOnSessionReadyCalled);
                                    } catch (Exception e) {
                                        fail();
                                    }
                                }
                            });
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSessionReadyCalled);
            assertTrue(mController.isSessionReady());
        }
    }

    @Test
    @SmallTest
    @SdkSuppress(maxSdkVersion = 19)
    public void testOnSessionReadyCalled_underApi21() throws Exception {
        mController = null;
        final SessionReadyCallback callback = new SessionReadyCallback();
        synchronized (callback.mWaitLock) {
            getInstrumentation()
                    .runOnMainSync(
                            new Runnable() {
                                @SuppressWarnings("deprecation")
                                @Override
                                public void run() {
                                    try {
                                        // No extra binder exists in API 19. (i.e. session is always
                                        // ready.)
                                        // onSessionReady() should be posted by registerCallback().
                                        mController =
                                                new android.support.v4.media.session
                                                        .MediaControllerCompat(
                                                        getInstrumentation().getTargetContext(),
                                                        mSessionToken);
                                        mController.registerCallback(callback, new Handler());
                                        assertTrue(mController.isSessionReady());
                                        assertFalse(callback.mOnSessionReadyCalled);
                                    } catch (Exception e) {
                                        fail();
                                    }
                                }
                            });
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertTrue(callback.mOnSessionReadyCalled);
            assertTrue(mController.isSessionReady());
        }
    }

    @Test
    @LargeTest
    @SdkSuppress(minSdkVersion = 21)
    public void testOnSessionReadyCalled_fwkMediaSession() throws Exception {
        mController = null;
        final MediaSession session = new MediaSession(getInstrumentation().getTargetContext(),
                "TestFwkSession");
        final SessionReadyCallback callback = new SessionReadyCallback();
        synchronized (callback.mWaitLock) {
            getInstrumentation()
                    .runOnMainSync(
                            new Runnable() {
                                @SuppressWarnings("deprecation")
                                @Override
                                public void run() {
                                    try {
                                        // No extra binder exists in API 19. (i.e. session is always
                                        // ready.)
                                        // onSessionReady() should be posted by registerCallback().
                                        MediaSession.Token fwkToken = session.getSessionToken();
                                        mController =
                                                new android.support.v4.media.session
                                                        .MediaControllerCompat(
                                                        getInstrumentation().getTargetContext(),
                                                        android.support.v4.media.session
                                                                .MediaSessionCompat.Token.fromToken(
                                                                fwkToken));
                                        mController.registerCallback(callback, new Handler());
                                    } catch (Exception e) {
                                        fail();
                                    }
                                }
                            });
            callback.mWaitLock.wait(TIME_OUT_MS);
            assertFalse(mController.isSessionReady());
            assertFalse(callback.mOnSessionReadyCalled);
        }
    }

    @Test
    @SmallTest
    @SuppressWarnings("deprecation")
    public void testReceivingParcelables() throws Exception {
        Bundle arguments = new Bundle();
        arguments.putString("event", TEST_SESSION_EVENT);

        final android.support.v4.media.MediaDescriptionCompat desc =
                new android.support.v4.media.MediaDescriptionCompat.Builder()
                        .setMediaId("testMediaId")
                        .build();
        final android.support.v4.media.session.MediaSessionCompat.QueueItem queueItem =
                new android.support.v4.media.session.MediaSessionCompat.QueueItem(
                        desc, 1 /* flags */);
        final android.support.v4.media.MediaBrowserCompat.MediaItem mediaItem =
                new android.support.v4.media.MediaBrowserCompat.MediaItem(
                        desc, android.support.v4.media.MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
        final android.support.v4.media.session.PlaybackStateCompat state =
                new android.support.v4.media.session.PlaybackStateCompat.Builder()
                        .setBufferedPosition(1000)
                        .build();

        Bundle extras = new Bundle();
        extras.putParcelable("description", desc);
        extras.putParcelable("queueItem", queueItem);
        extras.putParcelable("mediaItem", mediaItem);
        extras.putParcelable("state", state);

        arguments.putBundle("extras", extras);

        synchronized (mWaitLock) {
            mMediaControllerCallback.resetLocked();
            callMediaSessionMethod(SEND_SESSION_EVENT, arguments, getApplicationContext());
            mWaitLock.wait(TIME_OUT_MS);

            assertTrue(mMediaControllerCallback.mOnSessionEventCalled);
            assertEquals(TEST_SESSION_EVENT, mMediaControllerCallback.mEvent);

            Bundle extrasOut = mMediaControllerCallback.mExtras;
            // Viewing the contents of bundle from remote process should not throw any exceptions.
            // Also check whether the bundle has expected contents.
            assertNotNull(extrasOut);
            assertNotNull(extrasOut.getClassLoader());

            android.support.v4.media.MediaDescriptionCompat descOut =
                    extrasOut.getParcelable("description");
            assertEquals(desc.getMediaId(), descOut.getMediaId());

            android.support.v4.media.session.MediaSessionCompat.QueueItem queueItemOut =
                    extrasOut.getParcelable("queueItem");
            assertEquals(queueItem.getQueueId(), queueItemOut.getQueueId());

            android.support.v4.media.MediaBrowserCompat.MediaItem mediaItemOut =
                    extrasOut.getParcelable("mediaItem");
            assertEquals(mediaItem.getFlags(), mediaItemOut.getFlags());

            android.support.v4.media.session.PlaybackStateCompat stateOut =
                    extrasOut.getParcelable("state");
            assertEquals(state.getBufferedPosition(), stateOut.getBufferedPosition());
        }
    }

    @Test
    @LargeTest
    public void testRegisterCallbackTwice() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);

        MediaControllerMultipleCallback callback = new MediaControllerMultipleCallback(latch);
        mController.registerCallback(callback, mHandler);
        mController.registerCallback(callback, mHandler); // it must be ignored

        callMediaSessionMethod(SET_EXTRAS, new Bundle(), getApplicationContext());
        assertFalse(latch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, latch.getCount());
    }

    @Test
    @LargeTest
    public void testUnregisterCallbackTwice() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);

        MediaControllerMultipleCallback callback = new MediaControllerMultipleCallback(latch);
        mController.registerCallback(callback, mHandler);
        mController.unregisterCallback(callback);
        mController.unregisterCallback(callback); // it must be ignored

        callMediaSessionMethod(SET_EXTRAS, new Bundle(), getApplicationContext());
        assertFalse(latch.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    @Test
    @LargeTest
    public void testUnregisterUnknownCallback() throws InterruptedException {
        CountDownLatch latch1 = new CountDownLatch(1);
        CountDownLatch latch2 = new CountDownLatch(1);
        MediaControllerMultipleCallback callback1 = new MediaControllerMultipleCallback(latch1);
        MediaControllerMultipleCallback callback2 = new MediaControllerMultipleCallback(latch2);
        mController.registerCallback(callback1, mHandler);
        mController.unregisterCallback(callback2); // it must be ignored

        callMediaSessionMethod(SET_EXTRAS, new Bundle(), getApplicationContext());
        assertTrue(latch1.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(latch2.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
    }

    /**
     * Tests callbacks are not called after {@link
     * android.support.v4.media.session.MediaSessionCompat#release}.
     */
    @Test
    @LargeTest
    public void testCallbacksAreNotCalledAfterRelease() throws Exception {
        final CountDownLatch latchForDestroy = new CountDownLatch(1);
        final CountDownLatch latchForPlaybackState = new CountDownLatch(1);

        mController.registerCallback(
                new android.support.v4.media.session.MediaControllerCompat.Callback() {
                    @Override
                    public void onSessionDestroyed() {
                        latchForDestroy.countDown();
                    }

                    @Override
                    public void onPlaybackStateChanged(
                            android.support.v4.media.session.PlaybackStateCompat state) {
                        latchForPlaybackState.countDown();
                    }
                },
                mHandler);

        callMediaSessionMethod(RELEASE_AND_THEN_SET_PLAYBACK_STATE, null, getApplicationContext());
        assertTrue(latchForDestroy.await(TIME_OUT_MS, TimeUnit.MILLISECONDS));
        assertFalse(latchForPlaybackState.await(1000, TimeUnit.MILLISECONDS));
    }

    private void assertQueueEquals(
            List<android.support.v4.media.session.MediaSessionCompat.QueueItem> expected,
            List<android.support.v4.media.session.MediaSessionCompat.QueueItem> observed) {
        if (expected == null || observed == null) {
            assertTrue(expected == observed);
            return;
        }

        assertEquals(expected.size(), observed.size());
        for (int i = 0; i < expected.size(); i++) {
            android.support.v4.media.session.MediaSessionCompat.QueueItem expectedItem =
                    expected.get(i);
            android.support.v4.media.session.MediaSessionCompat.QueueItem observedItem =
                    observed.get(i);

            assertEquals(expectedItem.getQueueId(), observedItem.getQueueId());
            assertEquals(expectedItem.getDescription().getMediaId(),
                    observedItem.getDescription().getMediaId());
        }
    }

    private class MediaControllerCallback
            extends android.support.v4.media.session.MediaControllerCompat.Callback {
        private volatile boolean mOnPlaybackStateChangedCalled;
        private volatile boolean mOnMetadataChangedCalled;
        private volatile boolean mOnQueueChangedCalled;
        private volatile boolean mOnQueueTitleChangedCalled;
        private volatile boolean mOnExtraChangedCalled;
        private volatile boolean mOnAudioInfoChangedCalled;
        private volatile boolean mOnSessionDestroyedCalled;
        private volatile boolean mOnSessionEventCalled;
        private volatile boolean mOnCaptioningEnabledChangedCalled;
        private volatile boolean mOnRepeatModeChangedCalled;
        private volatile boolean mOnShuffleModeChangedCalled;

        private volatile android.support.v4.media.session.PlaybackStateCompat mPlaybackState;
        private volatile android.support.v4.media.MediaMetadataCompat mMediaMetadata;
        private volatile List<android.support.v4.media.session.MediaSessionCompat.QueueItem> mQueue;
        private volatile CharSequence mTitle;
        private volatile String mEvent;
        private volatile Bundle mExtras;
        private volatile android.support.v4.media.session.MediaControllerCompat.PlaybackInfo
                mPlaybackInfo;
        private volatile boolean mCaptioningEnabled;
        private volatile int mRepeatMode;
        private volatile int mShuffleMode;

        public void resetLocked() {
            mOnPlaybackStateChangedCalled = false;
            mOnMetadataChangedCalled = false;
            mOnQueueChangedCalled = false;
            mOnQueueTitleChangedCalled = false;
            mOnExtraChangedCalled = false;
            mOnAudioInfoChangedCalled = false;
            mOnSessionDestroyedCalled = false;
            mOnSessionEventCalled = false;
            mOnRepeatModeChangedCalled = false;
            mOnShuffleModeChangedCalled = false;

            mPlaybackState = null;
            mMediaMetadata = null;
            mQueue = null;
            mTitle = null;
            mExtras = null;
            mPlaybackInfo = null;
            mCaptioningEnabled = false;
            mRepeatMode = android.support.v4.media.session.PlaybackStateCompat.REPEAT_MODE_NONE;
            mShuffleMode = android.support.v4.media.session.PlaybackStateCompat.SHUFFLE_MODE_NONE;
        }

        @Override
        public void onPlaybackStateChanged(
                android.support.v4.media.session.PlaybackStateCompat state) {
            synchronized (mWaitLock) {
                mOnPlaybackStateChangedCalled = true;
                mPlaybackState = state;
                mWaitLock.notify();
            }
        }

        @Override
        public void onMetadataChanged(android.support.v4.media.MediaMetadataCompat metadata) {
            synchronized (mWaitLock) {
                mOnMetadataChangedCalled = true;
                mMediaMetadata = metadata;
                mWaitLock.notify();
            }
        }

        @Override
        public void onQueueChanged(
                List<android.support.v4.media.session.MediaSessionCompat.QueueItem> queue) {
            synchronized (mWaitLock) {
                mOnQueueChangedCalled = true;
                mQueue = queue;
                mWaitLock.notify();
            }
        }

        @Override
        public void onQueueTitleChanged(CharSequence title) {
            synchronized (mWaitLock) {
                mOnQueueTitleChangedCalled = true;
                mTitle = title;
                mWaitLock.notify();
            }
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            synchronized (mWaitLock) {
                mOnExtraChangedCalled = true;
                mExtras = extras;
                mWaitLock.notify();
            }
        }

        @Override
        public void onAudioInfoChanged(
                android.support.v4.media.session.MediaControllerCompat.PlaybackInfo info) {
            synchronized (mWaitLock) {
                mOnAudioInfoChangedCalled = true;
                mPlaybackInfo = info;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSessionDestroyed() {
            synchronized (mWaitLock) {
                mOnSessionDestroyedCalled = true;
                mWaitLock.notify();
            }
        }

        @Override
        public void onSessionEvent(String event, Bundle extras) {
            synchronized (mWaitLock) {
                mOnSessionEventCalled = true;
                mEvent = event;
                mExtras = (Bundle) extras.clone();
                mWaitLock.notify();
            }
        }

        @Override
        public void onCaptioningEnabledChanged(boolean enabled) {
            synchronized (mWaitLock) {
                mOnCaptioningEnabledChangedCalled = true;
                mCaptioningEnabled = enabled;
                mWaitLock.notify();
            }
        }

        @Override
        public void onRepeatModeChanged(int repeatMode) {
            synchronized (mWaitLock) {
                mOnRepeatModeChangedCalled = true;
                mRepeatMode = repeatMode;
                mWaitLock.notify();
            }
        }

        @Override
        public void onShuffleModeChanged(int shuffleMode) {
            synchronized (mWaitLock) {
                mOnShuffleModeChangedCalled = true;
                mShuffleMode = shuffleMode;
                mWaitLock.notify();
            }
        }
    }

    private class MediaControllerMultipleCallback
            extends android.support.v4.media.session.MediaControllerCompat.Callback {
        private CountDownLatch mLatch;

        MediaControllerMultipleCallback(CountDownLatch latch) {
            mLatch = latch;
        }

        @Override
        public void onExtrasChanged(Bundle extras) {
            mLatch.countDown();
        }
    }

    private class SessionReadyCallback
            extends android.support.v4.media.session.MediaControllerCompat.Callback {
        final Object mWaitLock = new Object();
        private volatile boolean mOnSessionReadyCalled;

        @Override
        public void onSessionReady() {
            synchronized (mWaitLock) {
                mOnSessionReadyCalled = true;
                mWaitLock.notify();
            }
        }
    }

    private class ConnectionCallback
            extends android.support.v4.media.MediaBrowserCompat.ConnectionCallback {
        final Object mWaitLock = new Object();

        @Override
        public void onConnected() {
            synchronized (mWaitLock) {
                mWaitLock.notify();
            }
        }
    }
}
