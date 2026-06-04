package androidx.compose.foundation

import androidx.compose.runtime.ComposeTabService
import androidx.compose.ui.graphics.Canvas
import kotlin.jvm.JvmInline

@JvmInline
value class FastTraceTag(val value: Long) {
    companion object {
        val None = FastTraceTag(0)
        val FrameBegin = FastTraceTag(666)
        val FrameEnd = FastTraceTag(667)
        val Text = FastTraceTag(668)
        val Image = FastTraceTag(669)
    }
}

@JvmInline
value class OVMPFastLogPhase(val value: Long) {
    companion object {
        /* 文本渲染 Sync 开始 */
        val TextOnKtSyncBegin = OVMPFastLogPhase(0)

        /* 文本渲染 Async1 开始 */
        val TextOnKtAsync1Begin = OVMPFastLogPhase(1)

        /* 文本渲染 Async3 开始 */
        val TextOnKtAsync3Begin = OVMPFastLogPhase(2)

        /* 文本在主线程命中缓存而渲染结束 */
        val TextOnKtSyncHitCacheEnd = OVMPFastLogPhase(3)

        /* 文本在主线程命中 DrawNullText 而渲染结束 */
        val TextOnKtSyncDrawNullTextEnd = OVMPFastLogPhase(4)

        /* 文本在 async3 函数内 DrawNullText 结束 */
        val TextOnKtAsync3DrawNullTextEnd = OVMPFastLogPhase(5)

        /* 文本在 async3 函数中命中缓存而结束 */
        val TextOnKtAsync3HitCacheEnd = OVMPFastLogPhase(6)

        /* 文本在 async1 函数内 DrawNullText 结束 */
        val TextOnKtAsync1DrawNullTextEnd = OVMPFastLogPhase(7)

        /* 文本在 async1 函数内 Paint 结束 */
        val TextOnKtAsync1HitCacheEnd = OVMPFastLogPhase(8)

        /* 调用了 Compose 文本函数*/
        val TextFunc = OVMPFastLogPhase(9)

        /* 调用了 Compose Image 函数*/
        val ImageFunc = OVMPFastLogPhase(10)
    }
}

expect inline fun fastLogImpl(
    tag: FastTraceTag,
    taskId: Long,
    phase: Long,
    a2: Long,
    a3: Long,
    a4: Long,
    a5: Long,
    a6: Long,
    a7: Long
)

inline fun fastLog(
    tag: FastTraceTag = FastTraceTag.None,
    taskId: Long = 0,
    phase: Long = 0,
    a2: Long = 0,
    a3: Long = 0,
    a4: Long = 0,
    a5: Long = 0,
    a6: Long = 0,
    a7: Long = 0,
) {
    if (ComposeTabService.composeFastLogEnable) {
        fastLogImpl(
            tag = tag,
            taskId = taskId,
            phase = phase,
            a2 = a2,
            a3 = a3,
            a4 = a4,
            a5 = a5,
            a6 = a6,
            a7 = a7,
        )
    }
}

expect fun Canvas.nativePtr(): Long

/**
 * 分配一个新的全局唯一 traceId（Text 和 Image 共用）
 */
expect fun fastLogAllocTraceId(): Long

/**
 * 设置当前正在处理的 traceId（Native 侧可通过 ovmp_get_current_trace_id 读取）
 */
expect fun fastLogSetCurrentTraceId(traceId: Long)

inline fun String.utf16Head4AsLong(): Long {
    val len = length
    return when {
        len >= 4 -> this[0].code.toLong() or (this[1].code.toLong() shl 16) or
                (this[2].code.toLong() shl 32) or (this[3].code.toLong() shl 48)

        len == 3 -> this[0].code.toLong() or (this[1].code.toLong() shl 16) or (this[2].code.toLong() shl 32)
        len == 2 -> this[0].code.toLong() or (this[1].code.toLong() shl 16)
        len == 1 -> this[0].code.toLong()
        else -> 0L
    }
}