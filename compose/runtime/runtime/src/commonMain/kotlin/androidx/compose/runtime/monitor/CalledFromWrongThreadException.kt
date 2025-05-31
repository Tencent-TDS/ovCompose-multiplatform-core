package androidx.compose.runtime.monitor

// region Tencent Code
class CalledFromWrongThreadException : RuntimeException {
    constructor()

    constructor(message: String?) : super(message)

    constructor(message: String?, cause: Throwable?) : super(message, cause)

    constructor(cause: Throwable?) : super(cause)
}
// endregion