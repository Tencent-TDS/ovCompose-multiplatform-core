// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.main.defaultUIKitMain
import androidx.compose.ui.window.ComposeUIViewController

fun main() {
    defaultUIKitMain("ComposeDemo", ComposeUIViewController {
        IosDemo()
    })
}

@Composable
fun IosDemo() {
//        val app = remember { App() }
//        app.Content()
    InsetsSample()
}

@Composable
fun InsetsSample() {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier.fillMaxSize()
                .windowInsetsPadding(WindowInsets.Companion.iosSafeArea)
                .background(Color.LightGray)
        )

        Column(Modifier.align(Alignment.Center)) {
            TextField("TextField", {})
            Button({}) {
                Text("Button")
            }
        }
        Text(
            "SafeAreaTop",
            Modifier.align(Alignment.TopCenter)
                .windowInsetsPadding(WindowInsets.Companion.iosSafeArea)
                .background(Color.Yellow)
        )

        Text(
            "IME",
            Modifier.align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.Companion.ime)
                .background(Color.Yellow)
        )

        Text(
            "BOTTOM",
            Modifier.align(Alignment.BottomStart).background(Color.Yellow)
        )
    }
}
