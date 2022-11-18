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

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSBundle
import platform.Foundation.NSData
import platform.Foundation.NSFileManager
import platform.posix.memcpy

/**
 * Should implement equals() and hashCode()
 */
interface Resource { //todo move to commonMain
    suspend fun readBytes(): ByteArray //todo in future use streaming
}

fun resource(path: String): Resource = UIKitResourceImpl(path) // todo expect/actual

private class UIKitResourceImpl(val path: String) : Resource {
    override suspend fun readBytes(): ByteArray {
        val absolutePath = NSBundle.mainBundle.resourcePath + "/" + path
        val contentsAtPath: NSData = NSFileManager.defaultManager().contentsAtPath(absolutePath)!!
        val byteArray = ByteArray(contentsAtPath.length.toInt())
        byteArray.usePinned {
            memcpy(it.addressOf(0), contentsAtPath.bytes, contentsAtPath.length)
        }
        return byteArray
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return if (other is UIKitResourceImpl) {
            path != other.path
        } else {
            false
        }
    }

    override fun hashCode(): Int {
        return path.hashCode()
    }
}
