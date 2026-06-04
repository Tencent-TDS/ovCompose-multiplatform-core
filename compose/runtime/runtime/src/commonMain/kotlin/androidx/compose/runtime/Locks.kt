package androidx.compose.runtime

// region Tencent Code
expect class Lock

/* 创建普通的锁 */
expect fun platformSynchronizedObject(): Lock

/* 创建可重入锁 */
expect fun platformReentrantLockObject(): Lock

/**
 * Don't use this API to lock anything.
 * It's unsafe as what its name says.
 */
expect inline fun <R> unsafeSynchronized(lock: Lock, block: () -> R): R
expect inline fun <R> synchronized(lock: Lock, block: () -> R): R
// endregion
