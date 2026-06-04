package androidx.compose.ui.platform

actual fun traceSync(scene: Int, taskId: Long, block: () -> Unit) {
    block()
}

actual fun traceSync(scene: Int, block: () -> Unit) {
   block()
}
actual fun performTraceOpen(): Boolean = false