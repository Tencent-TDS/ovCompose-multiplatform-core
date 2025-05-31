package androidx.compose.runtime

// region Tencent Code
actual typealias Lock = Any

actual fun platformSynchronizedObject(): Lock = Lock()

actual fun platformReentrantLockObject(): Lock = Lock()

actual inline fun <R> synchronized(lock: Lock, block: () -> R): R = kotlinx.atomicfu.locks.synchronized(lock, block)
// endregion
