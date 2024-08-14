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
 * An interface containing methods related to events emitted when an interop view transits to
 * specific states. All the changes are executed synchronously right after the state change on a
 * main thread in sync with a CATransaction batching UIKit objects changes to sync it with Compose
 * rendering.
 */
interface UIKitInteropListener<T> {
    /**
     * [T] was just added to hierarchy and will likely change the frame so that it is not entirely
     * clipped.
     *
     * @param component Component [T] associated with this listener
     */
    fun onWillAppear(component: T) = Unit

    /**
     * [T] has just appeared. It was added to the hierarchy and became visible, or it was
     * in the hierarchy but was clipped before.
     *
     * @param component Component [T] associated with this listener
     */
    fun onDidAppear(component: T) = Unit

    /**
     * [T] is about to be removed from the hierarchy, or it's about to become entirely clipped.
     *
     * @param component Component [T] associated with this listener
     */
    fun onWillDisappear(component: T) = Unit

    /**
     * [T] has just disappeared. It was either detached from the hierarchy or became entirely clipped.
     *
     * @param component Component [T] associated with this listener
     */
    fun onDidDisappear(component: T) = Unit

    /**
     * [T] was just resized to a [size].
     *
     * @param component Component [T] associated with this listener
     * @param size New size of the [component] that was just assigned.
     */
    fun onResize(component: T, size: CValue<CGSize>) = Unit
}