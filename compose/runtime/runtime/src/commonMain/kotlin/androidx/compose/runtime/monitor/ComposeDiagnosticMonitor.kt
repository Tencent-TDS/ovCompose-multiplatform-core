package androidx.compose.runtime.monitor

// region Tencent Code
enum class Severity {
    ERROR,
    WARNING
}

data class Diagnostic(
    val name: String,
    val severity: Severity,
    val message: String,
    val error: Throwable?
)

object ComposeDiagnosticMonitor {

    const val KEY_READ_WRITE_STATE_ON_WRONG_THREAD = "read_write_state_on_wrong_thread"
    const val KEY_STATE_MERGE_CONFLICT = "state_merge_conflict"

    var fallbackStateError = false
    var stateMergeConflictFix = false
    var adapter: ComposeMonitorAdapter = DefaultComposeMonitorAdapter()

    fun reportStateOnWrongThread(error: Throwable) {
        Diagnostic(
            KEY_READ_WRITE_STATE_ON_WRONG_THREAD,
            Severity.ERROR,
            error.message ?: "",
            error
        ).let {
            adapter.onReportDiagnostic(it)
        }
    }

    fun reportStateMergeConflict(error: Throwable) {
        Diagnostic(
            KEY_STATE_MERGE_CONFLICT,
            Severity.ERROR,
            error.message ?: "",
            error
        ).let {
            adapter.onReportDiagnostic(it)
        }
    }
}
// endregion