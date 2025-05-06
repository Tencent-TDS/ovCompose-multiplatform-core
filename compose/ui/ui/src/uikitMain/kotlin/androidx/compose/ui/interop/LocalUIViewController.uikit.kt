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

package androidx.compose.ui.interop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.InternalComposeApi
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.staticCompositionLocalOf
import platform.UIKit.UIViewController

/**
 * public value to get UIViewController of Compose window for library authors.
 * Maybe useful for features, like VideoPlayer and Bottom menus.
 * Please use it careful and don't remove another views.
 */
// region Tencent Code
object LocalUIViewController {
    @OptIn(InternalComposeApi::class)
    inline val current: UIViewController
        @ReadOnlyComposable
        @Composable
        get() = currentComposer.consume(LocalLazyUIViewController).value

    infix fun providesLazy(value: Lazy<UIViewController>) = LocalLazyUIViewController.provides(value)
    infix fun provides(value: UIViewController) = LocalLazyUIViewController.provides(lazy { value })
}

val LocalLazyUIViewController = staticCompositionLocalOf<Lazy<UIViewController>> {
    error("CompositionLocal UIViewController not provided")
}
// endregion

