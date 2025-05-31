package androidx.compose.ui.window

import androidx.compose.ui.graphics.traceAction
import kotlinx.cinterop.CValue
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGPoint
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGRectMake
import platform.UIKit.UIEvent
import platform.UIKit.UIView

/*
 * Tencent is pleased to support the open source community by making ovCompose available.
 * Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights reserved.
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

internal class RenderingUIViewForUIViewBackend(
    private val component: RenderingComponentForUIView
) : UIView(
    CGRectMake(
        x = 0.0,
        y = 0.0,
        width = 1.0, // TODO: Non-zero size need to first render with ComposeSceneLayer
        height = 1.0
    )
) {
    internal var drawSynchronouslyOnLayout = true

    /**
     * Canvas used in UIViewBackend will not respond to any operation.
     * Set bounds of this view to match with the translation applies to canvas before drawing.
     * See [androidx.compose.ui.scene.RenderingUIViewDelegateImpl.render]
     */
    override fun setFrame(frame: CValue<CGRect>) {
        super.setFrame(frame)
        setBounds(frame.useContents {
            // This will bring the content at (x, y) to the left-top corner of its frame.
            CGRectMake(origin.x, origin.y, size.width, size.height)
        })
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        component.didMoveToWindow()
    }

    override fun layoutSubviews() {
        traceAction("RenderingUIViewForUIViewBackend layoutSubviews") {
            super.layoutSubviews()

            // Render content synchronously to avoid flickering on first layout and size changed.
            if (drawSynchronouslyOnLayout) {
                component.redrawer.drawSynchronously()
                drawSynchronouslyOnLayout = false
            }
        }
    }

    override fun hitTest(point: CValue<CGPoint>, withEvent: UIEvent?): UIView? {
        val view = super.hitTest(point, withEvent)
        return if (view === this) null else view
    }

    override fun canBecomeFirstResponder() = false
}