package androidx.compose.ui.platform

import androidx.compose.ui.platform.v2.PerformanceTrace

actual fun traceSync(scene: Int, taskId: Long, block: () -> Unit) {
    PerformanceTrace.traceSync(scene, taskId, block)
}
actual fun traceSync(scene: Int, block: () -> Unit) {
    PerformanceTrace.traceSync(scene, block)
}

actual fun performTraceOpen(): Boolean = PerformanceTrace.enabled