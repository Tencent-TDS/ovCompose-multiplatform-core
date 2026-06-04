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

package androidx.compose.runtime.inspection

import androidx.compose.runtime.Composer
import androidx.compose.runtime.RecomposeScope

// region Tencent Code
interface RecompositionHandler {

    fun change(old: Any?, new: Any?, composer: Composer) {

    }

    fun skipping(skip: Boolean, composer: Composer) {

    }

    fun invalidate(scope: RecomposeScope, instance: Any?) {

    }

    fun changedInstance(old: Any?, new: Any?, composer: Composer) {

    }

    fun composeStart(instances: Set<Any>?, scope: RecomposeScope) {

    }

    fun startRestartGroupStart(key: Int, composer: Composer) {

    }

    fun startRestartGroupEnd(key: Int, composer: Composer) {

    }

    fun skipToGroupEnd(composer: Composer) {

    }

    fun endRestartGroup(composer: Composer) {

    }

    fun composeEnd(scope: RecomposeScope) {

    }

    fun release() {

    }

}

// endregion