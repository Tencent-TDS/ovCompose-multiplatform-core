package androidx.compose.ui.util

import platform.ohos.OH_HiTrace_FinishTrace
import platform.ohos.OH_HiTrace_StartTrace

actual inline fun <T> trace(sectionName: String, block: () -> T): T {
    OhosTraceDelegate.start(sectionName)
    try {
        return block()
    } finally {
        OhosTraceDelegate.end()
    }
}

@ThreadLocal
object OhosTraceDelegate {

    fun start(sectionName: String) {
        OH_HiTrace_StartTrace(sectionName)
    }

    fun end() {
        OH_HiTrace_FinishTrace()
    }
}