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
import androidx.navigation.internal.Uri
import kotlin.jvm.JvmStatic

public actual abstract class DeepLinkUri {
    actual abstract fun getFragment(): String?
    actual abstract fun getQuery(): String?
    actual abstract fun getPathSegments(): List<String>
    actual open fun getQueryParameters(key: String): List<String> = error("Abstract implementation")
    actual open fun getQueryParameterNames(): Set<String> = error("Abstract implementation")
}

fun String.toDeepLinkUri(): DeepLinkUri = ActualDeepLinkUri(this)

internal class ActualDeepLinkUri(
    private val uriString: String
) : DeepLinkUri() {

    private companion object {
        private val QUERY_PATTERN = Regex("^[^?#]+\\?([^#]*).*")
        private val FRAGMENT_PATTERN = Regex("#(.+)")
    }

    private val _query: String? by lazy {
        QUERY_PATTERN.find(uriString)?.groups?.get(1)?.value
    }

    private val _fragment: String? by lazy {
        FRAGMENT_PATTERN.find(uriString)?.groups?.get(1)?.value
    }

    private val schemeSeparatorIndex by lazy { uriString.indexOf(':') }

    private val _pathSegments: List<String> by lazy {
        val ssi = schemeSeparatorIndex
        if (ssi > -1) {
            if (ssi + 1 == uriString.length) return@lazy emptyList()
            if (uriString.getOrNull(ssi + 1) != '/') return@lazy emptyList()
        }

        val path = Uri.parsePath(uriString, ssi)

        path.split('/').map { Uri.decode(it) }
    }

    override fun getFragment(): String? = _fragment

    override fun getQuery(): String? = _query

    override fun getPathSegments(): List<String> = _pathSegments

    private fun isHierarchical(): Boolean {
        if (schemeSeparatorIndex == -1) return true // All relative URIs are hierarchical.
        if (uriString.length == schemeSeparatorIndex + 1) return false // No ssp.

        // If the ssp starts with a '/', this is hierarchical.
        return uriString[schemeSeparatorIndex + 1] == '/'
    }

    override fun getQueryParameters(key: String): List<String> {
        require(isHierarchical())
        val query = _query ?: return emptyList()
        val encodedKey = Uri.encode(key)

        return query.split('&').mapNotNull {
            val i = it.indexOf('=')
            when {
                i == -1  -> if (it == encodedKey) "" else null
                it.substring(0, i) == encodedKey -> { Uri.decode(it.substring(i + 1)) }
                else -> null
            }
        }
    }

    override fun getQueryParameterNames(): Set<String> {
        require(isHierarchical())
        val query = _query ?: return emptySet()

        return query.split('&').map {
            val index = it.indexOf('=')
            if (index == -1) return@map it
            else Uri.decode(it.substring(0, index))
        }.toSet()
    }

    override fun toString(): String = uriString
}

internal actual object UriUtils {
    actual fun encode(s: String?, allow: String?): String? = Uri.encode(s!!, allow)
    actual fun decode(s: String?): String? = Uri.decode(s!!)
    actual fun parse(uriString: String?): DeepLinkUri = ActualDeepLinkUri(uriString!!)
}

/**
 * A request for a deep link in a [NavDestination].
 *
 * NavDeepLinkRequest are used to check if a [NavDeepLink] exists for a [NavDestination] and to
 * navigate to a [NavDestination] with a matching [NavDeepLink].
 */
public actual open class NavDeepLinkRequest
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual constructor(
    /**
     * The uri from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.uriPattern
     */
    public actual open val uri: DeepLinkUri?,
    /**
     * The action from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.action
     */
    public actual open val action: String?,
    /**
     * The mimeType from the NavDeepLinkRequest.
     *
     * @see NavDeepLink.mimeType
     */
    public actual open val mimeType: String?,
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

    /** A builder for constructing [NavDeepLinkRequest] instances. */
    public actual class Builder private constructor() {
        private var uri: DeepLinkUri? = null
        private var action: String? = null
        private var mimeType: String? = null

        /**
         * Set the uri for the [NavDeepLinkRequest].
         *
         * @param uri The uri to add to the NavDeepLinkRequest
         * @return This builder.
         */
        public actual fun setUri(uri: DeepLinkUri): Builder {
            this.uri = uri
            return this
        }

        /**
         * Set the action for the [NavDeepLinkRequest].
         *
         * @param action the intent action for the NavDeepLinkRequest
         * @return This builder.
         * @throws IllegalArgumentException if the action is empty.
         */
        public actual fun setAction(action: String): Builder {
            require(action.isNotEmpty()) { "The NavDeepLinkRequest cannot have an empty action." }
            this.action = action
            return this
        }

        /**
         * Set the mimeType for the [NavDeepLinkRequest].
         *
         * @param mimeType the mimeType for the NavDeepLinkRequest
         * @return This builder.
         * @throws IllegalArgumentException if the given mimeType does not match th3e required
         *   "type/subtype" format.
         */
        public actual fun setMimeType(mimeType: String): Builder {
            val mimeTypeMatcher = mimeType.matches("^[-\\w*.]+/[-\\w+*.]+$".toRegex())
            require(mimeTypeMatcher) {
                "The given mimeType $mimeType does not match to required \"type/subtype\" format"
            }
            this.mimeType = mimeType
            return this
        }

        /**
         * Build the [NavDeepLinkRequest] specified by this builder.
         *
         * @return the newly constructed NavDeepLinkRequest
         */
        public actual fun build(): NavDeepLinkRequest {
            return NavDeepLinkRequest(uri, action, mimeType)
        }

        public actual companion object {
            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set uri.
             *
             * @param uri The uri to add to the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public actual fun fromUri(uri: DeepLinkUri): Builder {
                val builder = Builder()
                builder.setUri(uri)
                return builder
            }

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set action.
             *
             * @param action the intent action for the NavDeepLinkRequest
             * @return a [Builder] instance
             * @throws IllegalArgumentException if the action is empty.
             */
            @JvmStatic
            public actual fun fromAction(action: String): Builder {
                require(action.isNotEmpty()) {
                    "The NavDeepLinkRequest cannot have an empty action."
                }
                val builder = Builder()
                builder.setAction(action)
                return builder
            }

            /**
             * Creates a [NavDeepLinkRequest.Builder] with a set mimeType.
             *
             * @param mimeType the mimeType for the NavDeepLinkRequest
             * @return a [Builder] instance
             */
            @JvmStatic
            public actual fun fromMimeType(mimeType: String): Builder {
                val builder = Builder()
                builder.setMimeType(mimeType)
                return builder
            }
        }
    }
}