// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import NativeModalWithNaviationExample
import androidx.compose.runtime.*
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController


fun main() {
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        IosDemo()
    })
}

@Composable
fun IosDemo() {
    var isApplicationLayout by remember { mutableStateOf(true) }
    if (isApplicationLayout) {
        ApplicationLayouts {
            isApplicationLayout = false
        }
    } else {
        MultiplatformDemo()
    }
}

@Composable
private fun MultiplatformDemo() {
    val app = remember {
        App(
            extraScreens = listOf(
                NativeModalWithNaviationExample
            )
        )
    }
    app.Content()
}
