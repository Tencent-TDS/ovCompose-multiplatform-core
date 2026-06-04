package androidx.compose.ui.platform

import androidx.compose.runtime.platformSynchronizedObject
import androidx.compose.runtime.synchronized

object ChoreographerManager {

    private var curFrameTimeNs = 0L
    private val backingCallbacks1 = ArrayList<FrameCallback>()
    private val backingCallbacks2 = ArrayList<FrameCallback>()
    private var callbacks = backingCallbacks1
    private val lock = platformSynchronizedObject()

    /**
     * 运行在主、子线程
     * */
    fun postFrameCallback(callback: FrameCallback) {
        synchronized(lock) {
            callbacks.add(callback)
        }
    }

    /**
     * 运行在主线程
     * */
    fun removeFrameCallback(callback: FrameCallback) {
        synchronized(lock) {
            callbacks.remove(callback)
        }
    }

    /**
     * 运行在主线程
     * */
    fun onVsync(timestamp: Long) {
        if (curFrameTimeNs == timestamp) {
            return
        }
        curFrameTimeNs = timestamp
        var list: ArrayList<FrameCallback>? = null
        synchronized(lock) {
            list = callbacks
            callbacks = if (callbacks === backingCallbacks1) {
                backingCallbacks2
            } else {
                backingCallbacks1
            }
        }
        list?.forEach { it.doFrame(timestamp) }
        list?.clear()
    }
}