package androidx.compose.ui.platform

import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_global_queue

internal fun scheduleGCAsync() {
    dispatch_async(dispatch_get_global_queue(0, 0u)) {
        kotlin.native.internal.GC.schedule()
    }
}