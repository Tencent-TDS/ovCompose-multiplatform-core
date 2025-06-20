/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.compose.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Composer
import androidx.compose.runtime.CompositionLocalMap
import androidx.compose.runtime.Stable
import androidx.compose.ui.modifier.InjectModifier
import androidx.compose.ui.modifier.LocalModifierInjection
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.requireLayoutNode
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.platform.InspectorValueInfo
import androidx.compose.ui.platform.NoInspectorInfo
import kotlin.jvm.JvmName

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
fun Modifier.composed(
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(ComposedModifier(inspectorInfo, factory))

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * When keys are provided, [composed] produces a [Modifier] that will compare [equals] to
 * another modifier constructed with the same keys in order to take advantage of caching and
 * skipping optimizations. [fullyQualifiedName] should be the fully-qualified `import` name for
 * your modifier factory function, e.g. `com.example.myapp.ui.fancyPadding`.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
@ExperimentalComposeUiApi
fun Modifier.composed(
    fullyQualifiedName: String,
    key1: Any?,
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(
    KeyedComposedModifier1(fullyQualifiedName, key1, inspectorInfo, factory)
)

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * When keys are provided, [composed] produces a [Modifier] that will compare [equals] to
 * another modifier constructed with the same keys in order to take advantage of caching and
 * skipping optimizations. [fullyQualifiedName] should be the fully-qualified `import` name for
 * your modifier factory function, e.g. `com.example.myapp.ui.fancyPadding`.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
@ExperimentalComposeUiApi
fun Modifier.composed(
    fullyQualifiedName: String,
    key1: Any?,
    key2: Any?,
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(
    KeyedComposedModifier2(fullyQualifiedName, key1, key2, inspectorInfo, factory)
)

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * When keys are provided, [composed] produces a [Modifier] that will compare [equals] to
 * another modifier constructed with the same keys in order to take advantage of caching and
 * skipping optimizations. [fullyQualifiedName] should be the fully-qualified `import` name for
 * your modifier factory function, e.g. `com.example.myapp.ui.fancyPadding`.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
@ExperimentalComposeUiApi
fun Modifier.composed(
    fullyQualifiedName: String,
    key1: Any?,
    key2: Any?,
    key3: Any?,
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(
    KeyedComposedModifier3(fullyQualifiedName, key1, key2, key3, inspectorInfo, factory)
)

/**
 * Declare a just-in-time composition of a [Modifier] that will be composed for each element it
 * modifies. [composed] may be used to implement **stateful modifiers** that have
 * instance-specific state for each modified element, allowing the same [Modifier] instance to be
 * safely reused for multiple elements while maintaining element-specific state.
 *
 * When keys are provided, [composed] produces a [Modifier] that will compare [equals] to
 * another modifier constructed with the same keys in order to take advantage of caching and
 * skipping optimizations. [fullyQualifiedName] should be the fully-qualified `import` name for
 * your modifier factory function, e.g. `com.example.myapp.ui.fancyPadding`.
 *
 * If [inspectorInfo] is specified this modifier will be visible to tools during development.
 * Specify the name and arguments of the original modifier.
 *
 * Example usage:
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierSample
 * @sample androidx.compose.ui.samples.InspectorInfoInComposedModifierWithArgumentsSample
 *
 * [materialize] must be called to create instance-specific modifiers if you are directly
 * applying a [Modifier] to an element tree node.
 */
@ExperimentalComposeUiApi
fun Modifier.composed(
    fullyQualifiedName: String,
    vararg keys: Any?,
    inspectorInfo: InspectorInfo.() -> Unit = NoInspectorInfo,
    factory: @Composable Modifier.() -> Modifier
): Modifier = this.then(KeyedComposedModifierN(fullyQualifiedName, keys, inspectorInfo, factory))

private open class ComposedModifier(
    inspectorInfo: InspectorInfo.() -> Unit,
    val factory: @Composable Modifier.() -> Modifier
) : Modifier.Element, InspectorValueInfo(inspectorInfo)

@Stable
private class KeyedComposedModifier1(
    val fqName: String,
    val key1: Any?,
    inspectorInfo: InspectorInfo.() -> Unit,
    factory: @Composable Modifier.() -> Modifier
) : ComposedModifier(inspectorInfo, factory) {
    override fun equals(other: Any?) = other is KeyedComposedModifier1 &&
        fqName == other.fqName && key1 == other.key1
    override fun hashCode(): Int = 31 * fqName.hashCode() + key1.hashCode()
}

@Stable
private class KeyedComposedModifier2(
    val fqName: String,
    val key1: Any?,
    val key2: Any?,
    inspectorInfo: InspectorInfo.() -> Unit,
    factory: @Composable Modifier.() -> Modifier
) : ComposedModifier(inspectorInfo, factory) {
    override fun equals(other: Any?) = other is KeyedComposedModifier2 &&
        fqName == other.fqName && key1 == other.key1 && key2 == other.key2

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + key1.hashCode()
        result = 31 * result + key2.hashCode()
        return result
    }
}

@Stable
private class KeyedComposedModifier3(
    val fqName: String,
    val key1: Any?,
    val key2: Any?,
    val key3: Any?,
    inspectorInfo: InspectorInfo.() -> Unit,
    factory: @Composable Modifier.() -> Modifier
) : ComposedModifier(inspectorInfo, factory) {
    override fun equals(other: Any?) = other is KeyedComposedModifier3 &&
        fqName == other.fqName && key1 == other.key1 && key2 == other.key2 && key3 == other.key3

    override fun hashCode(): Int {
        var result = fqName.hashCode()
        result = 31 * result + key1.hashCode()
        result = 31 * result + key2.hashCode()
        result = 31 * result + key3.hashCode()
        return result
    }
}

@Stable
private class KeyedComposedModifierN(
    val fqName: String,
    val keys: Array<out Any?>,
    inspectorInfo: InspectorInfo.() -> Unit,
    factory: @Composable Modifier.() -> Modifier
) : ComposedModifier(inspectorInfo, factory) {
    override fun equals(other: Any?) = other is KeyedComposedModifierN &&
        fqName == other.fqName && keys.contentEquals(other.keys)
    override fun hashCode() = 31 * fqName.hashCode() + keys.contentHashCode()
}

/**
 * Materialize any instance-specific [composed modifiers][composed] for applying to a raw tree node.
 * Call right before setting the returned modifier on an emitted node.
 * You almost certainly do not need to call this function directly.
 */
@Suppress("ModifierFactoryExtensionFunction")
// "materialize" JVM name is taken below to solve a backwards-incompatibility
@JvmName("materializeModifier")
fun Composer.materialize(modifier: Modifier): Modifier {
    // region Tencent Code
    var result = modifier
    val injection = this.currentCompositionLocalMap.get(LocalModifierInjection)
    if (injection != null && result.all { it !== InjectModifier }) {
        result = injection.inject(result).then(InjectModifier)
    }
    // endregion

    // region Tencent Code
    /*
    if (modifier.all { it !is ComposedModifier }) {
        return result
    }
    */
    if (result.all { it !is ComposedModifier }) {
        return result
    }
    // endregion

    // This is a fake composable function that invokes the compose runtime directly so that it
    // can call the element factory functions from the non-@Composable lambda of Modifier.foldIn.
    // It would be more efficient to redefine the Modifier type hierarchy such that the fold
    // operations could be inlined or otherwise made cheaper, which could make this unnecessary.

    // Random number for fake group key. Chosen by fair die roll.
    startReplaceableGroup(0x48ae8da7)
    // region Tencent Code
    /*
    val result = modifier.foldIn<Modifier>(Modifier) { acc, element ->
     */
    result = result.foldIn<Modifier>(Modifier) { acc, element ->
    // endregion
        acc.then(
            if (element is ComposedModifier) {
                @Suppress("UNCHECKED_CAST")
                val factory = element.factory as Modifier.(Composer, Int) -> Modifier
                val composedMod = factory(Modifier, this, 0)
                // region Tencent Code
                /*
                materialize(composedMod)
                 */
                if (injection == null) {
                    materialize(composedMod)
                } else {
                    materialize(composedMod.then(InjectModifier))
                }
                // endregion
            } else {
                element
            }
        )
    }

    endReplaceableGroup()
    return result
}

/**
 * This class is only used for backwards compatibility purposes to inject the CompositionLocalMap
 * into LayoutNodes that were created by inlined code of older versions of the Layout composable.
 * More details can be found at https://issuetracker.google.com/275067189
 */
internal class CompositionLocalMapInjectionNode(map: CompositionLocalMap) : Modifier.Node() {
    var map: CompositionLocalMap = map
        set(value) {
            field = value
            requireLayoutNode().compositionLocalMap = value
        }
    override fun onAttach() {
        requireLayoutNode().compositionLocalMap = map
    }
}

/**
 * This class is only used for backwards compatibility purposes to inject the CompositionLocalMap
 * into LayoutNodes that were created by inlined code of older versions of the Layout composable.
 * More details can be found at https://issuetracker.google.com/275067189
 */
internal class CompositionLocalMapInjectionElement(
    val map: CompositionLocalMap
) : ModifierNodeElement<CompositionLocalMapInjectionNode>() {
    override fun create() = CompositionLocalMapInjectionNode(map)
    override fun update(node: CompositionLocalMapInjectionNode) { node.map = map }
    override fun hashCode(): Int = map.hashCode()
    override fun equals(other: Any?): Boolean {
        return other is CompositionLocalMapInjectionElement && other.map == map
    }
    override fun InspectorInfo.inspectableProperties() {
        name = "<Injected CompositionLocalMap>"
    }
}

/**
 * This function exists solely for solving a backwards-incompatibility with older compilations
 * that used an older version of the `Layout` composable. New code paths should not call this.
 * More details can be found at https://issuetracker.google.com/275067189
 */
@Suppress("ModifierFactoryExtensionFunction")
@JvmName("materialize")
@Deprecated(
    "Kept for backwards compatibility only. If you are recompiling, use materialize.",
    ReplaceWith("materialize"),
    DeprecationLevel.HIDDEN
)
fun Composer.materializeWithCompositionLocalInjection(modifier: Modifier): Modifier =
    materializeWithCompositionLocalInjectionInternal(modifier)

// This method is here to be called from tests since the deprecated hidden API cannot be.
@Suppress("ModifierFactoryExtensionFunction")
internal fun Composer.materializeWithCompositionLocalInjectionInternal(
    modifier: Modifier
): Modifier {
    return if (modifier === Modifier)
        modifier
    else
        materialize(CompositionLocalMapInjectionElement(currentCompositionLocalMap).then(modifier))
}
