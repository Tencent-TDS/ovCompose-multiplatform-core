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

package androidx.xr.extensions;

import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.util.Objects;

/** A wrapper class for {@link IBinder}. */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public abstract class IBinderWrapper {

    private final IBinder mToken;

    public IBinderWrapper(@NonNull IBinder token) {
        mToken = token;
    }

    @NonNull
    protected IBinder getRawToken() {
        return mToken;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IBinderWrapper)) return false;
        IBinderWrapper token = (IBinderWrapper) o;
        return Objects.equals(mToken, token.mToken);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mToken);
    }

    @NonNull
    @Override
    public String toString() {
        return "Token{" + "mToken=" + mToken + '}';
    }
}
