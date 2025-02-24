/*
 * Copyright 2021 The Android Open Source Project
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
package androidx.wear.watchface.complications.datasource

import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.os.OutcomeReceiver
import androidx.annotation.RequiresApi
import androidx.annotation.RestrictTo
import androidx.wear.watchface.complications.ComplicationDataSourceUpdateRequesterConstants
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.NoDataComplicationData
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.Companion.ACTION_WEAR_SDK_COMPLICATION_UPDATE_REQUEST
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceService.ComplicationDataRequester
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.Companion.UPDATE_REQUEST_RECEIVER_PACKAGE
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester.Companion.filterRequests
import com.google.wear.Sdk
import com.google.wear.services.complications.ComplicationData as WearSdkComplicationData
import com.google.wear.services.complications.ComplicationsManager
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Allows complication complication data source to request update calls from the system. This
 * effectively allows complication data source to push updates to the system outside of the update
 * request cycle.
 */
public interface ComplicationDataSourceUpdateRequester {
    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationDataSourceService.onComplicationRequest] on the specified
     * complication data source, for all active complications using that complication data source.
     *
     * This will do nothing if no active complications are configured to use the specified
     * complication data source.
     *
     * This will also only work if called from the same package as the complication data source.
     */
    public fun requestUpdateAll()

    /**
     * Requests that the system call
     * [onComplicationUpdate][ComplicationDataSourceService.onComplicationRequest] on the specified
     * complication data source, for the given complication ids. Inactive complications are ignored,
     * as are complications configured to use a different complication data source.
     *
     * @param complicationInstanceIds The system's IDs for the complications to be updated as
     *   provided to [ComplicationDataSourceService.onComplicationActivated] and
     *   [ComplicationDataSourceService.onComplicationRequest].
     */
    public fun requestUpdate(vararg complicationInstanceIds: Int)

    public companion object {
        /** The package of the service that accepts complication data source requests. */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val UPDATE_REQUEST_RECEIVER_PACKAGE = "com.google.android.wearable.app"

        /** An override to [UPDATE_REQUEST_RECEIVER_PACKAGE] for tests. */
        internal var overrideUpdateRequestsReceiverPackage: String? = null

        /**
         * Creates a [ComplicationDataSourceUpdateRequester].
         *
         * @param context The [ComplicationDataSourceService]'s [Context]
         * @param complicationDataSourceComponent The [ComponentName] of the
         *   [ComplicationDataSourceService] to reload.
         * @return The constructed [ComplicationDataSourceUpdateRequester].
         */
        @JvmStatic
        public fun create(
            context: Context,
            complicationDataSourceComponent: ComponentName
        ): ComplicationDataSourceUpdateRequester =
            ComplicationDataSourceUpdateRequesterImpl(context, complicationDataSourceComponent)

        /**
         * Filters [requests] if they aren't keyed by [requesterComponent] or if their
         * `complicationInstanceId` is not present in [instanceIds].
         *
         * If [instanceIds] is empty, only the [requesterComponent] will be checked.
         */
        internal fun filterRequests(
            requesterComponent: ComponentName,
            instanceIds: IntArray,
            requests: List<Pair<ComponentName, ComplicationRequest>>
        ): Set<ComplicationRequest> {
            fun isRequestedInstance(id: Int): Boolean = instanceIds.isEmpty() || id in instanceIds
            return requests
                .filter { (component, request) ->
                    component == requesterComponent &&
                        isRequestedInstance(request.complicationInstanceId)
                }
                .map { it.second }
                .toSet()
        }

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val ACTION_REQUEST_UPDATE_ALL: String =
            "android.support.wearable.complications.ACTION_REQUEST_UPDATE_ALL"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_PROVIDER_COMPONENT: String =
            "android.support.wearable.complications.EXTRA_PROVIDER_COMPONENT"

        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public const val EXTRA_COMPLICATION_IDS: String =
            "android.support.wearable.complications.EXTRA_COMPLICATION_IDS"
    }
}

