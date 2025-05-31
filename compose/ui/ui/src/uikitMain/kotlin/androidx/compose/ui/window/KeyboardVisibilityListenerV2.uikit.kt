package androidx.compose.ui.window

import androidx.compose.runtime.MutableState
import androidx.compose.ui.scene.ComposeSceneFocusManager
import androidx.compose.ui.scene.ComposeSceneMediator
import androidx.compose.ui.uikit.ComposeConfiguration
import androidx.compose.ui.uikit.OnFocusBehavior
import androidx.compose.ui.uikit.utils.OVComposeKeyboardVisibilityListener
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.toDpRect
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIView

private class NativeKeyboardVisibilityListenerWrapper(
    val outListener:KeyboardVisibilityListenerImplV2
) : OVComposeKeyboardVisibilityListener() {

    override fun keyboardWillShow() {
        outListener.keyboardWillShow()
    }

    override fun keyboardWillHide() {
        outListener.keyboardWillHide()
    }

    override fun keyboardOverlapHeightChanged(keyboardHeight: Float) {
        outListener.keyboardHeightChanged(keyboardHeight)
    }

}

internal class KeyboardVisibilityListenerImplV2(
    // region Tencent Code
    private val configuration: ComposeConfiguration,
    // endregion
    private val keyboardOverlapHeightState: MutableState<Float>,
    private val viewProvider: () -> UIView,
    private val densityProvider: () -> Density,
    private val composeSceneMediatorProvider: () -> ComposeSceneMediator,
    private val focusManager: ComposeSceneFocusManager,
) {
    private val nativeWrapper = NativeKeyboardVisibilityListenerWrapper(this).apply {
        bindComposeView(viewProvider())
    }

    fun keyboardWillShow() {
        if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            val focusedRect = focusManager.getFocusRect()?.toDpRect(densityProvider())

            if (focusedRect != null) {
                val mediator = composeSceneMediatorProvider()
                updateViewBounds(
                    offsetY = calcFocusedLiftingY(mediator, focusedRect, nativeWrapper.keyboardHeight)
                )
            }
        }
    }

    fun keyboardHeightChanged(keyboardHeight: Float) {
        keyboardOverlapHeightState.value = keyboardHeight
    }

    fun keyboardWillHide() {
        if (configuration.onFocusBehavior == OnFocusBehavior.FocusableAboveKeyboard) {
            updateViewBounds(offsetY = 0.0)
        }
    }

    fun dispose() {
        nativeWrapper.dispose()
    }

    fun prepare() {
        nativeWrapper.prepareIfNeeded()
    }

    private fun calcFocusedLiftingY(
        composeSceneMediator: ComposeSceneMediator,
        focusedRect: DpRect,
        keyboardHeight: Double
    ): Double {
        val viewHeight = composeSceneMediator.getViewHeight()
        val hiddenPartOfFocusedElement: Double =
            keyboardHeight - viewHeight + focusedRect.bottom.value
        return if (hiddenPartOfFocusedElement > 0) {
            // If focused element is partially hidden by the keyboard, we need to lift it upper
            val focusedTopY = focusedRect.top.value
            val isFocusedElementRemainsVisible = hiddenPartOfFocusedElement < focusedTopY
            if (isFocusedElementRemainsVisible) {
                // We need to lift focused element to be fully visible
                hiddenPartOfFocusedElement
            } else {
                // In this case focused element height is bigger than remain part of the screen after showing the keyboard.
                // Top edge of focused element should be visible. Same logic on Android.
                maxOf(focusedTopY, 0f).toDouble()
            }
        } else {
            // Focused element is not hidden by the keyboard.
            0.0
        }
    }

    private fun updateViewBounds(offsetX: Double = 0.0, offsetY: Double = 0.0) {
        val view = viewProvider()
        view.layer.setBounds(
            view.frame.useContents {
                CGRectMake(
                    x = offsetX,
                    y = offsetY,
                    width = size.width,
                    height = size.height
                )
            }
        )
    }
}