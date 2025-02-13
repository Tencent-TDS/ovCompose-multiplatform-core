/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.compose.foundation.accessibility

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.uikit.LocalUIViewController
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSSelectorFromString
import platform.UIKit.UIAccessibilityIsSwitchControlRunning
import platform.UIKit.UIAccessibilityIsVoiceOverRunning
import platform.UIKit.UIAccessibilitySwitchControlStatusDidChangeNotification
import platform.UIKit.UIAccessibilityVoiceOverStatusDidChangeNotification
import platform.UIKit.UIFocusDidUpdateNotification
import platform.UIKit.UIFocusSystem
import platform.UIKit.focusSystem
import platform.darwin.NSObject

internal object ScreenReaderStatus {
    private val monitor = ScreenReaderStatusMonitor()

    @Composable
    fun isScreenReaderRunning() = monitor.isScreenReaderRunning()
}

private class ScreenReaderStatusMonitor: NSObject() {
    var hasFocusItemUpdate by mutableStateOf(0)
    var isVoiceOverRunning by mutableStateOf(UIAccessibilityIsVoiceOverRunning())
    var isSwitchControlRunning by mutableStateOf(UIAccessibilityIsSwitchControlRunning())

    @Composable
    fun isScreenReaderRunning(): Boolean {
        return isVoiceOverRunning || isSwitchControlRunning || hasFocusItem()
    }

    @Composable
    fun hasFocusItem(): Boolean {
        hasFocusItemUpdate // Read to trigger composition update

        val window = LocalUIViewController.current.view.window
        return (window?.windowScene?.focusSystem?.focusedItem ?: window?.let {
            UIFocusSystem.focusSystemForEnvironment(it)?.focusedItem
        }) != null
    }

    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::updateHasFocusedItem.name),
            name = UIFocusDidUpdateNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::updateVoiceOverRunning.name),
            name = UIAccessibilityVoiceOverStatusDidChangeNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = this,
            selector = NSSelectorFromString(::updateSwitchControlRunning.name),
            name = UIAccessibilitySwitchControlStatusDidChangeNotification,
            `object` = null
        )
        updateHasFocusedItem()
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun updateHasFocusedItem() {
        hasFocusItemUpdate++
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun updateVoiceOverRunning() {
        isVoiceOverRunning = UIAccessibilityIsVoiceOverRunning()
    }

    @OptIn(BetaInteropApi::class)
    @ObjCAction
    fun updateSwitchControlRunning() {
        isSwitchControlRunning = UIAccessibilityIsSwitchControlRunning()
    }
}
