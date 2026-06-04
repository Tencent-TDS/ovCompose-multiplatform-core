package androidx.compose.foundation

import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.platform.v2.nativefoundation.IOSNativeCanvas
import androidx.compose.ui.uikit.utils.ovmp_alloc_trace_id
import androidx.compose.ui.uikit.utils.ovmp_fastlog_log8
import androidx.compose.ui.uikit.utils.ovmp_set_current_trace_id

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
    ovmp_fastlog_log8(
        tag.value.toInt(),
        taskId,
        phase,
        a2,
        a3,
        a4,
        a5,
        a6,
        a7
    )
    // a6/a7 当前未用，如需扩展（flags 等）可再压缩到 a5 或改用额外日志
}

actual fun Canvas.nativePtr(): Long {
    if (this is IOSNativeCanvas) {
        return nativePtr()
    }
    return 0L
}

actual fun fastLogAllocTraceId(): Long {
    return ovmp_alloc_trace_id()
}

actual fun fastLogSetCurrentTraceId(traceId: Long) {
    ovmp_set_current_trace_id(traceId)
}