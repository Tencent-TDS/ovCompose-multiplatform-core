package androidx.compose.web

import androidx.compose.runtime.Composable
import org.w3c.dom.css.CSSStyleDeclaration

/**
 * Adds a css style configuration which would be applied when node is updated
 */
internal class CssModifier(val configure: CSSStyleDeclaration.() -> Unit) : MppModifier.Element
internal class InlineStylesModifier(val styles: String) : MppModifier.Element
internal class ClassModifier(val classes: String) : MppModifier.Element
internal class AttributeModifier(val attrName: String, val attrValue: String) : MppModifier.Element
data class CssProperty(val value: String)
internal class CssProperties(val props: Map<CssProperty, String>) : MppModifier.Element

fun MppModifier.attr(attrName: String, attrValue: String): MppModifier =
    this.then(AttributeModifier(attrName, attrValue))

fun MppModifier.css(configure: CSSStyleDeclaration.() -> Unit): MppModifier =
    this.then(CssModifier(configure))

fun MppModifier.cssProps(props: Map<String, String>): MppModifier =
    this.then(
        CssProperties(
            props.mapKeys { prop ->
                CssProperty(prop.key)
            }
        )
    )

fun MppModifier.inlineStyles(styles: String): MppModifier =
    this.then(InlineStylesModifier(styles))

fun MppModifier.classes(classes: String): MppModifier =
    this.then(ClassModifier(classes))

internal class AttributesModifier(
    val configure: MutableMap<String, String>.() -> Unit
) : MppModifier.Element

fun MppModifier.attributes(configure: MutableMap<String, String>.() -> Unit) =
    this.then(AttributesModifier(configure))

@Composable
fun MppModifier.compose(b: @Composable MppModifier.() -> MppModifier): MppModifier {
    return this.then(b())
}

fun check(a: List<String>) {
    if (a.isEmpty()) { }
}
