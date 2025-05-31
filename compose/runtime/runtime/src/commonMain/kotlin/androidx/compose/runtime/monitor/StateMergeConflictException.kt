package androidx.compose.runtime.monitor

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.snapshots.StateObject

// region Tencent Code
class StateMergeConflictException(val state: StateObject) :
    Exception("${state.sourceFile} $state merge conflict")

private val StateObject.sourceFile get() = if (this is MutableState<*>) getSourceFile() else ""
// endregion