package androidx.compose.foundation.text.modifiers

import androidx.compose.ui.node.CompositionLocalConsumerModifierNode

/**
 * Whether text shadow padding is enabled for this node.
 *
 * On iOS (uikitMain), this reads [LocalTextShadowPaddingEnabled] CompositionLocal,
 * allowing per-component control via CompositionLocalProvider.
 *
 * On all other platforms, this always returns false — shadow padding is not needed
 * because they don't go through the bitmap → CALayer rendering pipeline.
 */
internal actual fun CompositionLocalConsumerModifierNode.isTextShadowPaddingEnabled(): Boolean {
    return false
}