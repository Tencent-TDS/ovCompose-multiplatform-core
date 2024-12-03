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

package androidx.compose.ui.input.pointer

object DummyPointerIcon : PointerIcon

internal data class BrowserCursor(val id: String): PointerIcon

// Extracted from: https://developer.mozilla.org/en-US/docs/Web/CSS/cursor
// Note: cursor existence may vary in different browsers and OS's

internal actual val pointerIconDefault: PointerIcon = BrowserCursor("default")
internal actual val pointerIconCrosshair: PointerIcon = BrowserCursor("crosshair")
internal actual val pointerIconText: PointerIcon = BrowserCursor("text")
internal actual val pointerIconHand: PointerIcon = BrowserCursor("pointer")

val ContextMenu: PointerIcon = BrowserCursor("context-menu")
val Help: PointerIcon = BrowserCursor("help")
val Wait: PointerIcon = BrowserCursor("wait")
val Cell: PointerIcon = BrowserCursor("cell")
val VerticalText: PointerIcon = BrowserCursor("vertical-text")
val Alias: PointerIcon = BrowserCursor("alias")
val Copy: PointerIcon = BrowserCursor("copy")
val Move: PointerIcon = BrowserCursor("move")
val NoDrop: PointerIcon = BrowserCursor("no-drop")
val NotAllowed: PointerIcon = BrowserCursor("not-allowed")
val Grab: PointerIcon = BrowserCursor("grab")
val Grabbing: PointerIcon = BrowserCursor("grabbing")
val AllScroll: PointerIcon = BrowserCursor("all-scroll")
val ColumnResize: PointerIcon = BrowserCursor("col-resize")
val RowResize: PointerIcon = BrowserCursor("row-resize")
val NResize: PointerIcon = BrowserCursor("n-resize")
val EResize: PointerIcon = BrowserCursor("e-resize")
val SResize: PointerIcon = BrowserCursor("s-resize")
val WResize: PointerIcon = BrowserCursor("w-resize")
val NEResize: PointerIcon = BrowserCursor("ne-resize")
val NWResize: PointerIcon = BrowserCursor("nw-resize")
val SEResize: PointerIcon = BrowserCursor("se-resize")
val SwResize: PointerIcon = BrowserCursor("sw-resize")
val EWresize: PointerIcon = BrowserCursor("ew-resize")
val NSResize: PointerIcon = BrowserCursor("ns-resize")
val NESWResize: PointerIcon = BrowserCursor("nesw-resize")
val NWSEResize: PointerIcon = BrowserCursor("nwse-resize")
val ZoomIn: PointerIcon = BrowserCursor("zoom-in")
val ZoomOut: PointerIcon = BrowserCursor("zoom-out")