/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.runtime

import kotlinx.cinterop.alloc
import kotlinx.cinterop.free
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.nativeHeap
import kotlinx.cinterop.ptr
import platform.posix.PTHREAD_MUTEX_RECURSIVE
import platform.posix.pthread_mutex_destroy
import platform.posix.pthread_mutex_init
import platform.posix.pthread_mutex_lock
import platform.posix.pthread_mutex_t
import platform.posix.pthread_mutex_trylock
import platform.posix.pthread_mutex_unlock
import platform.posix.pthread_mutexattr_destroy
import platform.posix.pthread_mutexattr_init
import platform.posix.pthread_mutexattr_settype
import platform.posix.pthread_mutexattr_t
import kotlin.native.ref.createCleaner

// region Tencent Code
actual open class Lock {
    protected val nativeLock = nativeHeap.alloc<pthread_mutex_t>()

    private val cleaner = createCleaner(nativeLock) {
        pthread_mutex_destroy(it.ptr)
        nativeHeap.free(it)
    }

    fun lock() {
        pthread_mutex_lock(nativeLock.ptr)
    }

    fun unlock() {
        pthread_mutex_unlock(nativeLock.ptr)
    }

    fun tryLock(): Boolean = pthread_mutex_trylock(nativeLock.ptr) == 0
}

private class NativeLockImpl : Lock() {
    init {
        pthread_mutex_init(nativeLock.ptr, null)
    }
}

private class NativeReentrantLockImpl : Lock() {
    init {
        memScoped {
            // Create mutex attributes and set recursive.
            val attr = alloc<pthread_mutexattr_t>()
            pthread_mutexattr_init(attr.ptr)
            pthread_mutexattr_settype(attr.ptr, PTHREAD_MUTEX_RECURSIVE)

            pthread_mutex_init(nativeLock.ptr, attr.ptr)

            // Destroy attr right after mutex initialized.
            pthread_mutexattr_destroy(attr.ptr)
        }
    }
}

actual fun platformSynchronizedObject(): Lock = NativeLockImpl()

actual fun platformReentrantLockObject(): Lock = NativeReentrantLockImpl()

actual inline fun <R> synchronized(lock: Lock, block: () -> R): R {
    lock.lock()
    try {
        return block()
    } finally {
        lock.unlock()
    }
}
// endregion
