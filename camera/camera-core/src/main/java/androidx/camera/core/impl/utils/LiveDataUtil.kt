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

package androidx.camera.core.impl.utils

import androidx.annotation.MainThread
import androidx.arch.core.util.Function
import androidx.camera.core.impl.utils.Threads.runOnMain
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.Observer

public object LiveDataUtil {
    /**
     * Returns a [LiveData] mapped from the input [LiveData] by applying {@code mapFunction} to each
     * value set on {@code source}.
     *
     * Similar to [androidx.lifecycle.Transformations.map], but it can ensure [getValue] returns
     * mapped value without the need to have active observers.
     */
    @MainThread
    @JvmStatic
    public fun <X> map(source: LiveData<X>, mapFunction: Function<X, X>): LiveData<X> {
        val result = RedirectableLiveData<X>(mapFunction.apply(source.value!!))
        result.redirectToWithMapping(source, mapFunction)
        return result
    }
}

/**
 * A [LiveData] which can be redirected to another [LiveData]. If no redirection is set, initial
 * value will be used. Optionally, a map function can be supplied to transform every value delivered
 * in observers or via [getValue].
 */
public class RedirectableLiveData<T> internal constructor(private val mInitialValue: T) :
    MediatorLiveData<T>() {
    private var mLiveDataSource: LiveData<T>? = null
    private var mapFunction: Function<T, T>? = null

    public fun redirectTo(liveDataSource: LiveData<T>) {
        redirectToWithMapping(liveDataSource, mapFunction = null)
    }

    public fun redirectToWithMapping(liveDataSource: LiveData<T>, mapFunction: Function<T, T>?) {
        if (mLiveDataSource != null) {
            super.removeSource(mLiveDataSource!!)
        }
        mLiveDataSource = liveDataSource
        this.mapFunction = mapFunction
        runOnMain {
            // addSource should be invoked in main thread.
            super.addSource(liveDataSource) { value: T ->
                this.setValue(mapFunction?.apply(value) ?: value)
            }
        }
    }

    override fun <S> addSource(source: LiveData<S>, onChanged: Observer<in S>) {
        throw UnsupportedOperationException()
    }

    // Overrides getValue() to reflect the correct value from source. This is required to ensure
    // getValue() is correct when observe() or observeForever() is not called.
    override fun getValue(): T? {
        // Returns initial value if source is not set.
        val livedataValue = mLiveDataSource?.value ?: mInitialValue
        return mapFunction?.apply(livedataValue) ?: livedataValue
    }
}
