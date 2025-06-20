/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.style.LineHeightStyle

private const val DefaultIncludeFontPadding = false

@Suppress("DEPRECATION")
private val DefaultPlatformTextStyle = PlatformTextStyle(
    includeFontPadding = DefaultIncludeFontPadding
)

internal actual fun defaultLineHeightStyleTrim(): LineHeightStyle.Trim = LineHeightStyle.Trim.None

internal actual fun defaultPlatformTextStyle(): PlatformTextStyle? = DefaultPlatformTextStyle
