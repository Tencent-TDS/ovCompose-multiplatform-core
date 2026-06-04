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
import { LayoutConstraint } from '@ohos.arkui.node'
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

export const interopBuilderTable = new Map<string, Function>()

export function registerComposeInteropBuilder(key: string, builder: Function) {
  interopBuilderTable.set(key, builder)
}

let globalComposeNodeId = 0

type ArkUIBuilder = (args: object) => void
type ParameterUpdater = (parameter: object) => void

const TAG = "InteropArkUIView"

export class ArkUIView extends ArkUINode {
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
  }

  updateArkUIParameter(parameter: Object) {
    this.parameter = parameter
    this.arkUIParameterUpdater?.(parameter)
  }

  updateComposeParameter(parameter: Object) {
    this.parameter = parameter
    this.composeParameterUpdater?.(parameter)
  }

  onTouchEvent(e: TouchEvent) {
    const offset = this.getPositionToParent()
    const event = transformTouchEvent(e, vp2px)
    offsetTouchEventLocalPosition(event, { x: -vp2px(offset.x), y: -vp2px(offset.y) })
    return this.builderNode.postTouchEvent(event)
  }

  // postMouseEvent enabled since 20
  postMouseEvent(e: MouseEvent): boolean {
    if (deviceInfo.sdkApiVersion < PostInputEventMinSdkApiVersion) {
      return false
    }
    const offset = this.getPositionToParent()
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
    const offset = this.getPositionToParent()
    const event = transformAxisEvent(e, vp2px)
    offsetAxisEventLocalPosition(event, { x: -vp2px(offset.x), y: -vp2px(offset.y) })
    const consumed = this.builderNode.postInputEvent(event)
    return consumed
  }

  reset() {
    const clipMaxValue = 10000;
    this.constraint = null;
    this.setClipBounds(0, 0, clipMaxValue, clipMaxValue)
    this.setVisible(true)
    this.setTranslation(clipMaxValue, clipMaxValue)
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

  disposeView() {
    ComposeLogger.i(TAG, `disposeView named ${this.name}`)
    this.removeChild(this.builderNode.getFrameNode())
    this.builderNode.dispose()
    this.parameter = undefined
  }
}

export function constraintSize(size: Size, constraint: LayoutConstraint): Size {
  return {
    width: constraintValue(size.width, constraint.minSize.width, constraint.maxSize.width),
    height: constraintValue(size.height, constraint.minSize.height, constraint.maxSize.height)
  }
}

export function constraintValue(value: number, min: number, max: number): number {
  if (value < min) {
    return min
  } else if (value > max) {
    return max
  } else {
    return value
  }
}

export function size_equals(one: Size, other: Size) {
  return length_equals(one.width, other.width) && length_equals(one.height, other.height)
}

//  For FrameNode size precision:
//  const size: Size = { width: x, height: y }  // size(x * y)
//  setMeasuredSize(size)                       // set size
//  getMeasuredSize()                           // get size(x.9999, y.9999)
export function length_equals(one: number, other: number) {
  return Math.abs(one - other) < 1
}

export const PostInputEventMinSdkApiVersion = 20
