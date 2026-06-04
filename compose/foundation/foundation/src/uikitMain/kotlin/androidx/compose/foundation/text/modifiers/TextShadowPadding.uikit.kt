package androidx.compose.foundation.text.modifiers

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.node.CompositionLocalConsumerModifierNode
import androidx.compose.ui.node.currentValueOf
import androidx.compose.ui.uikit.LocalDrawInSkia

/**
 * CompositionLocal to control text shadow padding on iOS.
 *
 * When set to `true`, text rendering will expand the bitmap to accommodate shadow blur,
 * preventing shadow clipping on the bitmap → CALayer pipeline.
 *
 * Default is `false` — no impact on existing behavior.
 *
 * Usage in iosMain:
 * ```
 * CompositionLocalProvider(LocalTextShadowPaddingEnabled provides true) {
 *     Text(text = "...", style = TextStyle(shadow = Shadow(...)))
 * }
 * ```
 */
val LocalTextShadowPaddingEnabled = compositionLocalOf { false }

/**
 * Only enabled when:
 * 1. [LocalTextShadowPaddingEnabled] is explicitly set to true
 * 2. Running in UIView render mode (not Skia) — the bitmap → CALayer pipeline
 *    that causes shadow clipping only exists in UIView mode
 */
internal actual fun CompositionLocalConsumerModifierNode.isTextShadowPaddingEnabled(): Boolean {
    if (!currentValueOf(LocalTextShadowPaddingEnabled)) return false
    // LocalDrawInSkia == true means Skia rendering, no bitmap→CALayer pipeline, no clipping issue
    val drawInSkia = currentValueOf(LocalDrawInSkia)
    return !drawInSkia
}