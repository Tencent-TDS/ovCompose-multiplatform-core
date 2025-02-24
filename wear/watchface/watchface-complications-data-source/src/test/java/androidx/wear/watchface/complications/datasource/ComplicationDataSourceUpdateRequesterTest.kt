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

package androidx.wear.watchface.complications.datasource

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.EmptyComplicationData
import androidx.wear.watchface.complications.data.LongTextComplicationData
import androidx.wear.watchface.complications.data.NotConfiguredComplicationData
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import com.google.common.truth.Truth.assertThat
import com.google.wear.services.complications.ComplicationData
import java.time.Instant
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import org.junit.After
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLooper

/** Tests for [WearSdkComplicationRequestListenerTest]. */
@RunWith(ComplicationsTestRunner::class)
@Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
public class WearSdkComplicationRequestListenerTest {
    @Test
    fun onComplicationData_emptyDefaultData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(EmptyComplicationData())
        }
    }

    @Test
    fun onComplicationData_notConfiguredData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(NotConfiguredComplicationData())
        }
    }

    @Test
    fun onComplicationData_invalidComplicationData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationData(
                ShortTextComplicationData.Builder(plainText("text"), plainText("description"))
                    .build()
            )
        }
    }

    @Test
    fun onComplicationData_success() {
        assertListenerThrows(RuntimeException::class.java) {
            it.onComplicationData(
                LongTextComplicationData.Builder(plainText("text"), plainText("description"))
                    .build()
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_emptyDefaultData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(EmptyComplicationData(), listOf())
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_notConfiguredData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(NotConfiguredComplicationData(), listOf())
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_invalidComplicationData_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    ShortTextComplicationData.Builder(plainText("text"), plainText("description"))
                        .build(),
                    listOf()
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_notConfiguredComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description")
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            NotConfiguredComplicationData()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_emptyComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description")
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            EmptyComplicationData()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_invalidComplicationDataEntry_exception() {
        assertListenerThrows(IllegalArgumentException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description")
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            ShortTextComplicationData.Builder(
                                    plainText("text"),
                                    plainText("description")
                                )
                                .build()
                        )
                    )
                )
            )
        }
    }

    @Test
    fun onComplicationDataTimeline_success() {
        assertListenerThrows(RuntimeException::class.java) {
            it.onComplicationDataTimeline(
                ComplicationDataTimeline(
                    LongTextComplicationData.Builder(
                            plainText("longText"),
                            plainText("description")
                        )
                        .build(),
                    listOf(
                        TimelineEntry(
                            TimeInterval(Instant.MIN, Instant.MAX),
                            LongTextComplicationData.Builder(
                                    plainText("text"),
                                    plainText("description")
                                )
                                .build()
                        )
                    )
                )
            )
        }
    }

    private fun plainText(text: String) = PlainComplicationText.Builder(text).build()

    private fun <T : Throwable> assertListenerThrows(
        expectedThrowable: Class<T>,
        block: (WearSdkComplicationRequestListener) -> Unit
    ) {
        assertThrows(expectedThrowable) {
            runBlocking {
                suspendCancellableCoroutine<Pair<Int, ComplicationData>> { continuation ->
                    block(
                        WearSdkComplicationRequestListener(
                            1,
                            ComplicationType.LONG_TEXT.toWireComplicationType(),
                            continuation
                        )
                    )
                }
            }
        }
    }
}

/** Tests for [ComplicationDataSourceUpdateRequester]. */
@RunWith(ComplicationsTestRunner::class)
public class ComplicationDataSourceUpdateRequesterTest {
    private val context: Context = getApplicationContext()

    private val providerComponent = ComponentName("pkg1", "cls1")
    private var broadcastReceiver: UpdateBroadcastReceiver? = null

    private var requester: ComplicationDataSourceUpdateRequester? = null

