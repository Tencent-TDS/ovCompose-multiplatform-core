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

import { BuilderNode, UIContext } from '@kit.ArkUI'
import { FrameNode, LayoutConstraint, NodeContent, Position, RenderNode } from '@ohos.arkui.node'
import { holderBuilder, Holder } from './ArkUIHolder'
import { ArkUINode } from './ArkUINode'
import {
  offsetAxisEventLocalPosition,
  offsetMouseEventLocalPosition,
  offsetTouchEventLocalPosition,
  transformAxisEvent,
  transformMouseEvent,
  transformTouchEvent
} from './TouchEventUtils'
import { deviceInfo } from '@kit.BasicServicesKit'
import ComposeLogger from './logger/ComposeLogger'
import { constraintSize, interopBuilderTable, PostInputEventMinSdkApiVersion, size_equals } from './ArkUIView'
import ComposeExport from './ComposeExport'

let globalComposeNodeId = 0

type ArkUIBuilder = (args: object) => void
type ParameterUpdater = (parameter: object) => void

const TAG = "InteropArkUIView"

export class ArkUIViewArkUIBacked extends FrameNode {
  id: number = globalComposeNodeId++
  name: string
  parameter: Object | undefined
  builder: ArkUIBuilder
  builderNode: BuilderNode<Object[]>
  constraint: LayoutConstraint | null = null
  //region Native Api
  onMeasured?: Function
  composeParameterUpdater: ParameterUpdater | null = null
  arkUIParameterUpdater: ParameterUpdater | null = null
  onRequestDisallowInterceptTouchEvent?: Function

  //endregion

  frameNode: FrameNode
  rootNodeId: number = -1
  extNodeId: number = -1

  constructor(context: UIContext, name: string, parameter: Object) {
    super(context)
    this.name = name
    this.parameter = parameter
    const composable = interopBuilderTable.get(name)
    if (composable) {
      this.builder = composable as ArkUIBuilder
    } else {
      throw new Error(`ArkUI Composable interop builder named: ${name} is not found. Please register it before initializing Compose.`)
    }

    this.setId(`ArkUIView${globalComposeNodeId++}`)

    // WrappedBuilder & wrapBuilder are accessible in ets.
    const builder = wrapBuilder<[Holder]>(holderBuilder)
    const builderNode = new BuilderNode<[Holder]>(context)
    builderNode.build(builder, this)

    this.appendChild(builderNode.getFrameNode())
    this.builderNode = builderNode
    this.frameNode = new FrameNode(context)
    this.frameNode.appendChild(this)
  }

  setId(value: string) {
    return this.commonAttribute.id(value)
  }

  updateArkUIParameter(parameter: Object) {
    this.parameter = parameter
    this.arkUIParameterUpdater?.(parameter)
  }

  updateComposeParameter(parameter: Object) {
    this.parameter = parameter
    this.composeParameterUpdater?.(parameter)
  }

  getPositionToRoot(): Position {
    const frameNodePos: number[] = ComposeExport.mapFrameNodePos(this.rootNodeId, this.extNodeId, 0, 0);
    return { x: 0 - frameNodePos[0], y: 0 - frameNodePos[1] }
  }

  onTouchEvent(e: TouchEvent) {
    const offset = this.getPositionToRoot()
    const event = transformTouchEvent(e, vp2px)
    offsetTouchEventLocalPosition(event, { x: - offset.x, y: -offset.y })
    return this.builderNode.postTouchEvent(event)
  }

  // postMouseEvent enabled since 20
  postMouseEvent(e: MouseEvent): boolean {
    if (deviceInfo.sdkApiVersion < PostInputEventMinSdkApiVersion) {
      return false
    }
    const offset = this.getPositionToRoot()
    const event = transformMouseEvent(e, vp2px)
    offsetMouseEventLocalPosition(event, { x: -vp2px(offset.x), y: -vp2px(offset.y) })
    const consumed = this.builderNode.postInputEvent(event)
    return consumed
  }

  // postAxisEvent enabled since 20
  postAxisEvent(e: AxisEvent): boolean {
    if (deviceInfo.sdkApiVersion < PostInputEventMinSdkApiVersion) {
      return false
    }
    const offset = this.getPositionToRoot()
    const event = transformAxisEvent(e, vp2px)
    offsetAxisEventLocalPosition(event, { x: -vp2px(offset.x), y: -vp2px(offset.y) })
    const consumed = this.builderNode.postInputEvent(event)
    return consumed
  }

  reset() {
    const clipMaxValue = 10000;
    this.constraint = null;
  }

  /**
   * Request parent not to intercept touchEvent, and then we will get the subsequent touch event dispatches in the initial pass.
   * @param disallowIntercept
   */
  requestDisallowInterceptTouchEvent(disallowIntercept: boolean) {
    this.onRequestDisallowInterceptTouchEvent?.(disallowIntercept)
  }


  /**
   * called from Compose measure policy.
   */
  measure(constraint: LayoutConstraint) {
    this.constraint = constraint
    const lastMeasuredSize = this.getMeasuredSize()
    const measuredSize = this.measureChild(constraint)
    if (!size_equals(lastMeasuredSize, measuredSize)) {
      this.setNeedsLayout()
    }
  }

  onMeasure(_constraint: LayoutConstraint): void {
    if (this.constraint && this.getFirstChild()) {
      // Use compose constraint instead.
      const lastMeasuredSize = this.getMeasuredSize()
      const measuredSize = this.measureChild(this.constraint)
      if (!size_equals(lastMeasuredSize, measuredSize)) {
        this.onMeasured?.(measuredSize.width, measuredSize.height)
      }
    }
  }

  private measureChild(constraint: LayoutConstraint): Size {
    const child = this.getFirstChild()!
    child.measure(constraint)
    const measuredSize = child.getMeasuredSize()
    const result = constraintSize(measuredSize, constraint)
    this.setMeasuredSize(result)
    return result
  }

  get measuredWidth() {
    return this.getMeasuredSize().width
  }

  get measuredHeight() {
    return this.getMeasuredSize().height
  }

  getRenderNode(): RenderNode | null {
    return this.frameNode.getRenderNode()
  }

  getFrameNode(): FrameNode {
    return this.frameNode
  }

  disposeView() {
    ComposeLogger.i(TAG, `disposeView named ${this.name}`)
    this.removeChild(this.builderNode.getFrameNode())
    this.builderNode.dispose()
    this.frameNode.dispose()
    this.parameter = undefined
  }
}

@Builder
export function ContentBuilder(rootSlot: NodeContent) {
  Stack() {
    ContentSlot(rootSlot)
  }
  .height('auto')
  .width('auto')
}

export class ArkUINativeView {
  private frameNode: FrameNode
  nodeContent: NodeContent
  private context: UIContext
  private nodeBuilder:BuilderNode<[NodeContent]>

  constructor(context: UIContext) {
    this.context = context
    this.frameNode = new FrameNode(context)
    this.nodeContent = new NodeContent()
    this.nodeBuilder = new BuilderNode(this.context)
    this.nodeBuilder.build(wrapBuilder(ContentBuilder), this.nodeContent)
    this.frameNode.appendChild(this.nodeBuilder.getFrameNode())
  }

  setStopPropagation() {
    this.frameNode.commonAttribute.onTouch(e =>{
      e.stopPropagation()
    })
  }

  getRenderNode(): RenderNode | null {
    return this.frameNode.getRenderNode()
  }

  dispose() {
    this.nodeBuilder.dispose()
    this.frameNode.dispose()
  }
}