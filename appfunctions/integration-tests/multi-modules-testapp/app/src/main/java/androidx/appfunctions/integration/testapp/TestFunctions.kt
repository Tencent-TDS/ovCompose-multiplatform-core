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

package androidx.appfunctions.integration.testapp

import androidx.appfunctions.AppFunction
import androidx.appfunctions.AppFunctionContext
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.AppFunctionSerializable

@AppFunctionSerializable
data class CreateNoteParams(
    val title: String? = null,
)

@AppFunctionSerializable
data class Note(
    val title: String,
)

@Suppress("UNUSED_PARAMETER")
class TestFunctions {
    @AppFunction
    fun add(appFunctionContext: AppFunctionContext, num1: Long, num2: Long) = num1 + num2

    @AppFunction
    fun doThrow(appFunctionContext: AppFunctionContext) {
        throw AppFunctionInvalidArgumentException("invalid")
    }

    @AppFunction fun voidFunction(appFunctionContext: AppFunctionContext) {}

    @AppFunction
    fun createNote(
        appFunctionContext: AppFunctionContext,
        createNoteParams: CreateNoteParams
    ): Note {
        return Note(
            title = createNoteParams.title ?: "DEFAULT TITLE",
        )
    }
}

@Suppress("UNUSED_PARAMETER")
class TestFactory {
    private val createdByFactory: Boolean

    constructor() : this(false)

    constructor(createdByFactory: Boolean) {
        this.createdByFactory = createdByFactory
    }

    @AppFunction
    fun isCreatedByFactory(appFunctionContext: AppFunctionContext): Boolean = createdByFactory
}
