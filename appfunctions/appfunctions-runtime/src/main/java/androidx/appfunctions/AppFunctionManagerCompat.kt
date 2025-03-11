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

package androidx.appfunctions

import android.content.Context
import android.os.Build
import android.os.CancellationSignal
import android.os.OutcomeReceiver
import androidx.annotation.IntDef
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import com.android.extensions.appfunctions.AppFunctionManager
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Provides access to interact with App Functions. This is a backward-compatible wrapper for the
 * platform class [android.app.appfunctions.AppFunctionManager].
 */
@RequiresApi(Build.VERSION_CODES.TIRAMISU)
public class AppFunctionManagerCompat(private val context: Context) {
    private val appFunctionManager: AppFunctionManager by lazy { AppFunctionManager(context) }

    /**
     * Checks whether the AppFunction feature is supported.
     *
     * Support is determined by verifying if the device implements the App Functions extension
     * library
     *
     * @return `true` if the AppFunctions feature is supported on this device, `false` otherwise.
     */
    public fun isSupported(): Boolean {
        // TODO(b/395589225): Check isSupported based on SDK version and update the document.
        return try {
            Class.forName("com.android.extensions.appfunctions.AppFunctionManager")
            true
        } catch (_: ClassNotFoundException) {
            false
        }
    }

    /**
     * Checks if [functionId] in the caller's package is enabled.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.isAppFunctionEnabled].
     *
     * @param functionId The identifier of the app function.
     * @throws UnsupportedOperationException if AppFunction is not supported on this device.
     * @throws IllegalArgumentException If the [functionId] is not available in caller's package.
     */
    public suspend fun isAppFunctionEnabled(functionId: String): Boolean {
        return isAppFunctionEnabled(context.packageName, functionId)
    }

    /**
     * Checks if [functionId] in [packageName] is enabled.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.isAppFunctionEnabled].
     *
     * @param packageName The package name of the owner of [functionId].
     * @param functionId The identifier of the app function.
     * @throws UnsupportedOperationException if AppFunction is not supported on this device.
     * @throws IllegalArgumentException If the [functionId] is not available under [packageName].
     */
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public suspend fun isAppFunctionEnabled(packageName: String, functionId: String): Boolean {
        checkAppFunctionsFeatureSupported()
        return suspendCancellableCoroutine { cont ->
            appFunctionManager.isAppFunctionEnabled(
                functionId,
                packageName,
                Runnable::run,
                object : OutcomeReceiver<Boolean, Exception> {
                    override fun onResult(result: Boolean?) {
                        if (result == null) {
                            cont.resumeWithException(IllegalStateException("Something went wrong"))
                        } else {
                            cont.resume(result)
                        }
                    }

                    override fun onError(error: Exception) {
                        cont.resumeWithException(error)
                    }
                }
            )
        }
    }

    /**
     * Sets [newEnabledState] to an app function [functionId] owned by the calling package.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.setAppFunctionEnabled].
     *
     * @param functionId The identifier of the app function.
     * @param newEnabledState The new state of the app function.
     * @throws UnsupportedOperationException if AppFunction is not supported on this device.
     * @throws IllegalArgumentException If the [functionId] is not available.
     */
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public suspend fun setAppFunctionEnabled(
        functionId: String,
        @EnabledState newEnabledState: Int
    ): Unit {
        checkAppFunctionsFeatureSupported()
        val platformExtensionEnabledState = convertToPlatformExtensionEnabledState(newEnabledState)
        return suspendCancellableCoroutine { cont ->
            appFunctionManager.setAppFunctionEnabled(
                functionId,
                platformExtensionEnabledState,
                Runnable::run,
                object : OutcomeReceiver<Void, Exception> {
                    override fun onResult(result: Void?) {
                        cont.resume(Unit)
                    }

                    override fun onError(error: Exception) {
                        cont.resumeWithException(error)
                    }
                }
            )
        }
    }