/**
 * Returns whether the Complication WearSDK APIs used here are available on this device.
 * - Wear SDK is only available on watches.
 * - The Complication APIs are only available on Android U+ devices.
 *
 * If false, we should use the legacy broadcast based system for complication updates.
 */
internal fun shouldUseWearSdk(context: Context) =
    context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) &&
        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)

/**
 * @param context The [ComplicationDataSourceService]'s [Context]
 * @param complicationDataSourceComponent The [ComponentName] of the ComplicationDataSourceService
 *   to reload.
 */
private class ComplicationDataSourceUpdateRequesterImpl(
    private val context: Context,
    private val complicationDataSourceComponent: ComponentName
) : ComplicationDataSourceUpdateRequester {

    private fun updateRequestReceiverPackage() =
        ComplicationDataSourceUpdateRequester.overrideUpdateRequestsReceiverPackage
            ?: UPDATE_REQUEST_RECEIVER_PACKAGE

    override fun requestUpdateAll() {
        if (shouldUseWearSdk(context)) {
            requestUpdate()
            return
        }
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE_ALL)
        intent.setPackage(updateRequestReceiverPackage())
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE)
        )
        context.sendBroadcast(intent)
    }

    override fun requestUpdate(vararg complicationInstanceIds: Int) {
        if (shouldUseWearSdk(context)) {
            Sdk.getWearManager(context, ComplicationsManager::class.java)?.let {
                updateComplicationsUsingWearSdk(complicationInstanceIds, it)
                return
            }
        }
        val intent = Intent(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE)
        intent.setPackage(updateRequestReceiverPackage())
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
            complicationDataSourceComponent
        )
        intent.putExtra(
            ComplicationDataSourceUpdateRequester.EXTRA_COMPLICATION_IDS,
            complicationInstanceIds
        )
        // Add a placeholder PendingIntent to allow the UID to be checked.
        intent.putExtra(
            ComplicationDataSourceUpdateRequesterConstants.EXTRA_PENDING_INTENT,
            PendingIntent.getActivity(context, 0, Intent(""), PendingIntent.FLAG_IMMUTABLE)
        )
        context.sendBroadcast(intent)
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private fun updateComplicationsUsingWearSdk(
        complicationInstanceIds: IntArray,
        complicationsManager: ComplicationsManager
    ) {
        val ioDispatcher = Dispatchers.IO
        CoroutineScope(ioDispatcher).launch {
            val executor = ioDispatcher.asExecutor()
            val updateConfigSet =
                filterRequests(
                    complicationDataSourceComponent,
                    complicationInstanceIds,
                    callWearSdk { outcomeReceiver ->
                            complicationsManager.getActiveComplicationConfigsAsync(
                                executor,
                                outcomeReceiver
                            )
                        }
                        .filter { it.config.providerComponent != null }
                        .map {
                            it.config.providerComponent!! to
                                ComplicationRequest(
                                    it.config.id,
                                    ComplicationType.fromWireType(it.config.dataType),
                                    immediateResponseRequired = false,
                                    isForSafeWatchFace = it.targetWatchFaceSafety
                                )
                        }
                )

            val deferredDataPairs = bindDataSource { dataSource ->
                updateConfigSet.map { request ->
                    asyncSuspendCancellable { continuation ->
                        dataSource.onComplicationRequest(
                            request,
                            WearSdkComplicationRequestListener(request, continuation)
                        )
                    }
                }
            }
            deferredDataPairs.awaitAll().forEach { (id, data) ->
                callWearSdk<Void> { outcomeReceiver ->
                    complicationsManager.updateComplication(id, data, executor, outcomeReceiver)
                }
            }
        }
    }

    private suspend fun <T> bindDataSource(block: suspend (ComplicationDataRequester) -> T): T =
        suspendCancellableCoroutine { continuation ->
                val connection =
                    object : ServiceConnection {
                        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) =
                            continuation.resume(binder as ComplicationDataRequester to this)

                        override fun onServiceDisconnected(name: ComponentName?) {
                            continuation.cancel()
                        }
                    }

                context.bindService(
                    Intent(ACTION_WEAR_SDK_COMPLICATION_UPDATE_REQUEST).apply {
                        component = complicationDataSourceComponent
                    },
                    connection,
                    Context.BIND_AUTO_CREATE
                )

                continuation.invokeOnCancellation { context.unbindService(connection) }
            }
            .let { (binder, connection) ->
                try {
                    block(binder)
                } finally {
                    context.unbindService(connection)
                }
            }

    @RequiresApi(Build.VERSION_CODES.S)
    private suspend fun <T> callWearSdk(block: (OutcomeReceiver<T, Throwable>) -> Unit): T =
        suspendCancellableCoroutine<T> { continuation ->
            block(suspendOutcomeReceiver(continuation))
        }

    @RequiresApi(Build.VERSION_CODES.S)
    private fun <T> suspendOutcomeReceiver(
        continuation: Continuation<T>
    ): OutcomeReceiver<T, Throwable> {
        return object : OutcomeReceiver<T, Throwable> {
            override fun onResult(result: T?) {
                result?.let { continuation.resume(it) }
            }

            override fun onError(error: Throwable) {
                continuation.resumeWithException(error)
            }
        }
    }

    private fun <T> CoroutineScope.asyncSuspendCancellable(
        block: (CancellableContinuation<T>) -> Unit
    ): Deferred<T> = async { suspendCancellableCoroutine(block) }
}

