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
    /**
     * [T] was just added to hierarchy and will likely change the frame so that it is not entirely clipped.
     */
    fun onWillAppear(component: T)

    /**
     * [T] has just appeared. It was added to the hierarchy and became visible, or it was
     * in the hierarchy but was clipped before.
     */
    fun onDidAppear(component: T)

    /**
     * [T] is about to be removed from the hierarchy, or it's about to become entirely clipped.
     */
    fun onWillDisappear(component: T)

    /**
     * [T] has just disappeared. It was either detached from the hierarchy or became entirely clipped.
     */
    fun onDidDisappear(component: T)

    /**
     * [T] was just resized to a [size].
     */
    fun onResize(component: T, size: CValue<CGSize>) = Unit
}