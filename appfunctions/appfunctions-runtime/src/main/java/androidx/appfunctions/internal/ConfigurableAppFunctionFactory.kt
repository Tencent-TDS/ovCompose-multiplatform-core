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

import android.content.Context
import android.util.Log
import androidx.annotation.RestrictTo
import androidx.appfunctions.AppFunctionConfiguration
import androidx.appfunctions.AppFunctionFactory
import androidx.appfunctions.internal.Constants.APP_FUNCTIONS_TAG
import java.lang.reflect.InvocationTargetException

/**
 * An [AppFunctionFactory] implementation that will incorporate [AppFunctionConfiguration] from
 * [context] to create AppFunction enclosing classes.
 *
 * If the application context from [context] overrides [AppFunctionConfiguration.Provider], the
 * customize [AppFunctionFactory] will be used to instantiate the enclosing class. Otherwise, it
 * will use reflection to create the instance assuming the enclosing class has no argument
 * constructor.
 *
 * [createEnclosingClass] will throw [AppFunctionInstantiationException] if unable to instantiate
 * the enclosing class.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class ConfigurableAppFunctionFactory<T : Any>(
    private val context: Context,
) : AppFunctionFactory<T> {

    override fun createEnclosingClass(enclosingClass: Class<T>): T {
        val configurationProvider = context.applicationContext as? AppFunctionConfiguration.Provider
        val customFactory =
            configurationProvider?.appFunctionConfiguration?.factories?.get(enclosingClass)
        if (customFactory == null) {
            Log.d(APP_FUNCTIONS_TAG, "Unable to find custom factory for [$enclosingClass]")
            return getNoArgumentAppFunctionFactory<T>().createEnclosingClass(enclosingClass)
        }

        @Suppress("UNCHECKED_CAST")
        return (customFactory as AppFunctionFactory<T>).createEnclosingClass(enclosingClass)
    }

    /** Thrown when unable to instantiate the AppFunction enclosing class. */
    public class AppFunctionInstantiationException(errorMessage: String) :
        RuntimeException(errorMessage)

    private fun <T : Any> getNoArgumentAppFunctionFactory(): AppFunctionFactory<T> {
        return object : AppFunctionFactory<T> {
            override fun createEnclosingClass(enclosingClass: Class<T>): T {
                return try {
                    enclosingClass.getDeclaredConstructor().newInstance()
                } catch (_: IllegalAccessException) {
                    throw AppFunctionInstantiationException(
                        "Cannot access the constructor of $enclosingClass"
                    )
                } catch (_: NoSuchMethodException) {
                    throw AppFunctionInstantiationException(
                        "$enclosingClass requires additional parameter to create. " +
                            "Please either remove the additional parameters or implement the " +
                            "${AppFunctionFactory::class.qualifiedName} and provide it in " +
                            "${AppFunctionConfiguration::class.qualifiedName}",
                    )
                } catch (_: InstantiationException) {
                    throw AppFunctionInstantiationException(
                        "$enclosingClass should have a public no-argument constructor"
                    )
                } catch (_: InvocationTargetException) {
                    throw AppFunctionInstantiationException(
                        "Something went wrong when creating $enclosingClass"
                    )
                } catch (_: ExceptionInInitializerError) {
                    throw AppFunctionInstantiationException(
                        "Something went wrong when creating $enclosingClass"
                    )
                }
            }
        }
    }
}
