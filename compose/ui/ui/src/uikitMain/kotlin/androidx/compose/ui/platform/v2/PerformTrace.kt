package androidx.compose.ui.platform.v2

import androidx.compose.ui.uikit.utils.TMMNativeTraceBegin
import androidx.compose.ui.uikit.utils.TMMNativeTraceEnd
import androidx.compose.ui.uikit.utils.TMMNativeTraceScene

object PerformanceTrace {

    var traceImpl: SyncTraceInterface? = null
        set(value) {
            if (traceImpl == null) {
                field = value
            }
        }

    val enabled: Boolean get() = traceImpl != null

    var globalVsyncId: Long = 0
        private set

    fun increaseVsyncId(): Long {
        if (traceImpl != null) {
            globalVsyncId++
        }
        return globalVsyncId
    }

    inline fun <T> traceSync(scene: Int, taskId: Long, block: () -> T): T {
        val trace = traceImpl
        return if (trace == null) {
            block()
        } else {
            trace.startTrace(scene, taskId)
            val result = block()
            trace.endTrace(scene, taskId)
            result
        }
    }
}

interface SyncTraceInterface {
    fun startTrace(scene: Int, taskId: Long)

    fun endTrace(scene: Int, taskId: Long)
}

value class TraceScene private constructor(val value: Int) {
    companion object {
        val None = TraceScene(0)

        // Compose Scene draw
        val DrawFrame = TraceScene(1)
    }
}

object DefaultSignPostSyncTrace : SyncTraceInterface {
    override fun startTrace(scene: Int, taskId: Long) {
        TMMNativeTraceBegin(TMMNativeTraceScene.TMMNativeTraceSceneDrawFrame.value, taskId)
    }

    override fun endTrace(scene: Int, taskId: Long) {
        TMMNativeTraceEnd(TMMNativeTraceScene.TMMNativeTraceSceneDrawFrame.value, taskId)
    }
}