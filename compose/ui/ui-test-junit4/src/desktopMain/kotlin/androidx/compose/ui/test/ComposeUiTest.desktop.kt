/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.compose.ui.test

@OptIn(ExperimentalTestApi::class)
typealias DesktopComposeUiTest = SkikoComposeUiTest

/**
 * Variant of [runComposeUiTest] that allows you to specify the size of the surface.
 *
 * @param width the desired width of the surface
 * @param height the desired height of the surface
 */
@ExperimentalTestApi
fun runDesktopComposeUiTest(
    width: Int = 1024,
    height: Int = 768,
    block: DesktopComposeUiTest.() -> Unit
) {
    DesktopComposeUiTest(width, height).runTest(block)
}