    /**
     * Execute the app function.
     *
     * This method matches the platform behavior defined in
     * [android.app.appfunctions.AppFunctionManager.executeAppFunction].
     *
     * @param request the app function details and the arguments.
     * @return the result of the attempt to execute the function.
     * @throws UnsupportedOperationException if AppFunction is not supported on this device.
     */
    @RequiresPermission(value = "android.permission.EXECUTE_APP_FUNCTIONS", conditional = true)
    public suspend fun executeAppFunction(
        request: ExecuteAppFunctionRequest,
    ): ExecuteAppFunctionResponse {
        checkAppFunctionsFeatureSupported()
        return suspendCancellableCoroutine { cont ->
            val cancellationSignal = CancellationSignal()
            cont.invokeOnCancellation { cancellationSignal.cancel() }

            appFunctionManager.executeAppFunction(
                request.toPlatformExtensionClass(),
                Runnable::run,
                cancellationSignal,
                object :
                    OutcomeReceiver<
                        com.android.extensions.appfunctions.ExecuteAppFunctionResponse,
                        com.android.extensions.appfunctions.AppFunctionException
                    > {
                    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
                    override fun onResult(
                        result: com.android.extensions.appfunctions.ExecuteAppFunctionResponse
                    ) {
                        cont.resume(
                            ExecuteAppFunctionResponse.Success.fromPlatformExtensionClass(result)
                        )
                    }

                    override fun onError(
                        error: com.android.extensions.appfunctions.AppFunctionException
                    ) {
                        val exception =
                            fixAppFunctionExceptionErrorType(
                                AppFunctionException.fromPlatformExtensionsClass(error)
                            )
                        cont.resume(ExecuteAppFunctionResponse.Error(exception))
                    }
                }
            )
        }
    }

    private fun fixAppFunctionExceptionErrorType(
        exception: AppFunctionException
    ): AppFunctionException {
        // TODO: Once fixed on the platform side, this behaviour should be done based on the SDK.
        // Platform throws IllegalArgumentException when function not found during function
        // execution and ends up being AppFunctionSystemUnknownException instead of
        // AppFunctionFunctionNotFoundException.
        if (
            exception is AppFunctionSystemUnknownException &&
                exception.errorMessage == "IllegalArgumentException: App function not found."
        ) {
            return AppFunctionFunctionNotFoundException("App function not found.")
        }
        return exception
    }

    private fun checkAppFunctionsFeatureSupported() {
        if (!isSupported()) {
            throw UnsupportedOperationException("AppFunction feature is not supported.")
        }
    }

    private fun convertToPlatformExtensionEnabledState(@EnabledState enabledState: Int): Int {
        return when (enabledState) {
            APP_FUNCTION_STATE_DEFAULT -> AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
            APP_FUNCTION_STATE_ENABLED -> AppFunctionManager.APP_FUNCTION_STATE_ENABLED
            APP_FUNCTION_STATE_DISABLED -> AppFunctionManager.APP_FUNCTION_STATE_DISABLED
            else -> throw IllegalArgumentException("Unknown enabled state $enabledState")
        }
    }

    @IntDef(
        value =
            [APP_FUNCTION_STATE_DEFAULT, APP_FUNCTION_STATE_ENABLED, APP_FUNCTION_STATE_DISABLED]
    )
    @Retention(AnnotationRetention.SOURCE)
    private annotation class EnabledState

    public companion object {
        /**
         * The default state of the app function. Call [setAppFunctionEnabled] with this to reset
         * enabled state to the default value.
         */
        public const val APP_FUNCTION_STATE_DEFAULT: Int =
            AppFunctionManager.APP_FUNCTION_STATE_DEFAULT
        /**
         * The app function is enabled. To enable an app function, call [setAppFunctionEnabled] with
         * this value.
         */
        public const val APP_FUNCTION_STATE_ENABLED: Int =
            AppFunctionManager.APP_FUNCTION_STATE_ENABLED
        /**
         * The app function is disabled. To disable an app function, call [setAppFunctionEnabled]
         * with this value.
         */
        public const val APP_FUNCTION_STATE_DISABLED: Int =
            AppFunctionManager.APP_FUNCTION_STATE_DISABLED
    }
}
