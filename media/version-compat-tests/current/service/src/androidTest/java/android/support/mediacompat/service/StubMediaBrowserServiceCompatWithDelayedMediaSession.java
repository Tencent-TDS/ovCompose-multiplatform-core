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

package android.support.mediacompat.service;

import android.os.Bundle;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * Stub implementation of {@link androidx.media.MediaBrowserServiceCompat}. This implementation does
 * not call {@link
 * androidx.media.MediaBrowserServiceCompat#setSessionToken(
 * android.support.v4.media.session.MediaSessionCompat.Token)}
 * in its {@link android.app.Service#onCreate}.
 */
@SuppressWarnings("deprecation")
public class StubMediaBrowserServiceCompatWithDelayedMediaSession
        extends androidx.media.MediaBrowserServiceCompat {

    static StubMediaBrowserServiceCompatWithDelayedMediaSession sInstance;
    private android.support.v4.media.session.MediaSessionCompat mSession;

    @Override
    public void onCreate() {
        super.onCreate();
        sInstance = this;
        mSession =
                new android.support.v4.media.session.MediaSessionCompat(
                        this, "StubMediaBrowserServiceCompatWithDelayedMediaSession");
    }

    @Nullable
    @Override
    public BrowserRoot onGetRoot(@NonNull String clientPackageName,
            int clientUid, @Nullable Bundle rootHints) {
        return new BrowserRoot("StubRootId", null);
    }

    @Override
    public void onLoadChildren(
            @NonNull String parentId,
            @NonNull Result<List<android.support.v4.media.MediaBrowserCompat.MediaItem>> result) {
        result.detach();
    }

    public void callSetSessionToken() {
        setSessionToken(mSession.getSessionToken());
    }
}
