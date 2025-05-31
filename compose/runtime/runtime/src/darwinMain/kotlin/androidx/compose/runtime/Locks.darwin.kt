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

import platform.Foundation.NSLock
import platform.Foundation.NSRecursiveLock

// region Tencent Code

actual open class Lock {
    open fun lock() = Unit

    open fun unlock() = Unit

    open fun tryLock(): Boolean = false
}

private class NativeLockImpl : Lock() {
    private val nativeLock = NSLock()

    override fun lock() {
        nativeLock.lock()
    }

    override fun unlock() {
        nativeLock.unlock()
    }

    override fun tryLock(): Boolean = nativeLock.tryLock()
}

private class NativeReentrantLockImpl : Lock() {
    private val nativeLock = NSRecursiveLock()

    override fun lock() {
        nativeLock.lock()
    }

    override fun unlock() {
        nativeLock.unlock()
    }

    override fun tryLock(): Boolean = nativeLock.tryLock()
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
