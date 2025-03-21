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

package androidx.appfunctions.compiler.core

import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSTypeReference
import com.google.devtools.ksp.symbol.KSValueParameter

// TODO(b/403525399): Add support for checking optional property.
/** A wrapper class to store the property declaration in a class. */
data class AppFunctionPropertyDeclaration(val name: String, val type: KSTypeReference) {
    /** Creates an [AppFunctionPropertyDeclaration] from [KSPropertyDeclaration]. */
    constructor(
        property: KSPropertyDeclaration
    ) : this(checkNotNull(property.simpleName).asString(), property.type)

    /** Creates an [AppFunctionPropertyDeclaration] from [KSValueParameter]. */
    constructor(
        valueParameter: KSValueParameter
    ) : this(checkNotNull(valueParameter.name).asString(), valueParameter.type)
}
