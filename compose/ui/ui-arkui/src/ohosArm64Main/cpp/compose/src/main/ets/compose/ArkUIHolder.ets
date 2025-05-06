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

import { ArkUIView } from './ArkUIView'

export type Holder = ArkUIView

@Builder
export function holderBuilder(holder: Holder) {
  ArkUIHolder({ holder: holder, builder: holder.builder, state: holder.parameter })
}

const EmptyObject: object = new Object()

@Component
export struct ArkUIHolder {
  @Require holder!: ArkUIView
  @Require @Provide("compose_args") @Watch("stateWatcher") state: object = EmptyObject
  @BuilderParam builder: (param: object) => void

  stateWatcher(name: string) {
    this.holder.updateComposeParameter(this.state)
  }

  aboutToAppear(): void {
    this.holder.arkUIParameterUpdater = parameter => {
      this.state = parameter
    }
  }

  aboutToDisappear(): void {
    this.holder.arkUIParameterUpdater = null
  }

  build() {
    Stack() {
      this.builder(this.state)
    }
    .gestureModifier(new PanGestureDetector(disallow => {
      this.holder.requestDisallowInterceptTouchEvent(disallow)
    }))
  }

  onMeasureSize(selfLayoutInfo: GeometryInfo, children: Measurable[], constraint: ConstraintSizeOptions): SizeResult {
      return children[0].measure(constraint)
  }
}

type RequestDisallowInterceptTouchEvent = (disallow: boolean) => void

class PanGestureDetector implements GestureModifier {
  private onRequestDisallowInterceptTouchEvent: RequestDisallowInterceptTouchEvent

  constructor(onRequestDisallowInterceptTouchEvent: RequestDisallowInterceptTouchEvent) {
    this.onRequestDisallowInterceptTouchEvent = onRequestDisallowInterceptTouchEvent
  }

  applyGesture(event: UIGestureEvent): void {
    this.applyPanGestureDetector(event, PanDirection.Horizontal)
    this.applyPanGestureDetector(event, PanDirection.Vertical)
  }

  // There are two gesture detectors, one low priority and one parallel.
  // When a child gesture detector recognizes successfully, the low priority one won't be triggered but
  // In that case, request parent to dispatch the subsequent touch event to us in the initial pass.
  applyPanGestureDetector(event: UIGestureEvent, direction: PanDirection): void {
    let detected = false
    event.addGesture(new PanGestureHandler({ direction: direction }).onActionStart(_ => {
      detected = true
    }))
    event.addParallelGesture(new PanGestureHandler({ direction: direction }).onActionStart(_ => {
      if (!detected) {
        this.onRequestDisallowInterceptTouchEvent(true)
      }
      detected = false
    }))
  }
}