package androidx.compose.ui.render

import org.jetbrains.skia.DirectContext
import kotlin.concurrent.Volatile

object SkiaMemoryDumper {

    @Volatile
    var maxSkiaMemory: DirectContext.SkiaMemory? = null
        private set

    private const val DEFAULT_DUMP_INTERVAL = 10 * 1000 * 1000 * 1000L
    private var open = false
    private var debug = false
    private var dumpInterval = DEFAULT_DUMP_INTERVAL
    private var lastDumpStamp = 0L

    /**
     * 启动监控
     * @param dumpInterval a long value indicating the dump interval in nanoseconds
     */
    fun start(debug: Boolean = false, dumpInterval: Long = DEFAULT_DUMP_INTERVAL) {
        this.debug = debug
        this.dumpInterval = dumpInterval
        open = true
        // 重置状态
        lastDumpStamp = 0L
        maxSkiaMemory = null
    }

    /**
     * 停止监控
     */
    fun stop() {
        open = false
    }

    /**
     * 清理记录的数据
     */
    fun reset() {
        maxSkiaMemory = null
    }

    /**
     * 在每一帧调用此方法来转储 Skia 内存信息
     * 内部会根据设定的时间间隔来节流，避免频繁执行
     */
    @Deprecated("框架相关内容，业务不要自行调用")
    fun dump(context: DirectContext, timestamp: Long) {
        if (!open) {
            log { "return open=$open, timestamp=$timestamp" }
            return
        }

        if (timestamp - lastDumpStamp < dumpInterval) {
            return
        }

        lastDumpStamp = timestamp

        val skiaMemory = context.dumpSkiaMemory()
        val currentMaxSize = maxSkiaMemory?.totalSize ?: 0L
        if (skiaMemory.totalSize > currentMaxSize) {
            maxSkiaMemory = skiaMemory
        }

        log { "dump maxSkiaMemory=$maxSkiaMemory" }
    }

    private inline fun log(content: () -> String) {
        if (debug) {
            println("SkiaMemoryDumper ${content()}")
        }
    }
}
