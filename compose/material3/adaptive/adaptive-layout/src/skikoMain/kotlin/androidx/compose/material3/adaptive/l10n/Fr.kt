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

package androidx.compose.material3.adaptive.l10n

import androidx.compose.material3.adaptive.layout.Strings
import androidx.compose.material3.adaptive.layout.Translations

@Suppress("UnusedReceiverParameter", "DuplicatedCode")
internal fun Translations.frCA() = mapOf(
    Strings.defaultPaneExpansionDragHandleContentDescription to "Poignée de déplacement d\'extension du volet",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Volet actuel divisé, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Modifiez la division du volet à %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d pour cent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP à partir du début",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DPs à partir de la fin",
)

@Suppress("UnusedReceiverParameter", "DuplicatedCode")
internal fun Translations.fr() = mapOf(
    Strings.defaultPaneExpansionDragHandleContentDescription to "Poignée de déplacement pour développer les volets",
    Strings.defaultPaneExpansionDragHandleStateDescription to "Répartition actuelle des volets, %s",
    Strings.defaultPaneExpansionDragHandleActionDescription to "Passer la répartition des volets sur %s",
    Strings.defaultPaneExpansionProportionAnchorDescription to "%d pour cent",
    Strings.defaultPaneExpansionStartOffsetAnchorDescription to "%d DP depuis le début",
    Strings.defaultPaneExpansionEndOffsetAnchorDescription to "%d DP depuis la fin",
)
