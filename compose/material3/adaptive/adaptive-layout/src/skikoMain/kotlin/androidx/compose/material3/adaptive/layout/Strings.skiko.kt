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

package androidx.compose.material3.adaptive.layout

import androidx.compose.material3.adaptive.l10n.en
import androidx.compose.material3.adaptive.l10n.translationFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.text.intl.Locale
import kotlin.jvm.JvmInline

@JvmInline
@Immutable
internal actual value class Strings(val value: Int) {
    actual companion object {
        actual val defaultPaneExpansionDragHandleContentDescription
            get() = Strings(0)

        actual val defaultPaneExpansionDragHandleStateDescription
            get() = Strings(0)

        actual val defaultPaneExpansionDragHandleActionDescription
            get() = Strings(0)

        actual val defaultPaneExpansionProportionAnchorDescription
            get() = Strings(0)

        actual val defaultPaneExpansionStartOffsetAnchorDescription
            get() = Strings(0)

        actual val defaultPaneExpansionEndOffsetAnchorDescription
            get() = Strings(0)
    }
}

// TODO check if we should replace it by a more performant implementation
//  (without creating intermediate strings)
// TODO current implementation doesn't support sophisticated formatting like %.2f,
//  but currently we use it only for integers and strings
internal fun String.format(vararg formatArgs: Any?): String {
    var result = this
    formatArgs.forEachIndexed { index, arg ->
        result = result
            .replace("%${index + 1}\$d", arg.toString())
            .replace("%${index + 1}\$s", arg.toString())
    }
    return result
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings): String {
    val locale = Locale.current
    val tag = localeTag(language = locale.language, region = locale.region)
    val translation = translationByLocaleTag.getOrPut(tag) {
        findTranslation(locale)
    }
    return translation[string] ?: error("Missing translation for $string")
}

@Composable
@ReadOnlyComposable
internal actual fun getString(string: Strings, vararg formatArgs: Any): String {
    return getString(string).format(*formatArgs)
}

/**
 * A single translation; should contain all the [Strings].
 */
internal typealias Translation = Map<Strings, String>

/**
 * Translations we've already loaded, mapped by the locale tag (see [localeTag]).
 */
private val translationByLocaleTag = mutableMapOf<String, Translation>()

/**
 * Returns the tag for the given locale.
 *
 * Note that this is our internal format; this isn't the same as [Locale.toLanguageTag].
 */
private fun localeTag(language: String, region: String) = when {
    language == "" -> ""
    region == "" -> language
    else -> "${language}_$region"
}

/**
 * Returns a sequence of locale tags to use as keys to look up the translation for the given locale.
 *
 * Note that we don't need to check children (e.g. use `fr_FR` if `fr` is missing) because the
 * translations should never have a missing parent.
 */
private fun localeTagChain(locale: Locale) = sequence {
    if (locale.region != "") {
        yield(localeTag(language = locale.language, region = locale.region))
    }
    if (locale.language != "") {
        yield(localeTag(language = locale.language, region = ""))
    }
    yield(localeTag("", ""))
}

/**
 * Finds a [Translation] for the given locale.
 */
private fun findTranslation(locale: Locale): Map<Strings, String> {
    // We don't need to merge translations because each one should contain all the strings.
    return localeTagChain(locale).firstNotNullOf { translationFor(it) }
}

/**
 * This object is only needed to provide a namespace for the [Translation] provider functions
 * (e.g. [Translations.en]), to avoid polluting the global namespace.
 */
internal object Translations
