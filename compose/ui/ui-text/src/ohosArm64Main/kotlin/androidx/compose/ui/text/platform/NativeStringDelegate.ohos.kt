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

package androidx.compose.ui.text.platform

import androidx.compose.ui.text.PlatformStringDelegate
import androidx.compose.ui.text.intl.PlatformLocale

/**
 * A native implementation of StringDelegate
 */

@Suppress("CAST_NEVER_SUCCEEDS")
internal class OHOSStringDelegate : PlatformStringDelegate {
    override fun toUpperCase(string: String, locale: PlatformLocale): String =
        toUpperCase(string)
    override fun toLowerCase(string: String, locale: PlatformLocale): String =
        toLowerCase(string)
    override fun capitalize(string: String, locale: PlatformLocale): String =
        capitalize(string)
    override fun decapitalize(string: String, locale: PlatformLocale): String =
        decapitalize(string)


    private inline fun toUpperCase(string: String): String =
        string.toUpperCase()
    private inline fun toLowerCase(string: String): String =
        string.toLowerCase()
    private inline fun capitalize(string: String): String =
        string.capitalize()
    private inline fun decapitalize(string: String): String =
        string.decapitalize()
}

// TODO("补API；临时写死)
internal actual fun ActualStringDelegate(): PlatformStringDelegate =
    OHOSStringDelegate()
