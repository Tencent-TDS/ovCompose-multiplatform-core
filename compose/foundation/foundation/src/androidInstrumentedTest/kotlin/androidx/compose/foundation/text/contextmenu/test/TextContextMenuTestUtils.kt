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

package androidx.compose.foundation.text.contextmenu.test

import androidx.compose.foundation.internal.checkPreconditionNotNull
import androidx.compose.foundation.text.contextmenu.data.TextContextMenuData
import androidx.compose.foundation.text.contextmenu.modifier.collectTextContextMenuData
import androidx.compose.ui.Modifier
import androidx.compose.ui.node.ModifierNodeElement
import com.google.common.truth.Truth.assertThat

/**
 * Can collect the context menu items wherever its associated
 * [Modifier.testTextContextMenuDataReader][testTextContextMenuDataReader] is positioned.
 */
internal class TestTextContextMenuDataInvoker {
    internal var node: TestTextContextMenuDataReaderNode? = null

    fun invokeTraversal(): TextContextMenuData =
        checkPreconditionNotNull(node).collectTextContextMenuData()
}

/** Wires up the [invoker] to collect the [TextContextMenuData] at this modifier. */
internal fun Modifier.testTextContextMenuDataReader(
    invoker: TestTextContextMenuDataInvoker
): Modifier = this then TestTextContextMenuDataReaderElement(invoker)

private data class TestTextContextMenuDataReaderElement(
    val invoker: TestTextContextMenuDataInvoker,
) : ModifierNodeElement<TestTextContextMenuDataReaderNode>() {
    override fun create(): TestTextContextMenuDataReaderNode =
        TestTextContextMenuDataReaderNode(invoker)

    override fun update(node: TestTextContextMenuDataReaderNode) {
        node.update(invoker)
    }
}

internal class TestTextContextMenuDataReaderNode(
    invoker: TestTextContextMenuDataInvoker,
) : Modifier.Node() {
    var invoker: TestTextContextMenuDataInvoker = invoker
        private set

    override fun onAttach() {
        super.onAttach()
        invoker.node = this
    }

    override fun onDetach() {
        invoker.node = null
        super.onDetach()
    }

    fun update(reader: TestTextContextMenuDataInvoker) {
        this.invoker = reader
    }
}

internal fun TextContextMenuData.assertItems(expectedKeys: List<Any>) {
    assertThat(components.map { it.key }).containsExactly(*expectedKeys.toTypedArray()).inOrder()
}
