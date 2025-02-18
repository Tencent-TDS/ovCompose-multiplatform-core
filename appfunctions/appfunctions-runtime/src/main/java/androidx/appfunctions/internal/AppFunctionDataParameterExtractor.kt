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

package androidx.appfunctions.internal

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appfunctions.AppFunctionData
import androidx.appfunctions.AppFunctionInvalidArgumentException
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import androidx.appfunctions.metadata.AppFunctionArrayTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata
import androidx.appfunctions.metadata.AppFunctionObjectTypeMetadata.Companion.TYPE as TYPE_OBJECT
import androidx.appfunctions.metadata.AppFunctionParameterMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BOOLEAN
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_BYTES
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_DOUBLE
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_FLOAT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_INT
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_LONG
import androidx.appfunctions.metadata.AppFunctionPrimitiveTypeMetadata.Companion.TYPE_STRING
import androidx.appfunctions.metadata.AppFunctionReferenceTypeMetadata

/**
 * Gets the parameter value from [AppFunctionData] based on [parameterMetadata].
 *
 * @throws [androidx.appfunctions.AppFunctionInvalidArgumentException] if the parameter in
 *   [AppFunctionData] is not valid according to [parameterMetadata].
 */
// TODO: Update the RequiresApi in AppFunctionData to be T.
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
internal fun AppFunctionData.unsafeGetParameterValue(
    parameterMetadata: AppFunctionParameterMetadata,
): Any? =
    try {
        when (val castDataType = parameterMetadata.dataType) {
            is AppFunctionPrimitiveTypeMetadata -> {
                unsafeGetSingleProperty(parameterMetadata.name, castDataType.type)
            }
            is AppFunctionObjectTypeMetadata -> {
                unsafeGetSingleProperty(parameterMetadata.name, TYPE_OBJECT)
            }
            is AppFunctionArrayTypeMetadata -> {
                getArrayTypeParameterValue(
                    parameterMetadata.name,
                    parameterMetadata.isRequired,
                    castDataType
                )
            }
            is AppFunctionReferenceTypeMetadata -> {
                TODO("Not implement yet - requires reference resolving APIs")
            }
            else ->
                throw IllegalStateException("Unknown DataTypeMetadata: ${castDataType::class.java}")
        }
    } catch (e: NoSuchElementException) {
        if (parameterMetadata.isRequired) {
            Log.d(APP_FUNCTIONS_TAG, "Parameter ${parameterMetadata.name} is required", e)
            throw AppFunctionInvalidArgumentException(
                "Parameter ${parameterMetadata.name} is required"
            )
        }
        null
    } catch (e: IllegalArgumentException) {
        Log.d(
            APP_FUNCTIONS_TAG,
            "Parameter ${parameterMetadata.name} should be the type of ${parameterMetadata.dataType}",
            e
        )
        throw AppFunctionInvalidArgumentException(
            "Parameter ${parameterMetadata.name} should be the type of ${parameterMetadata.dataType}"
        )
    }

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun AppFunctionData.getArrayTypeParameterValue(
    key: String,
    isRequired: Boolean,
    arrayDataTypeMetadata: AppFunctionArrayTypeMetadata
): Any? {
    val itemType = arrayDataTypeMetadata.itemType
    return when (itemType) {
        is AppFunctionPrimitiveTypeMetadata -> {
            val parameter = unsafeGetCollectionProperty(key, itemType.type)
            if (parameter == null && isRequired) {
                throw NoSuchElementException("Parameter $key is required")
            }
            parameter
        }
        is AppFunctionObjectTypeMetadata -> {
            val parameter = unsafeGetCollectionProperty(key, TYPE_OBJECT)
            if (parameter == null && isRequired) {
                throw NoSuchElementException("Parameter $key is required")
            }
            parameter
        }
        is AppFunctionReferenceTypeMetadata -> {
            TODO("Not implement yet - requires reference resolving APIs")
        }
        else ->
            throw IllegalStateException("Unknown item DataTypeMetadata: ${itemType::class.java}")
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun AppFunctionData.unsafeGetSingleProperty(
    key: String,
    type: Int,
): Any? {
    return when (type) {
        TYPE_INT -> {
            TODO("Not implement yet - clarify AppFunctionData#getInt API")
        }
        TYPE_LONG -> {
            getLong(key)
        }
        TYPE_FLOAT -> {
            TODO("Not implement yet - clarify AppFunctionData#getFloat API")
        }
        TYPE_DOUBLE -> {
            getDouble(key)
        }
        TYPE_BOOLEAN -> {
            getBoolean(key)
        }
        TYPE_BYTES -> {
            TODO("Not implement yet - clarify AppFunctionData#getBytes API")
        }
        TYPE_STRING -> {
            getString(key)
        }
        TYPE_OBJECT -> {
            TODO("Not implement yet - require AppFunctionSerializableFactory")
        }
        else -> throw IllegalStateException("Unknown data type $type")
    }
}

@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
private fun AppFunctionData.unsafeGetCollectionProperty(
    key: String,
    type: Int,
): Any? {
    return when (type) {
        TYPE_INT -> {
            TODO("Not implement yet - clarify AppFunctionData#getIntArray API")
        }
        TYPE_LONG -> {
            getLongArray(key)
        }
        TYPE_FLOAT -> {
            TODO("Not implement yet - clarify AppFunctionData#getFloatArray API")
        }
        TYPE_DOUBLE -> {
            getDoubleArray(key)
        }
        TYPE_BOOLEAN -> {
            getBooleanArray(key)
        }
        TYPE_BYTES -> {
            TODO("Not implement yet - clarify AppFunctionData#getBytesArray API")
        }
        TYPE_STRING -> {
            getStringList(key)
        }
        TYPE_OBJECT -> {
            TODO("Not implement yet - require AppFunctionSerializableFactory")
        }
        else -> throw IllegalStateException("Unknown data type $type")
    }
}
