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

package elements

import androidx.compose.web.attributes.InputType
import androidx.compose.web.elements.Button
import androidx.compose.web.elements.Input
import androidx.compose.web.elements.TextArea
import org.w3c.dom.HTMLElement
import org.w3c.dom.HTMLInputElement
import org.w3c.dom.HTMLTextAreaElement
import org.w3c.dom.events.Event
import org.w3c.dom.events.InputEvent
import org.w3c.dom.events.MouseEvent
import runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class TableTests {
    @Test
    fun create() = runTest {
        composition {
            Table {
                Caption {
                    Text("CaptionText")
                }
                Colgroup {
                    Col { }
                    Col { }
                    Col(attrs = {
                        span(2)
                    })
                }
                Thead {
                    Tr {
                        Th { }
                        Th { }
                        Th(attrs = {
                            colspan(2)
                        }) {
                            Text("First")
                        }
                    }
                    Tr {
                        Th { }
                        Th { }
                        Th(attrs = { scope(Scope.Col)}) { Text("A") }
                        Th(attrs = { scope(Scope.Col)}) { Text("B") }
                    }
                }
                Tbody {
                    Tr {
                        Th(attrs = {
                            scope(Scope.Row)
                            rowspan(2)
                        }) {
                            Text("Rows")
                        }
                        Th(attrs = {
                            scope(Scope.Row)
                        }) {
                            Text("1")
                        }
                        Td {
                            Text("30")
                        }
                        Td {
                            Text("40")
                        }
                    }
                    Tr {
                        Th(attrs = {
                            scope(Scope.Row)
                        }) {
                            Text("2")
                        }
                        Td {
                            Text("10")
                        }
                        Td {
                            Text("20")
                        }
                    }
                }
                TFoot {
                    Tr {
                        Th(attrs = {
                            scope(Scope.Row)
                        }) {
                            Text("Totals")
                        }
                        Th { }
                        Td { Text("21") }
                        Td { Text("42") }
                    }
                }
            }
        }

        assertEquals(
            expected = """
            <table>
                <caption>CaptionText</caption>
                <colgroup>
                    <col>
                    <col>
                    <col span="2">
                </colgroup>
                <thead>
                    <tr>
                        <th></th>
                        <th></th>
                        <th colspan="2">First</th>
                    </tr>
                    <tr>
                        <th></th>
                        <th></th>
                        <th scope="col">A</th>
                        <th scope="col">B</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <th scope="row" rowspan="2">Rows</th>
                        <th scope="row">1</th>
                        <td>30</td>
                        <td>40</td>
                    </tr>
                    <tr>
                        <th scope="row">2</th>
                        <td>10</td>
                        <td>20</td>
                    </tr>
                </tbody>
                <tfoot>
                    <tr>
                        <th scope="row">Totals</th>
                        <th></th>
                        <td>21</td>
                        <td>42</td>
                    </tr>
                </tfoot>
            </table>
            """.trimIndent(),
            actual = root.innerHTML
        )
    }
}
