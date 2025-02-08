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
import androidx.core.uri.Uri
import kotlin.jvm.JvmStatic

/**
 * A request for a deep link in a [NavDestination].
 *
 * NavDeepLinkRequest are used to check if a [NavDeepLink] exists for a [NavDestination] and to
 * navigate to a [NavDestination] with a matching [NavDeepLink].
 */
public expect open class NavDeepLinkRequest
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
constructor(
    uri: Uri?, action: String?, mimeType: String?,
) {
    /**
     * The uri from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.uriPattern
     */
    public open val uri: Uri?

    /**
     * The action from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.action
     */
    public open val action: String?

    /**
     * The mimeType from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.mimeType
     */
    public open val mimeType: String?

    /** A builder for constructing [NavDeepLinkRequest] instances. */
    public class Builder {

        /**
         * Set the uri for the [NavDeepLinkRequest].
         *
         * @param uri The uri to add to the NavDeepLinkRequest
         * @return This builder.
         */
        public fun setUri(uri: Uri): Builder

        /**
         * Set the action for the [NavDeepLinkRequest].
         *
         * @param action the intent action for the NavDeepLinkRequest
         * @return This builder.
         * @throws IllegalArgumentException if the action is empty.
         */
        public fun setAction(action: String): Builder

        /**
         * Set the mimeType for the [NavDeepLinkRequest].
         *
         * @param mimeType the mimeType for the NavDeepLinkRequest
         * @return This builder.
         * @throws IllegalArgumentException if the given mimeType does not match th3e required
         *   "type/subtype" format.
         */
        public fun setMimeType(mimeType: String): Builder

        /**
         * Build the [NavDeepLinkRequest] specified by this builder.
         *
         * @return the newly constructed NavDeepLinkRequest
         */
        public fun build(): NavDeepLinkRequest

        public companion object {
            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set uri.
             *
             * @param uri The uri to add to the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public fun fromUri(uri: Uri): Builder

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set action.
             *
             * @param action the intent action for the NavDeepLinkRequest
             * @return a [Builder] instance
             * @throws IllegalArgumentException if the action is empty.
             */
            @JvmStatic
            public fun fromAction(action: String): Builder

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public fun fromMimeType(mimeType: String): Builder
        }
    }
}