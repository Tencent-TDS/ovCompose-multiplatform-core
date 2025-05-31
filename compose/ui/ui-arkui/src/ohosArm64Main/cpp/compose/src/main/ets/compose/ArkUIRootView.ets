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

import { FrameNode, NodeController, RenderNode, UIContext } from '@kit.ArkUI'
import { ArkUIView } from './ArkUIView'

export class ArkUIRootView extends NodeController {
  frameNode!: FrameNode
  rootNode!: RenderNode
  private context?: UIContext

  makeNode(uiContext: UIContext): FrameNode | null {
    this.context = uiContext
    this.frameNode = new FrameNode(uiContext)
    this.frameNode.commonAttribute.width('100%').height('100%').clip(true)
    return this.frameNode
  }

  buildView(name: string, parameter: Object): ArkUIView | null {
    if (!this.context) {
      return null
    }
    let arkUIView = new ArkUIView(this.context, name, parameter)
    return arkUIView
  }

  insertSubView(arkUIView: ArkUIView, index: number): ArkUIView | null {
    // the arkui view position has not been specified, so translate it off screen to hide it.
    arkUIView.setTranslation(Number.MAX_VALUE, Number.MAX_VALUE)
    const previousSibling = this.frameNode.getChild(index - 1)
    this.frameNode.insertChildAfter(arkUIView, previousSibling)
    return arkUIView
  }

  removeSubView(view: ArkUIView) {
    this.frameNode.removeChild(view)
  }
}