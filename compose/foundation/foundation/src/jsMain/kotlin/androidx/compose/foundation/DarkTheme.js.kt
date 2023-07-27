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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.mutableStateOf
import kotlinx.browser.window
import org.w3c.dom.MediaQueryList

@Composable
@ReadOnlyComposable
internal actual fun _isSystemInDarkTheme(): Boolean {
    return if (DarkThemeObserver.isSupported)
        DarkThemeObserver.isSystemInDarkTheme.value
    else isSkikoInDarkTheme()
}

private object DarkThemeObserver {

    private val media: MediaQueryList by lazy {
        window.matchMedia("(prefers-color-scheme: dark)")
    }

    val isSystemInDarkTheme = mutableStateOf(
        isSupported && media.matches
    )

    // supported by all browsers since 2015
    // https://developer.mozilla.org/en-US/docs/Web/API/Window/matchMedia
    val isSupported: Boolean
        get() = js("window.matchMedia != undefined").unsafeCast<Boolean>()

    init {
        if (isSupported) {
            media.addListener {
                isSystemInDarkTheme.value = it.unsafeCast<MediaQueryList>().matches
            }
        }
    }
}