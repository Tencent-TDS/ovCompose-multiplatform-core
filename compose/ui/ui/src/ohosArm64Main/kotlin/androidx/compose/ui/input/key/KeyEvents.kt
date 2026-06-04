package androidx.compose.ui.input.key

import androidx.compose.ui.annotation.InternalComposeApi
import org.jetbrains.skiko.SkikoKeyboardEventKind

/**
 * [KeyEvent] 是否为一个被键入的按键事件；Compose 内部 API
 *
 * @author gavinbaoliu
 * @since 2025/12/5
 */
@InternalComposeApi
val KeyEvent.isTypedEventInternal: Boolean
    get() = nativeKeyEvent.kind == SkikoKeyboardEventKind.DOWN && utf16CodePoint > 0