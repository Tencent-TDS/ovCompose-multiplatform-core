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

package androidx.compose.ui.text

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.ResolvedTextDirection
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration

/**
 * 样式映射器接口（策略模式）
 * 将Compose样式映射到Native值的逻辑统一管理
 *
 * @param T Compose样式类型
 * @param R Native值类型
 */
interface StyleMapper<T, R> {
    /**
     * 将Compose样式映射到Native值
     *
     * @param value Compose样式值（可为null）
     * @return Native值
     */
    fun map(value: T?): R
}

/**
 * 文本对齐映射器
 *
 * 映射规则：
 * - Left -> 0
 * - Right -> 1
 * - Center -> 2
 * - Justify -> 3
 * - Start -> 4 (默认)
 * - End -> 5
 */
object TextAlignMapper : StyleMapper<TextAlign, Int> {
    override fun map(value: TextAlign?): Int = when (value) {
        TextAlign.Left -> 0
        TextAlign.Right -> 1
        TextAlign.Center -> 2
        TextAlign.Justify -> 3
        TextAlign.Start -> 4
        TextAlign.End -> 5
        null -> 4  // 默认Start
        else -> {
            throw IllegalArgumentException("Unsupported TextAlign: $value")
        }
    }
}

/**
 * 文本方向映射器
 *
 * 映射规则：
 * - RTL -> 0
 * - LTR -> 1
 */
object TextDirectionMapper : StyleMapper<ResolvedTextDirection, Int> {
    override fun map(value: ResolvedTextDirection?): Int = when (value) {
        ResolvedTextDirection.Rtl -> 0
        ResolvedTextDirection.Ltr -> 1
        null -> 1  // 默认LTR
    }
}

/**
 * 字体粗细映射器
 *
 * 映射规则：将0-1000的权重值映射到100-900（步长100）
 *
 * 性能优化：使用查找表而非when表达式
 */
object FontWeightMapper : StyleMapper<FontWeight, Int> {
    // 查找表：[0, 150) -> 100, [150, 250) -> 200, ...
    private val weightRanges = intArrayOf(100, 200, 300, 400, 500, 600, 700, 800, 900)
    private val thresholds = intArrayOf(150, 250, 350, 450, 550, 650, 750, 850)

    override fun map(value: FontWeight?): Int {
        val w = value?.weight ?: 400  // 默认Normal

        // 二分查找优化版本（对于小数组，线性查找可能更快）
        for (i in thresholds.indices) {
            if (w < thresholds[i]) {
                return weightRanges[i]
            }
        }

        return weightRanges.last()
    }
}

/**
 * 字体样式映射器
 *
 * 映射规则：
 * - Normal -> 0
 * - Italic -> 1
 */
object FontStyleMapper : StyleMapper<FontStyle, Int> {
    override fun map(value: FontStyle?): Int = when (value) {
        FontStyle.Italic -> 1
        FontStyle.Normal, null -> 0
        else -> {
            throw IllegalArgumentException("Unsupported FontStyle: $value")
        }
    }
}

object TextDecorationMapper : StyleMapper<TextDecoration, Int> {
    override fun map(value: TextDecoration?): Int {
        var result = 0
        if (value == null) {
            return result
        }
        if (value.contains(TextDecoration.Underline)) {
            result = result or 0x1
        }
        if (value.contains(TextDecoration.LineThrough)) {
            result = result or 0x4
        }
        return result
    }
}

/**
 * 样式映射器注册表（单例模式）
 *
 * 设计目的：
 * 1. 集中管理所有映射器
 * 2. 提供统一的访问点
 * 3. 便于扩展和维护
 */
object StyleMapperRegistry {
    val textAlign: StyleMapper<TextAlign, Int> = TextAlignMapper
    val textDirection: StyleMapper<ResolvedTextDirection, Int> = TextDirectionMapper
    val fontWeight: StyleMapper<FontWeight, Int> = FontWeightMapper
    val fontStyle: StyleMapper<FontStyle, Int> = FontStyleMapper
    val textDecoration: StyleMapper<TextDecoration, Int> = TextDecorationMapper

    /**
     * 验证所有映射器是否正确初始化
     * 可在应用启动时调用
     */
    fun validate(): Boolean {
        return try {
            textAlign.map(null)
            textDirection.map(null)
            fontWeight.map(null)
            fontStyle.map(null)
            true
        } catch (e: Exception) {
            false
        }
    }
}

