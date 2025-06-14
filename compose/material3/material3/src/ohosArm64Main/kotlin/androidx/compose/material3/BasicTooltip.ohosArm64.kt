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

package androidx.compose.material3

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.PopupPositionProvider

/**
 * NOTICE:
 * Fork from androidx.compose.foundation.BasicTooltip box since those are experimental
 *
 * BasicTooltipBox that wraps a composable with a tooltip.
 *
 * Tooltip that provides a descriptive message for an anchor.
 * It can be used to call the users attention to the anchor.
 *
 * @param positionProvider [PopupPositionProvider] that will be used to place the tooltip
 * relative to the anchor content.
 * @param tooltip the composable that will be used to populate the tooltip's content.
 * @param state handles the state of the tooltip's visibility.
 * @param modifier the [Modifier] to be applied to this BasicTooltipBox.
 * @param focusable [Boolean] that determines if the tooltip is focusable. When true,
 * the tooltip will consume touch events while it's shown and will have accessibility
 * focus move to the first element of the component. When false, the tooltip
 * won't consume touch events while it's shown but assistive-tech users will need
 * to swipe or drag to get to the first element of the component.
 * @param enableUserInput [Boolean] which determines if this BasicTooltipBox will handle
 * long press and mouse hover to trigger the tooltip through the state provided.
 * @param content the composable that the tooltip will anchor to.
 */
@Composable
internal actual fun BasicTooltipBox(
    positionProvider: PopupPositionProvider,
    tooltip: @Composable () -> Unit,
    state: BasicTooltipState,
    modifier: Modifier,
    focusable: Boolean,
    enableUserInput: Boolean,
    content: @Composable () -> Unit
) {
}