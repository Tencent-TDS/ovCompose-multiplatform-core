package androidx.compose.foundation

import androidx.compose.ui.graphics.Canvas

actual inline fun fastLogImpl(
    tag: FastTraceTag,
    taskId: Long,
    phase: Long,
    a2: Long,
    a3: Long,
    a4: Long,
    a5: Long,
    a6: Long,
    a7: Long
) {
}

actual fun Canvas.nativePtr(): Long {
    return 0L
}

actual fun fastLogAllocTraceId(): Long {
    return 0L
}

actual fun fastLogSetCurrentTraceId(traceId: Long) {
}