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

package androidx.compose.mpp.demo.accessibility

import androidx.compose.mpp.demo.Screen

val AndroidAccessibilityDemos = Screen.Selection(
    "Android Accessibility",
    Screen.Example("Scaffold Top Bar") { ScaffoldSampleDemo() },
    Screen.Example("Scaffold with Scrolling") { ScaffoldSampleScrollDemo() },
    Screen.Example("Simple Top Bar with Scrolling") { ScrollingColumnDemo() },
    Screen.Example("Nested Containers—True") { NestedContainersTrueDemo() },
    Screen.Example("Nested Containers—False") { NestedContainersFalseDemo() },
    Screen.Example("Linear Progress Indicator") { LinearProgressIndicatorDemo() },
    Screen.Example("Dual LTR and RTL Scene") { SimpleRtlLayoutDemo() },
    Screen.Example("Scrolling Tooltip scene") { SampleScrollingTooltipScreen() },

    // Additional demos:
    Screen.Example("Nested Traversal Index Inheritance") { NestedTraversalIndexInheritanceDemo() },
    Screen.Example("Nested and Peer Traversal Index") { NestedAndPeerTraversalIndexDemo() }
)