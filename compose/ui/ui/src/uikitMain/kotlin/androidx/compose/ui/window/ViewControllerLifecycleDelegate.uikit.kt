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

import androidx.compose.ui.platform.IOSLifecycleOwner
import androidx.compose.ui.uikit.utils.CMPViewControllerLifecycleDelegateProtocol
import platform.Foundation.NSNotificationCenter
import platform.darwin.NSObject

internal class ViewControllerLifecycleDelegate(
    private val lifecycleOwner: IOSLifecycleOwner,
    notificationCenter: NSNotificationCenter = NSNotificationCenter.defaultCenter
): NSObject(), CMPViewControllerLifecycleDelegateProtocol {

    private val applicationForegroundStateListener =
        ApplicationForegroundStateListener(notificationCenter) { isForeground ->
            lifecycleOwner.isAppForeground = isForeground
        }

    private val applicationActiveStateListener =
        ApplicationActiveStateListener(notificationCenter) { isActive ->
            lifecycleOwner.isAppActive = isActive
        }

    override fun viewControllerWillDealloc() {
        applicationForegroundStateListener.dispose()
        applicationActiveStateListener.dispose()
        lifecycleOwner.dispose()
    }

    override fun viewControllerWillAppear() {
        lifecycleOwner.isViewAppeared = true
    }

    override fun viewControllerDidDisappear() {
        lifecycleOwner.isViewAppeared = false
    }
}
