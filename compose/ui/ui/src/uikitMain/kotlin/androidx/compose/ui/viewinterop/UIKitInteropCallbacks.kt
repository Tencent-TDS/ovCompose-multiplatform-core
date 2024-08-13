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

package androidx.compose.ui.viewinterop

import kotlinx.cinterop.CValue
import platform.CoreGraphics.CGSize

/**
 * Contains a list of callbacks to be invoked when an interop view transits to specific states.
 */
interface UIKitInteropCallbacks<T> {
    fun onWillAppear(component: T) = Unit
    fun onDidAppear(component: T) = Unit
    fun onWillDisappear(component: T) = Unit
    fun onDidDisappear(component: T) = Unit
    fun onResize(component: T, size: CValue<CGSize>) = Unit
}