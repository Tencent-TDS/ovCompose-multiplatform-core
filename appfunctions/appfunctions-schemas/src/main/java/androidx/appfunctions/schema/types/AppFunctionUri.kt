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

package androidx.appfunctions.schema.types

import android.net.Uri
import androidx.appfunctions.AppFunctionSerializable
import java.util.Objects

/**
 * Immutable URI reference. A URI reference includes a URI and a fragment, the component of the URI
 * following a '#'. Builds and parses URI references which conform to
 * [RFC 2396](http://www.faqs.org/rfcs/rfc2396.html).
 */
@AppFunctionSerializable
public class AppFunctionUri(
    public val value: String,
) {
    /** Constructs a URI from an Android URI. */
    public constructor(uri: Uri) : this(value = uri.toString())

    override fun equals(other: Any?): Boolean =
        this === other || (other is AppFunctionUri && value == other.value)

    override fun hashCode(): Int = Objects.hash(value)

    /** The [Uri]. */
    public val uri: Uri
        get() = Uri.parse(value)
}
