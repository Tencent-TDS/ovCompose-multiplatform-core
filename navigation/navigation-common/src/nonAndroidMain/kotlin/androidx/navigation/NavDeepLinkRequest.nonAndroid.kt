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
package androidx.navigation

import androidx.annotation.RestrictTo
import kotlin.jvm.JvmStatic

public actual open class NavDeepLinkRequest
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual constructor(uri: NavUri?, action: String?, mimeType: String?) {
    public actual open val uri: NavUri? = implementedInJetBrainsFork()

    public actual open val action: String? = implementedInJetBrainsFork()

    public actual open val mimeType: String? = implementedInJetBrainsFork()

    public actual class Builder private actual constructor() {
        public actual fun setUri(uri: NavUri): Builder {
            implementedInJetBrainsFork()
        }

        public actual fun setAction(action: String): Builder {
            implementedInJetBrainsFork()
        }

        public actual fun setMimeType(mimeType: String): Builder {
            implementedInJetBrainsFork()
        }

        public actual fun build(): NavDeepLinkRequest {
            implementedInJetBrainsFork()
        }

        public actual companion object {
            @JvmStatic
            public actual fun fromUri(uri: NavUri): Builder {
                implementedInJetBrainsFork()
            }

            @JvmStatic
            public actual fun fromAction(action: String): Builder {
                implementedInJetBrainsFork()
            }

            @JvmStatic
            public actual fun fromMimeType(mimeType: String): Builder {
                implementedInJetBrainsFork()
            }
        }
    }
}
