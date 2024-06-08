/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.compose.ui.window

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextField
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.test.Test
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDate
import platform.Foundation.NSRunLoop
import platform.Foundation.dateWithTimeIntervalSinceNow
import platform.Foundation.runUntilDate
import platform.UIKit.UIScreen
import platform.UIKit.UIWindow

@OptIn(ExperimentalForeignApi::class)
class KeyboardInsetsTest {
    @Test
    fun testBottomIMEInsets() {
        var viewAppeared = false

        val window = UIWindow(frame = UIScreen.mainScreen().bounds())
        window.makeKeyAndVisible()

        val controller = ComposeUIViewController {
            MaterialTheme {
                val focusRequester = remember { FocusRequester() }
                val text = remember { mutableStateOf("Hello world") }
                Column(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.ime)
                        .safeDrawingPadding()
                        .onGloballyPositioned { coordinates ->
                            println(">>>> POS ${coordinates.positionInRoot()} | ${coordinates.size}")
                        },
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.weight(1f))
                    TextField(
                        value = text.value,
                        onValueChange = { text.value = it },
                        modifier = Modifier.focusRequester(focusRequester),
                    )
                }

                LaunchedEffect(Unit) {
                    focusRequester.requestFocus()
                }
            }
        }

        window.setRootViewController(controller)

        // class FakeController: UIViewController(nibName = null, bundle = null) {
        //     override fun viewDidAppear(animated: Boolean) {
        //         super.viewDidAppear(animated)
//
        //         println(">>>>> viewDidAppear -- Fake")
        //     }
        // }
//
        // class Controller: UIViewController(nibName = null, bundle = null) {
        //     override fun viewDidLoad() {
        //         super.viewDidLoad()
//
        //         println(">>>>> viewDidLoad - CTR -- ${NSStringFromCGRect(view.frame)}")
        //     }
        //     override fun viewDidAppear(animated: Boolean) {
        //         super.viewDidAppear(animated)
//
        //         println(">>>>> viewDidAppear - CTR -- ${NSStringFromCGRect(view.frame)}")
//
        //         viewAppeared = true
        //     }
        // }
//
        // //val controller = Controller()
        // val navigationController = UINavigationController(rootViewController = FakeController())
        // window.setRootViewController(navigationController)
//
        // println(">>>>> Start")
//
        // dispatch_after(
        //     dispatch_time(DISPATCH_TIME_NOW, (0.toULong() * NSEC_PER_SEC).toLong()),
        //     dispatch_get_main_queue()
        // ) {
        //     navigationController.pushViewController(Controller(), true)
        // }

        // NSNotificationCenter.defaultCenter().postNotification()

        val runLoop = NSRunLoop.currentRunLoop()
        for (i in 0 until 50) {
            println(">>>>> RUN LOOP")
            runLoop.runUntilDate(NSDate.dateWithTimeIntervalSinceNow(0.2))
        }
    }
}