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

package androidx.navigation.compose.internal

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModelStoreOwner
import kotlinx.coroutines.flow.Flow

internal actual object LocalViewModelStoreOwner {
    actual val current: ViewModelStoreOwner?
        @Composable get() = implementedInJetBrainsFork()
}

internal actual class BackEventCompat {
    actual val touchX: Float = 0f
    actual val touchY: Float = 0f
    actual val progress: Float = 0f
    actual val swipeEdge: Int = -1

    init {
        implementedInJetBrainsFork()
    }
}

@Composable
internal actual fun PredictiveBackHandler(
    enabled: Boolean,
    onBack: suspend (Flow<BackEventCompat>) -> Unit
) {
    implementedInJetBrainsFork()
}
