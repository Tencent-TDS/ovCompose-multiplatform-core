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
import androidx.compose.runtime.ComposeNode
import androidx.compose.web.DomApplier
import androidx.compose.web.DomNodeWrapper
import androidx.compose.web.attributes.AttrsBuilder
import androidx.compose.web.attributes.InputType
import androidx.compose.web.attributes.Tag
import androidx.compose.web.attributes.action
import androidx.compose.web.attributes.alt
import androidx.compose.web.attributes.href
import androidx.compose.web.attributes.label
import androidx.compose.web.attributes.src
import androidx.compose.web.attributes.type
import androidx.compose.web.attributes.value
import androidx.compose.web.css.StyleBuilder
import kotlinx.browser.document
import org.w3c.dom.HTMLAnchorElement
import org.w3c.dom.HTMLBRElement
import org.w3c.dom.HTMLDivElement
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLFormElement
import org.w3c.dom.HTMLHeadingElement
import org.w3c.dom.HTMLImageElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLLIElement
import org.w3c.dom.HTMLOListElement
import org.w3c.dom.HTMLOptGroupElement
import org.w3c.dom.HTMLOptionElement
import org.w3c.dom.HTMLParagraphElement
import org.w3c.dom.HTMLPreElement
import org.w3c.dom.HTMLSelectElement
import org.w3c.dom.HTMLSpanElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.HTMLUListElement
import org.w3c.dom.Text

@Composable
fun Text(value: String) {
    ComposeNode<DomNodeWrapper, DomApplier>(
        factory = { DomNodeWrapper(document.createTextNode("")) },
        update = {
            set(value) { value -> (node as Text).data = value }
        },
    )
}

@Composable
inline fun Div(
    classes: List<String> = emptyList(),
    crossinline attrs: (AttrsBuilder<Tag.Div>.() -> Unit) = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLDivElement>.() -> Unit = {}
) {
    TagElement(
        classes = classes,
        tagName = "div",
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun A(
    classes: List<String> = emptyList(),
    href: String? = null,
    crossinline attrs: (AttrsBuilder<Tag.A>.() -> Unit) = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLAnchorElement>.() -> Unit = {}
) {
    TagElement<Tag.A, HTMLAnchorElement>(
        classes = classes,
        tagName = "a",
        applyAttrs = {
            href(href)
            attrs()
        },
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Input(
    classes: List<String> = emptyList(),
    type: InputType = InputType.Text,
    value: String = "",
    crossinline attrs: (AttrsBuilder<Tag.Input>.() -> Unit) = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLInputElement>.() -> Unit = {}
) {
    TagElement<Tag.Input, HTMLInputElement>(
        classes = classes,
        tagName = "input",
        applyAttrs = {
            type(type)
            value(value)
            attrs()
        },
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Button(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Button>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "button",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H1(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h1",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H2(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h2",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H3(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h3",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H4(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h4",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H5(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h5",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun H6(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.H>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLHeadingElement>.() -> Unit = {}
) = TagElement(
    tagName = "h6",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun P(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.P>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLParagraphElement>.() -> Unit = {}
) = TagElement(
    tagName = "p",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Em(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "em",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun I(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "i",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun B(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "b",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Small(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "small",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Span(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Span>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLSpanElement>.() -> Unit = {}
) = TagElement(
    tagName = "span",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Br(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Br>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLBRElement>.() -> Unit = {}
) = TagElement(
    tagName = "br",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Ul(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Ul>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLUListElement>.() -> Unit = {}
) = TagElement(
    tagName = "ul",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Ol(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Ol>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLOListElement>.() -> Unit = {}
) = TagElement(
    tagName = "ol",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun DOMScope<HTMLOListElement>.Li(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Li>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLLIElement>.() -> Unit = {}
) = TagElement(
    tagName = "li",
    applyAttrs = attrs,
    classes = classes,
    applyStyle = style,
    content = content
)

@Composable
inline fun DOMScope<HTMLUListElement>.Li(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Li>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLLIElement>.() -> Unit = {}
) = TagElement(
    tagName = "li",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Img(
    classes: List<String> = emptyList(),
    src: String,
    alt: String = "",
    crossinline attrs: AttrsBuilder<Tag.Img>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLImageElement>.() -> Unit = {}
) = TagElement<Tag.Img, HTMLImageElement>(
    tagName = "img",
    classes = classes,
    applyAttrs = {
        src(src).alt(alt)
        attrs()
    },
    applyStyle = style, content = content
)

@Composable
inline fun Form(
    classes: List<String> = emptyList(),
    action: String? = null,
    crossinline attrs: AttrsBuilder<Tag.Form>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLFormElement>.() -> Unit = {}
) = TagElement<Tag.Form, HTMLFormElement>(
    tagName = "form",
    classes = classes,
    applyAttrs = {
        if (!action.isNullOrEmpty()) action(action)
        attrs()
    },
    applyStyle = style, content = content
)

@Composable
inline fun Select(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Select>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLSelectElement>.() -> Unit = {}
) = TagElement(
    tagName = "select",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun DOMScope<HTMLUListElement>.Option(
    classes: List<String> = emptyList(),
    value: String,
    crossinline attrs: AttrsBuilder<Tag.Option>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLOptionElement>.() -> Unit = {}
) = TagElement<Tag.Option, HTMLOptionElement>(
    tagName = "option",
    classes = classes,
    applyAttrs = {
        value(value)
        attrs()
    },
    applyStyle = style,
    content = content
)

@Composable
inline fun OptGroup(
    classes: List<String> = emptyList(),
    label: String,
    crossinline attrs: AttrsBuilder<Tag.OptGroup>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLOptGroupElement>.() -> Unit = {}
) = TagElement<Tag.OptGroup, HTMLOptGroupElement>(
    tagName = "optgroup",
    classes = classes,
    applyAttrs = {
        label(label)
        attrs()
    },
    applyStyle = style,
    content = content
)

@Composable
inline fun Section(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "section",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun TextArea(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.TextArea>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    value: String
) = TagElement<Tag.TextArea, HTMLTextAreaElement>(
    tagName = "textarea",
    classes = classes,
    applyAttrs = {
        value(value)
        attrs()
    },
    applyStyle = style
) {
    Text(value)
}

@Composable
inline fun Nav(
    classes: List<String> = emptyList(),
    crossinline attrs: AttrsBuilder<Tag.Nav>.() -> Unit = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) = TagElement(
    tagName = "nav",
    classes = classes,
    applyAttrs = attrs,
    applyStyle = style,
    content = content
)

@Composable
inline fun Pre(
    classes: List<String> = emptyList(),
    crossinline attrs: (AttrsBuilder<Tag.Pre>.() -> Unit) = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLPreElement>.() -> Unit = {}
) {
    TagElement(
        tagName = "pre",
        classes = classes,
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}

@Composable
inline fun Code(
    classes: List<String> = emptyList(),
    crossinline attrs: (AttrsBuilder<Tag.Code>.() -> Unit) = {},
    crossinline style: (StyleBuilder.() -> Unit) = {},
    content: @Composable ElementScope<HTMLElement>.() -> Unit = {}
) {
    TagElement(
        tagName = "code",
        classes = classes,
        applyAttrs = attrs,
        applyStyle = style,
        content = content
    )
}