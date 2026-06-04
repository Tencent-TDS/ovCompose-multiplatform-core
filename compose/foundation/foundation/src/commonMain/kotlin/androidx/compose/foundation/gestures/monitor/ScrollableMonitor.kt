package androidx.compose.foundation.gestures.monitor

import kotlin.experimental.ExperimentalObjCName
import kotlin.native.ObjCName

@OptIn(ExperimentalObjCName::class)
@ObjCName("ComposeScrollableMonitor", exact = true)
interface ScrollableMonitor {

    val isAggressive: Boolean
        get() = false

    @OptIn(ExperimentalObjCName::class)
    @ObjCName("onStartScroll")
    fun onStartScroll(@ObjCName("withNames") names: List<String>)

    @ObjCName("onStopScroll")
    fun onStopScroll()

}

internal var sharedScrollableMonitor: ScrollableMonitor? = null
    private set

fun initializeScrollableMonitor(scrollableMonitor: ScrollableMonitor?) {
    sharedScrollableMonitor = scrollableMonitor
}