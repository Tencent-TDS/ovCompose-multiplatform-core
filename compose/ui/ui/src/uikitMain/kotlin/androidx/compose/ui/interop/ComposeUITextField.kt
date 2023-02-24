package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import kotlinx.cinterop.ObjCAction
import platform.CoreGraphics.CGRectMake
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIControlEventEditingChanged
import platform.UIKit.UITextField

//todo in the future, move to separate library
@Composable
fun ComposeUITextField(modifier: Modifier, value: String, onValueChange: (String) -> Unit) {
    UIKitInteropView(
        background = Color.Green,
        modifier = modifier,
        factory = {
            val textField = object : UITextField(CGRectMake(0.0, 0.0, 0.0, 0.0)) {
                @ObjCAction
                fun editingChanged() {
                    onValueChange(text ?: "")
                }
            }
            textField.addTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
            textField
        },
        update = { textField ->
            textField.text = value
        },
        dispose = { textField ->
            textField.removeTarget(
                target = textField,
                action = NSSelectorFromString(textField::editingChanged.name),
                forControlEvents = UIControlEventEditingChanged
            )
        },
    )
}
