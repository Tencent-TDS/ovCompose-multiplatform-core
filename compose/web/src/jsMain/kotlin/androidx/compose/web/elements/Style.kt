/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.compose.web.elements

import androidx.compose.runtime.Composable
import androidx.compose.web.attributes.AttrsBuilder
import androidx.compose.web.attributes.Tag
import androidx.compose.web.css.StyleSheetBuilder
import androidx.compose.web.css.cssRules
import org.w3c.dom.HTMLStyleElement

@Composable
inline fun Style(
    crossinline applyAttrs: AttrsBuilder<Tag.Style>.() -> Unit = {},
    cssRules: StyleSheetBuilder.() -> Unit
) {
    val builder = StyleSheetBuilder()
    builder.cssRules()
    TagElement<Tag.Style, HTMLStyleElement>(
        tagName = "style",
        applyAttrs = {
            cssRules(builder.cssRules)
            applyAttrs()
        },
        applyStyle = {},
        content = {}
    )
}
