// Use `xcodegen` first, then `open ./SkikoSample.xcodeproj` and then Run button in XCode.
package androidx.compose.mpp.demo

import androidx.compose.ui.window.Application
import androidx.compose.ui.main.defaultIOSMain

fun main() {
    defaultIOSMain("ComposeDemo", Application("Compose/Native sample") {
        myContent()
    })
}
