/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.compose.ui.window

import androidx.compose.ui.util.fastForEachReversed
import platform.UIKit.UIView

/**
 * Stack to remember previously focused UIView.
 */
interface FocusStack {

    /**
     * Add new UIView and focus on it.
     */
    fun push(view: UIView)

    /**
     * Pop all elements until some element. Also pop this element too.
     */
    fun popUntilNext(view: UIView)

    /**
     * Return first added element or null
     */
    fun first(): UIView?
}

internal class FocusStackImpl : FocusStack {

    private var list = emptyList<UIView>()

    override fun push(view: UIView) {
        list += view
        view.becomeFirstResponder()
    }

    override fun popUntilNext(view: UIView) {
        if (list.contains(view)) {
            val index = list.indexOf(view)
            list.subList(index, list.lastIndex).fastForEachReversed {
                it.resignFirstResponder()
            }
            list = list.subList(0, index)
            list.lastOrNull()?.becomeFirstResponder()
        }
    }

    override fun first(): UIView? = list.firstOrNull()

}
