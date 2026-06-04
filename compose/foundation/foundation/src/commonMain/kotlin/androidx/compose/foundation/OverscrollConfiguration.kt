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

package androidx.compose.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf

// region Tencent Code
/**
 * Composition local for enabling/disabling overscroll effects on Native platforms.
 *
 * Setting this value to `false` will completely disable overscroll effects for all scrollable
 * children in the composition hierarchy. Unlike Android's LocalOverscrollConfiguration which
 * uses a configuration object, this is a simple boolean toggle specifically designed for
 * Native platform implementations.
 *
 * This is not part of standard Compose API and is experimental. Behavior may change in future releases.
 *
 * Example usage:
 * ```
 * // Disable overscroll for a screen section
 * CompositionLocalProvider(LocalOverscrollEnabled provides false) {
 *     ScrollableColumn {
 *         // Content without overscroll effect
 *     }
 * }
 *
 * // Re-enable overscroll for a specific component
 * CompositionLocalProvider(LocalOverscrollEnabled provides true) {
 *     ScrollableColumn {
 *         // Content with overscroll effect restored
 *     }
 * }
 * ```
 *
 * Android equivalent: Set LocalOverscrollConfiguration to `null` to disable effects
 * ```
 * CompositionLocalProvider(LocalOverscrollConfiguration provides null) {
 *     // Scrollable content without overscroll
 * }
 * ```
 */
@ExperimentalFoundationApi
val LocalOverscrollEnabled = compositionLocalOf { true }

/**
 * Composable function that enables overscroll effects for its content.
 *
 * This creates a composition scope where [LocalOverscrollEnabled] is set to `true`,
 * restoring the overscroll effect for all scrollable children. Use this to selectively
 * re-enable overscroll in areas where it was previously disabled.
 *
 * Note: This API is experimental and not part of standard Compose.
 *
 * Example:
 * ```
 * @Composable
 * fun VerticalPagerWithoutOverscroll(
 *     state: PagerState,
 *     content: @Composable () -> Unit
 * ) {
 *     // Disable overscroll for VerticalPager
 *     DisableOverscrollEffect {
 *         VerticalPager(state) {
 *             // But enable overscroll for VerticalPager content
 *             EnableOverscrollEffect {
 *                 content()
 *             }
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalFoundationApi
@Composable
inline fun EnableOverscrollEffect(noinline content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollEnabled provides true, content = content)
}

/**
 * Composable function that disables overscroll effects for its content.
 *
 * This creates a composition scope where [LocalOverscrollEnabled] is set to `false`,
 * disabling any overscroll visual effects for all scrollable children. This is useful
 * for creating areas where scroll boundaries should be strict without visual feedback.
 *
 * Note: This API is experimental and not part of standard Compose.
 *
 * Example:
 * ```
 * @Composable
 * fun VerticalPagerWithoutOverscroll(
 *     state: PagerState,
 *     content: @Composable () -> Unit
 * ) {
 *     // Disable overscroll for VerticalPager
 *     DisableOverscrollEffect {
 *         VerticalPager(state) {
 *             // But enable overscroll for VerticalPager content
 *             EnableOverscrollEffect {
 *                 content()
 *             }
 *         }
 *     }
 * }
 * ```
 */
@ExperimentalFoundationApi
@Composable
inline fun DisableOverscrollEffect(noinline content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalOverscrollEnabled provides false, content = content)
}
// endregion