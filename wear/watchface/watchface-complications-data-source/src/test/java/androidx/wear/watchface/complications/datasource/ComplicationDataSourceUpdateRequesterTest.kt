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
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

/** Tests for [ComplicationDataSourceUpdateRequesterImpl]. */
@RunWith(ComplicationsTestRunner::class)
class ComplicationDataSourceUpdateRequesterImplTest {
    private val context: Context = getApplicationContext()

    private var requester: ComplicationDataSourceUpdateRequester? = null
    private val providerComponent = ComponentName("pkg1", "cls1")
    private var broadcastReceiver: UpdateBroadcastReceiver? = null

    @Before
    fun setup() {
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
    fun tearDown() {
        context.unregisterReceiver(broadcastReceiver)
        broadcastReceiver = null
    }

    @Test
    fun filterRequests_filtersOutNonMatchingComponents() {
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
    fun filterRequests_filtersOutNonMatchingInstanceIds() {
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
    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    fun requestUpdate_sendsBroadcast() {
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
    fun requestUpdateAll_sendsBroadcast() {
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
