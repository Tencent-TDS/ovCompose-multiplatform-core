/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

package androidx.compose.ui.napi

/**
 * 用于持有某个对象的引用，并在不需要时候可以主动调用 [dispose] 函数释放对其的引用
 *
 * 该类设计用于 Kotlin 与鸿蒙 JS 间的交互，由于鸿蒙不能总是及时回收 JS 对象，某些情况下需要主动断开 JS 对 Kotlin 对象的引用
 *
 * @author gavinbaoliu
 * @since 2024/11/25
 */
internal class DisposableRef<out T : Any>(value: T) {

    private var value: T? = value
    private var message: String? = null

    fun get(): T? = value.also {
        if (it == null) {
            androidx.compose.ui.graphics.kLog("$message is disposed!")
        }
    }

    fun dispose() {
        message = value?.toString()
        value = null
        androidx.compose.ui.graphics.kLog("$message is disposed.")
    }
}