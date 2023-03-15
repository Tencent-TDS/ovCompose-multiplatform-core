/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.platform

import androidx.compose.ui.ComposeScene
import javax.accessibility.Accessible
import javax.accessibility.AccessibleContext

internal class ComposeSceneAccessible(
    private val scene: ComposeScene
) : Accessible {
    private val a11yDisabled by lazy {
        System.getProperty("compose.accessibility.enable") == "false" ||
            System.getenv("COMPOSE_DISABLE_ACCESSIBILITY") != null
    }

    override fun getAccessibleContext(): AccessibleContext? {
        if (a11yDisabled) return null
        val controller = scene.focusedOwner?.accessibilityController as? AccessibilityControllerImpl
        return controller?.rootAccessible?.getAccessibleContext()
    }
}