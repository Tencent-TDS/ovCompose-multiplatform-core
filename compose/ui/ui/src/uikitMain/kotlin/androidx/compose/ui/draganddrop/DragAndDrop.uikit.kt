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

package androidx.compose.ui.draganddrop

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.uikit.density
import androidx.compose.ui.unit.asDpOffset
import androidx.compose.ui.unit.toOffset
import platform.UIKit.UIDragItem
import platform.UIKit.UIDropSessionProtocol
import platform.UIKit.UIView
import androidx.compose.ui.uikit.utils.cmp_itemWithString
import androidx.compose.ui.uikit.utils.cmp_loadString
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import platform.Foundation.NSError

/**
 * A representation of an event sent by the platform during a drag and drop operation.
 */
actual class DragAndDropEvent internal constructor(
    private val dropSessionContext: DropSessionContext
) {
    internal val view: UIView
        get() = dropSessionContext.view

    internal val session: UIDropSessionProtocol
        get() = dropSessionContext.session
}

@ExperimentalComposeUiApi
interface DragAndDropTransferDataEncodingScope {
    fun string(value: String)
}

@ExperimentalComposeUiApi
interface DragAndDropTransferDataItemDecodingScope {
    /**
     * Returns a String if a String was encoded in the current item. Otherwise returns null.
     */
    suspend fun string(): String?
}

/**
 * Perform a decoding in the context of each item contained in the [DragAndDropEvent].
 */
@ExperimentalComposeUiApi
suspend fun DragAndDropEvent.decodeEachItem(block: suspend DragAndDropTransferDataItemDecodingScope.() -> Unit) {
    for (item in session.items) {
        item as UIDragItem
        println("Iterated to item $item")
        object : DragAndDropTransferDataItemDecodingScope {
            override suspend fun string(): String? =
                suspendCoroutine { continuation ->
                    println("Unwrapping item $item")
                    item.cmp_loadString { string, nsError ->
                        if (nsError != null) {
                            println("Failure on $item")
                            continuation.resumeWithException(nsError.asThrowable())
                        } else {
                            println("Success on $item: $string")
                            continuation.resume(string)
                        }
                    }
                }
        }.block()
    }
}

/**
 * On iOS drag and drop session data is represented by [UIDragItem]s, which contains
 * information about how data can be transferred across processes boundaries and an optional
 * local object to be used in the same app.
 */
actual class DragAndDropTransferData internal constructor (
    internal val items: List<UIDragItem>
) {
    @ExperimentalComposeUiApi
    constructor(block: DragAndDropTransferDataEncodingScope.() -> Unit) : this(
        object : DragAndDropTransferDataEncodingScope {
            val items = mutableListOf<UIDragItem>()

            override fun string(value: String) {
                val item = UIDragItem.cmp_itemWithString(value)
                item.localObject = value
                items.add(item)
            }
        }.apply(block).items
    )
}

/**
 * Adapter allowing [NSError] to participate in the Kotlin exception machinery.
 */
internal class ThrowableNSError(val error: NSError): Throwable(error.toString())

internal fun NSError.asThrowable(): Throwable {
    return ThrowableNSError(this)
}

/**
 * Returns the position of this [DragAndDropEvent] relative to the root Compose View in the
 * layout hierarchy.
 */
internal actual val DragAndDropEvent.positionInRoot: Offset
    get() =
        session
            .locationInView(view)
            .asDpOffset()
            .toOffset(view.density)