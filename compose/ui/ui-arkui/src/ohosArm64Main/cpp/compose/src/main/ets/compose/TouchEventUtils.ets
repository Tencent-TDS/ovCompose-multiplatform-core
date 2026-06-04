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

import { Offset } from "@kit.ArkUI"

export function transformTouchEvent(
  event: TouchEvent,
  transform: (value: number) => number
): TouchEvent {
  let transformedEvent = {
    target: event.target,
    timestamp: event.timestamp,
    source: event.source,
    pressure: event.pressure,
    tiltX: transform(event.tiltX),
    tiltY: transform(event.tiltY),
    sourceTool: event.sourceTool,
    type: event.type,
    touches: event.touches.map(touch => transformTouchObject(touch, transform)),
    changedTouches: event.changedTouches.map(touch => transformTouchObject(touch, transform)),
    stopPropagation: event.stopPropagation,
  } as TouchEvent
  return transformedEvent
}

export function transformTouchObject(
  touchObject: TouchObject,
  transform: (value: number) => number
): TouchObject {
  return {
    type: touchObject.type,
    id: touchObject.id,
    displayX: transform(touchObject.displayX),
    displayY: transform(touchObject.displayY),
    windowX: transform(touchObject.windowX),
    windowY: transform(touchObject.windowY),
    screenX: transform(touchObject.screenX),
    screenY: transform(touchObject.screenY),
    x: transform(touchObject.x),
    y: transform(touchObject.y)
  } as TouchObject
}

export function transformHistoricalPoint(
  historicalPoint: HistoricalPoint,
  transform: (value: number) => number
): HistoricalPoint {
  return {
    touchObject: transformTouchObject(historicalPoint.touchObject, transform),
    size: transform(historicalPoint.size),
    force: historicalPoint.force,
    timestamp: historicalPoint.timestamp
  } as HistoricalPoint
}

export function offsetTouchEventLocalPosition(event: TouchEvent, offset: Offset) {
  event.touches.forEach(touch => {
    offsetTouchObjectLocalPosition(touch, offset)
  })
  event.changedTouches.forEach(touch => {
    offsetTouchObjectLocalPosition(touch, offset)
  })
}

function offsetTouchObjectLocalPosition(event: TouchObject, offset: Offset) {
  event.x += offset.x
  event.y += offset.y
}

export function transformMouseEvent(event: MouseEvent, transform: (value: number) => number): MouseEvent {
  return {
    // BaseEvent start
    target: event.target,
    timestamp: event.timestamp,
    source: event.source,
    axisHorizontal: event.axisHorizontal,
    axisVertical: event.axisVertical,
    pressure: event.pressure,
    tiltX: transform(event.tiltX),
    tiltY: transform(event.tiltY),
    rollAngle: event.rollAngle,
    getModifierKeyState: event.getModifierKeyState,
    deviceId: event.deviceId,
    targetDisplayId: event.targetDisplayId,
    sourceTool: event.sourceTool,
    // BaseEvent end

    // MouseEvent start
    button: event.button,
    action: event.action,
    globalDisplayX: transform(event.globalDisplayX ?? 0),
    globalDisplayY: transform(event.globalDisplayY ?? 0),
    displayX: transform(event.displayX),
    displayY: transform(event.displayY),
    windowX: transform(event.windowX),
    windowY: transform(event.windowY),
    screenX: transform(event.screenX),
    screenY: transform(event.screenY),
    x: transform(event.x),
    y: transform(event.y),
    rawDeltaX: transform(event.rawDeltaX ?? 0),
    rawDeltaY: transform(event.rawDeltaY ?? 0),
    stopPropagation: event.stopPropagation,
    pressedButtons: event.pressedButtons
    // MouseEvent end
  } as MouseEvent
}

export function transformAxisEvent(event: AxisEvent, transform: (value: number) => number): AxisEvent {
  return {
    // BaseEvent start
    target: event.target,
    timestamp: event.timestamp,
    source: event.source,
    axisHorizontal: event.axisHorizontal,
    axisVertical: event.axisVertical,
    pressure: event.pressure,
    tiltX: transform(event.tiltX),
    tiltY: transform(event.tiltY),
    rollAngle: event.rollAngle,
    getModifierKeyState: event.getModifierKeyState,
    deviceId: event.deviceId,
    targetDisplayId: event.targetDisplayId,
    sourceTool: event.sourceTool,
    // BaseEvent end
    // AxisEvent start
    action: event.action,
    globalDisplayX: transform(event.globalDisplayX ?? 0),
    globalDisplayY: transform(event.globalDisplayY ?? 0),
    displayX: transform(event.displayX),
    displayY: transform(event.displayY),
    windowX: transform(event.windowX),
    windowY: transform(event.windowY),
    x: transform(event.x),
    y: transform(event.y),
    scrollStep: event.scrollStep,
    propagation: event.propagation,
    getHorizontalAxisValue: event.getHorizontalAxisValue,
    getVerticalAxisValue: event.getVerticalAxisValue,
    // AxisEvent end
  } as AxisEvent
}

export function offsetAxisEventLocalPosition(event: AxisEvent, offset: Offset) {
  event.x += offset.x
  event.y += offset.y
  event.displayX += offset.x
  event.displayY += offset.y
  event.windowX += offset.x
  event.windowY += offset.y
  if (event.globalDisplayX) {
    event.globalDisplayX += offset.x
  }
  if (event.globalDisplayY) {
    event.globalDisplayY += offset.y
  }
}

export function offsetMouseEventLocalPosition(event: MouseEvent, offset: Offset) {
  event.x += offset.x
  event.y += offset.y
  event.displayX += offset.x
  event.displayY += offset.y
  event.windowX += offset.x
  event.windowY += offset.y
  event.screenX += offset.x
  event.screenY += offset.y
  if (event.globalDisplayX) {
    event.globalDisplayX += offset.x
  }
  if (event.globalDisplayY) {
    event.globalDisplayY += offset.y
  }
}