    @Before
    public fun setup() {
        requester = ComplicationDataSourceUpdateRequester.create(context, providerComponent)
        broadcastReceiver = UpdateBroadcastReceiver()
        context.registerReceiver(
            broadcastReceiver,
            IntentFilter().apply {
                addAction(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE)
                addAction(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE_ALL)
            }
        )
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_WATCH, true)
    }

    @After
    public fun tearDown() {
        context.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU, Build.VERSION_CODES.UPSIDE_DOWN_CAKE])
    public fun shouldUseWearSdk_doesNotHaveWatchFeature_returnsFalse() {
        shadowOf(context.packageManager).setSystemFeature(PackageManager.FEATURE_WATCH, false)
        assertThat(shouldUseWearSdk(context)).isFalse()
    }

    @Test
    @Config(maxSdk = Build.VERSION_CODES.TIRAMISU)
    public fun shouldUseWearSdk_androidTOrLower_returnsFalse() {
        assertThat(shouldUseWearSdk(context)).isFalse()
    }

    @Test
    @Config(minSdk = Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    public fun shouldUseWearSdk_androidUOrHigherWithWatchFeature_returnsTrue() {
        assertThat(shouldUseWearSdk(context)).isTrue()
    }

    @Test
    public fun filterRequests_filtersOutNonMatchingComponents() {
        fun fakeRequest(id: Int) = ComplicationRequest(id, ComplicationType.SHORT_TEXT, false)
        val requests =
            ArrayList<Pair<ComponentName, ComplicationRequest>>().apply {
                add(Pair(ComponentName("a", "b"), fakeRequest(1)))
                add(Pair(providerComponent, fakeRequest(3)))
                add(Pair(providerComponent, fakeRequest(4)))
            }
        val expectedResult =
            requests.filter { it.first == providerComponent }.map { it.second }.toSet()

        assertThat(
                ComplicationDataSourceUpdateRequester.filterRequests(
                    providerComponent,
                    arrayOf(1, 3, 4).toIntArray(),
                    requests
                )
            )
            .isEqualTo(expectedResult)
    }

    @Test
    public fun filterRequests_filtersOutNonMatchingInstanceIds() {
        fun fakeRequest(id: Int) = ComplicationRequest(id, ComplicationType.SHORT_TEXT, false)
        val requests =
            ArrayList<Pair<ComponentName, ComplicationRequest>>().apply {
                add(Pair(providerComponent, fakeRequest(2)))
                add(Pair(providerComponent, fakeRequest(3)))
                add(Pair(providerComponent, fakeRequest(4)))
            }
        val expectedResult =
            requests
                .filter { it.second.complicationInstanceId in arrayOf(3, 4) }
                .map { it.second }
                .toSet()

        assertThat(
                ComplicationDataSourceUpdateRequester.filterRequests(
                    providerComponent,
                    arrayOf(3, 4).toIntArray(),
                    requests
                )
            )
            .isEqualTo(expectedResult)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    public fun requestUpdate_androidT_sendsBroadcast() {
        requester?.requestUpdate(0)
        ShadowLooper.idleMainLooper()

        assertThat(broadcastReceiver?.latestIntent).isNotNull()
        assertThat(broadcastReceiver?.latestIntent?.action)
            .isEqualTo(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE)
        var componentName =
            broadcastReceiver
                ?.latestIntent
                ?.getParcelableExtra(
                    ComplicationDataSourceUpdateRequester.EXTRA_PROVIDER_COMPONENT,
                    ComponentName::class.java
                )
        assertThat(componentName).isNotNull()
        assertThat(componentName).isEqualTo(providerComponent)
    }

    @Test
    @Config(sdk = [Build.VERSION_CODES.TIRAMISU])
    public fun requestUpdateAll_androidT_sendsBroadcast() {
        requester?.requestUpdateAll()
        ShadowLooper.idleMainLooper()

        assertThat(broadcastReceiver?.latestIntent).isNotNull()
        assertThat(broadcastReceiver?.latestIntent?.action)
            .isEqualTo(ComplicationDataSourceUpdateRequester.ACTION_REQUEST_UPDATE_ALL)
    }

    private class UpdateBroadcastReceiver() : BroadcastReceiver() {
        var latestIntent: Intent? = null

        override fun onReceive(context: Context?, intent: Intent?) {
            latestIntent = intent
        }
    }
}
