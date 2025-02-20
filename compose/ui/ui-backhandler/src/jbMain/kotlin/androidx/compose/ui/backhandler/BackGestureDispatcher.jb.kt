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

package androidx.compose.ui.backhandler

import androidx.compose.ui.ExperimentalComposeUiApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow.SUSPEND
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.Channel.Factory.BUFFERED
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

/**
 * Interface for handling system back gestures in Compose UI.
 *
 * A [BackGestureListener] provides hooks for responding to the beginning, progress, cancellation,
 * and completion of back gesture events. Implementing this interface allows components to handle
 * back gesture events and monitor their state.
 *
 * This interface is typically used in conjunction with the [BackGestureDispatcher],
 * which manages multiple listeners and determines the active listener based on their `enabled`
 * property.
 */
@ExperimentalComposeUiApi
interface BackGestureListener {
    var enabled: Boolean
    var active: Boolean
    fun onStarted()
    fun onProgressed(event: BackEventCompat)
    fun onCancelled()
    fun onCompleted()
}

/**
 * A dispatcher that manages a list of [BackGestureListener] instances and determines which
 * listener is actively handling back gesture events.
 *
 * This class facilitates managing multiple back gesture event listeners, allowing the most
 * recently added and enabled listener to handle events. It provides functionality to add and
 * remove listeners and maintains information about the currently active listener.
 */
@ExperimentalComposeUiApi
open class BackGestureDispatcher {
    private val listeners = mutableListOf<BackGestureListener>()
    protected val activeListener: BackGestureListener?
        get() = listeners.findLast { it.enabled }

    open fun activeListenerChanged() {}

    internal fun addListener(listener: BackGestureListener) {
        if (listeners.contains(listener)) return
        listeners.add(listener)
        activeListenerChanged()
    }

    internal fun removeListener(listener: BackGestureListener) {
        listeners.remove(listener)
        activeListenerChanged()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
internal class BackGestureListenerImpl(
    private val scope: CoroutineScope,
    private val onBack: suspend (progress: Flow<BackEventCompat>) -> Unit,
    private val onReadyStateChanged: (isReady: Boolean) -> Unit
) : BackGestureListener {
    private var channel: Channel<BackEventCompat>? = null
    private var progressJob: Job? = null

    override var enabled = true
        set(value) {
            if (field == value) return
            field = value
            newReadyState()
        }

    override var active = false
        set(value) {
            if (field == value) return
            field = value
            newReadyState()
        }

    private fun newReadyState() {
        val isReady = enabled && active
        if (!isReady) {
            onCancelled()
        }
        onReadyStateChanged(isReady)
    }

    override fun onStarted() {
        channel = Channel<BackEventCompat>(capacity = BUFFERED, onBufferOverflow = SUSPEND).also {
            progressJob = provideProgress(it.consumeAsFlow())
        }
    }

    override fun onProgressed(event: BackEventCompat) {
        channel?.trySend(event)
    }

    override fun onCompleted() {
        channel?.close()
    }

    override fun onCancelled() {
        channel?.cancel(CancellationException("onBack cancelled"))
        channel = null

        progressJob?.cancel()
        progressJob = null
    }

    private fun provideProgress(flow: Flow<BackEventCompat>): Job = scope.launch {
        var completed = false
        onBack(flow.onCompletion { completed = true })
        check(completed) { "You must collect the progress flow" }
    }
}
