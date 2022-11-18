/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.res

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap

import platform.Foundation.*
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.posix.memcpy
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image

interface ByteSource { //todo move to commonMain
    suspend fun readBytes(): ByteArray //todo in future use streaming
}

interface ResourceProvider<T> { //todo move to commonMain
    suspend fun provide(): T
}

internal class ByteSourceWithKey(val key: Any, val byteSource: ByteSource):ByteSource { // todo move to commonMain
    override suspend fun readBytes(): ByteArray = byteSource.readBytes()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is ByteSourceWithKey) {
            key != other.key
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

}

internal class ResourceProviderWithKey<T>(val key: Any, val provider: ResourceProvider<T>) : ResourceProvider<T> { //todo move to commonMain
    override suspend fun provide(): T = provider.provide()
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is ResourceProviderWithKey<*>) {
            key != other.key
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return key.hashCode()
    }

}

@Composable
fun <T> ResourceProvider<T>.rememberBlocking() = remember(this) { //todo move to commonMain
    runBlocking {
        provide()
    }
}

@Composable
fun <T> ResourceProvider<T>.rememberAsync(): State<T?> { //todo move to commonMain
    val state = remember(this) { mutableStateOf<T?>(null) }
    LaunchedEffect(Unit) {
        state.value = provide()
    }
    return state
}

fun ByteSource.asImageBitmap(): ResourceProvider<ImageBitmap> = ResourceProviderWithKey( //todo move to skikoMain
    key = this,
    object : ResourceProvider<ImageBitmap> {
        override suspend fun provide(): ImageBitmap = Image.makeFromEncoded(readBytes()).toComposeImageBitmap()
    }
)

fun resource(path: String): ByteSource = ByteSourceWithKey( // todo expect / actual
    key =  path,
    object : ByteSource {
        override suspend fun readBytes(): ByteArray {
            val absolutePath = NSBundle.mainBundle.resourcePath + "/" + path
            val contentsAtPath: NSData = NSFileManager.defaultManager().contentsAtPath(absolutePath)!!
            val byteArray = ByteArray(contentsAtPath.length.toInt())
            byteArray.usePinned {
                memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
            }
            return byteArray
        }
    }
)
