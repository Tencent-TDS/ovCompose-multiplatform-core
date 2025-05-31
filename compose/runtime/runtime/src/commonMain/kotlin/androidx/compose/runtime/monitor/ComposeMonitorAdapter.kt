package androidx.compose.runtime.monitor

// region Tencent Code
interface ComposeMonitorAdapter {

    fun onReportDiagnostic(diagnostic: Diagnostic)
}

class DefaultComposeMonitorAdapter : ComposeMonitorAdapter {
    override fun onReportDiagnostic(diagnostic: Diagnostic) {
    }
}
// endregion