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

@file:OptIn(ExperimentalComposeUiApi::class)

package androidx.navigation.compose.internal

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal actual object LocalViewModelStoreOwner {
    actual val current: ViewModelStoreOwner?
        @Composable get() = LocalViewModelStoreOwner.current ?: rememberViewModelStoreOwner()
}

private class ComposeViewModelStoreOwner: ViewModelStoreOwner {
    override val viewModelStore: ViewModelStore = ViewModelStore()
    fun dispose() { viewModelStore.clear() }
}

/**
 * Return remembered [ViewModelStoreOwner] with the scope of current composable.
 *
 * TODO: Consider to move it to `lifecycle-viewmodel-compose` and upstream this to AOSP.
 */
@Composable
private fun rememberViewModelStoreOwner(): ViewModelStoreOwner {
    val viewModelStoreOwner = remember { ComposeViewModelStoreOwner() }
    DisposableEffect(viewModelStoreOwner) {
        onDispose { viewModelStoreOwner.dispose() }
    }
    return viewModelStoreOwner
}

internal actual class BackEventCompat(
    actual val touchX: Float,
    actual val touchY: Float,
    actual val progress: Float,
    actual val swipeEdge: Int
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (progress: Flow<BackEventCompat>) -> Unit
) {
    androidx.compose.ui.backhandler.PredictiveBackHandler(enabled) { progress ->
        onBack(progress.map { BackEventCompat(it.touchX, it.touchY, it.progress, it.swipeEdge) })
    }
}