/**
 * An implementation of [ComplicationDataSourceService.ComplicationRequestListener] which validates
 * incoming [ComplicationData] and converts it to
 * [com.google.wear.services.complications.ComplicationData] to resume the provided [Continuation].
 */
@RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
internal class WearSdkComplicationRequestListener
internal constructor(
    val id: Int,
    val dataType: Int,
    val continuation: Continuation<Pair<Int, WearSdkComplicationData>>
) : ComplicationDataSourceService.ComplicationRequestListener {
    internal constructor(
        request: ComplicationRequest,
        continuation: Continuation<Pair<Int, WearSdkComplicationData>>
    ) : this(
        request.complicationInstanceId,
        request.complicationType.toWireComplicationType(),
        continuation
    )

    override fun onComplicationData(complicationData: ComplicationData?) {
        // The result will be null when there is no update required.
        complicationData?.let {
            try {
                validateReceivedData(it)
                continuation.resume(Pair(id, it.asWearSdkComplicationData()))
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    override fun onComplicationDataTimeline(complicationDataTimeline: ComplicationDataTimeline?) {
        // The result will be null when there is no update required.
        complicationDataTimeline?.let {
            try {
                it.validate()
                // This can be run on an arbitrary thread, but that's OK.
                validateReceivedData(it.defaultComplicationData)
                it.timelineEntries.forEach { entry -> validateReceivedData(entry.complicationData) }
                continuation.resume(Pair(id, it.asWearSdkComplicationData()))
            } catch (t: Throwable) {
                continuation.resumeWithException(t)
            }
        }
    }

    private fun validateReceivedData(complicationData: ComplicationData?) {
        complicationData?.validate()
        // This can be run on an arbitrary thread, but that's OK.
        val receivedDataType = complicationData?.type ?: ComplicationType.NO_DATA
        val expectedDataType = ComplicationType.fromWireType(dataType)
        require(
            receivedDataType != ComplicationType.NOT_CONFIGURED &&
                receivedDataType != ComplicationType.EMPTY
        ) {
            "Cannot send data of TYPE_NOT_CONFIGURED or TYPE_EMPTY. Use TYPE_NO_DATA" + " instead."
        }
        require(
            receivedDataType == ComplicationType.NO_DATA || receivedDataType == expectedDataType
        ) {
            "Complication data should match the requested type. Expected " +
                "$expectedDataType got $receivedDataType."
        }
        if (complicationData is NoDataComplicationData) {
            complicationData.placeholder?.let {
                require(it.type == expectedDataType) {
                    "Placeholder type must match the requested type. Expected " +
                        "$expectedDataType got ${it.type}."
                }
            }
        }
    }
}
