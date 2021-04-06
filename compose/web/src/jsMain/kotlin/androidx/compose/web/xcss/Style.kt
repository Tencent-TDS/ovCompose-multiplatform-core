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

package androidx.compose.web.xcss

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.web.css.CSSRuleBuilder
import androidx.compose.web.css.CSSRuleDeclaration
import androidx.compose.web.css.CSSRuleDeclarationList
import androidx.compose.web.css.StylePropertyList
import androidx.compose.web.css.buildCSSRule
import androidx.compose.web.css.selectors.CSSSelector
import androidx.compose.web.css.selectors.className
import androidx.compose.web.elements.Style
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

interface CSSRulesHolder {
    var cssRules: CSSRuleDeclarationList
}

class CSSRulesHolderState : CSSRulesHolder {
    override var cssRules: CSSRuleDeclarationList by mutableStateOf(listOf())
}

open class StyleSheet(
    private val cssRulesHolder: CSSRulesHolder = CSSRulesHolderState()
) : CSSRulesHolder by cssRulesHolder {

    fun add(selector: CSSSelector, properties: StylePropertyList) {
        cssRules += CSSRuleDeclaration(selector, properties)
    }

    protected fun rule(cssRule: CSSRuleBuilder.() -> Unit) = CSSHolder(cssRule)

    companion object {
        var counter = 0
    }
    // TODO: just proof of concept, do not use it
    fun css(cssRuleBuilder: CSSRuleBuilder.() -> Unit): String {
        val properties = buildCSSRule(cssRuleBuilder)
        val cssRule = cssRules.find {
            it.selector is CSSSelector.CSSClass && it.properties == properties
        }
        return if (cssRule != null) {
            cssRule.selector.unsafeCast<CSSSelector.CSSClass>().className
        } else {
            val classNameSelector = className("auto-${counter++}")
            add(classNameSelector, buildCSSRule(cssRuleBuilder))
            classNameSelector.className
        }
    }

    protected class CSSHolder(val cssRule: CSSRuleBuilder.() -> Unit) {
        operator fun provideDelegate(
            sheet: StyleSheet,
            property: KProperty<*>
        ): ReadOnlyProperty<Any?, String> {
            val sheetName = "${sheet::class.simpleName}-"
            val classNameSelector = className("$sheetName${property.name}")
            sheet.add(classNameSelector, buildCSSRule(cssRule))

            return ReadOnlyProperty { _, _ ->
                classNameSelector.className
            }
        }
    }
}

@Composable
inline fun Style(
    styleSheet: CSSRulesHolder
) {
    Style(cssRules = styleSheet.cssRules)
}
