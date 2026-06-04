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

package androidx.compose.ui.text.intl


internal class OHLocale : PlatformLocale {
    override val language: String
        get() = "zh_CN"

    override val script: String
        get() = ""

    override val region: String
        get() = "zh-cn"

    override fun toLanguageTag(): String = "zh_CN"
}

// TODO("补API；临时写死)
internal actual fun createPlatformLocaleDelegate(): PlatformLocaleDelegate =
    object : PlatformLocaleDelegate {
        override val current: LocaleList
            get() = LocaleList(listOf(Locale(OHLocale())))

        override fun parseLanguageTag(languageTag: String): PlatformLocale = OHLocale()
    }

// TODO("补API；临时写死)
internal actual fun PlatformLocale.isRtl(): Boolean = false
