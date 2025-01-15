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

package androidx.camera.core.impl.utils

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyZeroInteractions

@RunWith(JUnit4::class)
class LiveDataUtilTest {
    private lateinit var sourceLiveData: MutableLiveData<Int>
    private lateinit var redirectableLiveData: RedirectableLiveData<Int>

    @Before
    fun setUp() {
        sourceLiveData = MutableLiveData()
        redirectableLiveData = RedirectableLiveData(0)
    }

    @Test
    fun map_returnsMappedValueWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val mappedLiveData = LiveDataUtil.map(sourceLiveData) { it * 2 }

            assertEquals(10, mappedLiveData.value)
        }

    @Test
    fun map_propagatesChangesToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            val mappedLiveData = LiveDataUtil.map(sourceLiveData) { it * 2 }

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            mappedLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(10)
            verify(observer).onChanged(20)
        }

    @Test
    fun redirectableLiveData_initialValue_isReturnedBeforeRedirection() =
        runBlocking(Dispatchers.Main) { assertEquals(0, redirectableLiveData.value) }

    @Test
    fun redirectableLiveData_reflectsSourceValueAfterRedirection() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            assertEquals(5, redirectableLiveData.value)
        }

    @Test
    fun redirectableLiveData_propagatesChangesFromSourceToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(5)
            verify(observer).onChanged(10)
        }

    @Test
    fun redirectableLiveData_withMapping_returnsMappedValueWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectToWithMapping(sourceLiveData) { it * 2 }

            assertEquals(10, redirectableLiveData.value)
        }

    @Test
    fun redirectableLiveData_withMapping_propagatesMappedChangesToObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectToWithMapping(sourceLiveData) { it * 2 }

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 10

            verify(observer).onChanged(10)
            verify(observer).onChanged(20)
        }

    @Test
    fun redirectableLiveData_getValueReflectsLatestSourceValueEvenWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectTo(sourceLiveData)

            assertEquals(5, redirectableLiveData.value)

            sourceLiveData.value = 10

            assertEquals(10, redirectableLiveData.value)
        }

    @Test
    fun redirectableLiveData_getValueReflectsLatestSourceValueWithMappingEvenWithoutObservers() =
        runBlocking(Dispatchers.Main) {
            sourceLiveData.value = 5
            redirectableLiveData.redirectToWithMapping(sourceLiveData) { it * 2 }

            assertEquals(10, redirectableLiveData.value)

            sourceLiveData.value = 10

            assertEquals(20, redirectableLiveData.value)
        }

    @Test
    fun redirectableLiveData_addSource_throwsUnsupportedOperationException() =
        runBlocking(Dispatchers.Main) {
            val otherLiveData = MutableLiveData<String>()
            @Suppress("UNCHECKED_CAST")
            val observer = mock(Observer::class.java) as Observer<String>

            assertThrows(UnsupportedOperationException::class.java) {
                redirectableLiveData.addSource(otherLiveData, observer)
            }

            verifyZeroInteractions(observer)
        }

    @Test
    fun redirectableLiveData_redirectTo_switchToNewSource_correctly() =
        runBlocking(Dispatchers.Main) {
            val sourceLiveData2 = MutableLiveData<Int>()
            sourceLiveData.value = 1
            sourceLiveData2.value = 100
            redirectableLiveData.redirectTo(sourceLiveData)

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 2
            redirectableLiveData.redirectTo(sourceLiveData2)
            sourceLiveData.value = 3 // Should not be observed
            sourceLiveData2.value = 200

            verify(observer).onChanged(1)
            verify(observer).onChanged(2)
            verify(observer).onChanged(100)
            verify(observer).onChanged(200)

            assertEquals(200, redirectableLiveData.value)
        }

    @Test
    fun redirectableLiveData_redirectToWithMapping_switchToNewSourceWithMapping_correctly() =
        runBlocking(Dispatchers.Main) {
            val sourceLiveData2 = MutableLiveData<Int>()
            sourceLiveData.value = 1
            sourceLiveData2.value = 100
            redirectableLiveData.redirectTo(sourceLiveData)

            @Suppress("UNCHECKED_CAST") val observer = mock(Observer::class.java) as Observer<Int>
            redirectableLiveData.observeForever(observer)

            sourceLiveData.value = 2
            redirectableLiveData.redirectToWithMapping(sourceLiveData2) { it + 1000 }
            sourceLiveData.value = 3 // Should not be observed
            sourceLiveData2.value = 200

            verify(observer).onChanged(1)
            verify(observer).onChanged(2)
            verify(observer).onChanged(1100)
            verify(observer).onChanged(1200)

            assertEquals(1200, redirectableLiveData.value)
        }
}
