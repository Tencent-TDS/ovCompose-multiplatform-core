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

package androidx.compose.ui.extention

import androidx.compose.runtime.Composable
import kotlin.jvm.JvmStatic

// region Tencent Code
// 定义代理函数类型
@DelicateComposeApi
typealias GlobalContent = @Composable ((content: @Composable () -> Unit) -> Unit)

/**
 * 代理函数，用于在 Compose 中注册自定义的 Composable 函数
 * 主要用于给所有的入口Composable方法增加各种自定义拦截
 */
@DelicateComposeApi
object GlobalContentScope {

    internal var content: GlobalContent = @Composable { content -> content() }
    // 标记是否已经初始化
    private var isInitialized = false

    /**
     * 初始化全局Composable代理
     * @param content 要设置的Composable代理
     * @throws IllegalStateException 如果已经初始化过
     */
    @DelicateComposeApi
    @JvmStatic
    fun initialize(content: GlobalContent) {
        if (isInitialized) {
            throw IllegalStateException("GlobalComposableRootScope has already been initialized. Initialization can only be performed once.")
        }
        isInitialized = true
        this.content = content
    }
}
// endregion
