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
actual constructor(
    public actual open val uri: NavUri?,
    public actual open val action: String?,
    public actual open val mimeType: String?
) {

    public override fun toString(): String {
        val sb = StringBuilder()
        sb.append("NavDeepLinkRequest")
        sb.append("{")
        if (uri != null) {
            sb.append(" uri=")
            sb.append(uri.toString())
        }
        if (action != null) {
            sb.append(" action=")
            sb.append(action)
        }
        if (mimeType != null) {
            sb.append(" mimetype=")
            sb.append(mimeType)
        }
        sb.append(" }")
        return sb.toString()
    }

    public actual class Builder private actual constructor() {
        private var uri: NavUri? = null
        private var action: String? = null
        private var mimeType: String? = null

        public actual fun setUri(uri: NavUri): Builder {
            this.uri = uri
            return this
        }

        public actual fun setAction(action: String): Builder {
            require(action.isNotEmpty()) { "The NavDeepLinkRequest cannot have an empty action." }
            this.action = action
            return this
        }

        public actual fun setMimeType(mimeType: String): Builder {
            val mimeTypeMatcher = mimeType.matches("^[-\\w*.]+/[-\\w+*.]+$".toRegex())
            require(mimeTypeMatcher) {
                "The given mimeType $mimeType does not match to required \"type/subtype\" format"
            }
            this.mimeType = mimeType
            return this
        }

        public actual fun build(): NavDeepLinkRequest {
            return NavDeepLinkRequest(uri, action, mimeType)
        }

        public actual companion object {
            @JvmStatic
            public actual fun fromUri(uri: NavUri): Builder {
                val builder = Builder()
                builder.setUri(uri)
                return builder
            }

            @JvmStatic
            public actual fun fromAction(action: String): Builder {
                require(action.isNotEmpty()) {
                    "The NavDeepLinkRequest cannot have an empty action."
                }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }

            @JvmStatic
            public actual fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }
}
