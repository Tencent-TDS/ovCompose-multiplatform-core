/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.wear.protolayout.modifiers

import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ModifiersBuilders.Background
import androidx.wear.protolayout.ModifiersBuilders.Clickable
import androidx.wear.protolayout.ModifiersBuilders.Corner
import androidx.wear.protolayout.ModifiersBuilders.ElementMetadata
import androidx.wear.protolayout.ModifiersBuilders.Padding
import androidx.wear.protolayout.ModifiersBuilders.Semantics

/** Creates a [ModifiersBuilders.Modifiers] from a [LayoutModifier]. */
fun LayoutModifier.toProtoLayoutModifiers(): ModifiersBuilders.Modifiers {
    var semantics: Semantics.Builder? = null
    var background: Background.Builder? = null
    var corners: Corner.Builder? = null
    var clickable: Clickable.Builder? = null
    var padding: Padding.Builder? = null
    var metadata: ElementMetadata.Builder? = null

    this.foldIn(Unit) { _, e ->
        when (e) {
            is BaseSemanticElement -> semantics = e.foldIn(semantics)
            is BaseBackgroundElement -> background = e.foldIn(background)
            is BaseCornerElement -> corners = e.foldIn(corners)
            is BaseClickableElement -> clickable = e.foldIn(clickable)
            is BasePaddingElement -> padding = e.foldIn(padding)
            is BaseMetadataElement -> metadata = e.foldIn(metadata)
        }
    }

    corners?.let { background = (background ?: Background.Builder()).setCorner(it.build()) }

    return ModifiersBuilders.Modifiers.Builder()
        .apply {
            semantics?.let { setSemantics(it.build()) }
            background?.let { setBackground(it.build()) }
            clickable?.let { setClickable(it.build()) }
            padding?.let { setPadding(it.build()) }
            metadata?.let { setMetadata(it.build()) }
        }
        .build()
}
