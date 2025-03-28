package androidx.compose.mpp.demo

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.compose.rememberNavController
import androidx.navigation.bindToNavigation
import org.jetbrains.skiko.wasm.onWasmReady
import kotlinx.browser.window

@OptIn(ExperimentalComposeUiApi::class)
@ExperimentalBrowserHistoryApi
fun main() {
    onWasmReady {
        ComposeViewport(viewportContainerId = "composeApplication") {
            val navController = rememberNavController()

            val app = remember {
                App(
                    extraScreens = listOf(
                        Screen.Example("Web Clipboard API example") {
                            WebClipboardDemo()
                        }
                    )
                )
            }
            app.Content(navController)

            LaunchedEffect(Unit) {
                window.bindToNavigation(navController)
            }
        }
    }
}
