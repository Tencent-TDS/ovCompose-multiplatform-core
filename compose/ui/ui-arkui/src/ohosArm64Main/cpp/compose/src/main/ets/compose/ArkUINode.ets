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

import { FrameNode, RectShape, RenderNode, ShapeClip } from '@kit.ArkUI';

export class ArkUINode extends FrameNode {
  private shapeClip?: ShapeClip

  setId(value: string) {
    return this.commonAttribute.id(value)
  }

  getId(): string {
    return super.getId()
  }

  setBackgroundColor(backgroundColor: number) {
    this.requireRenderNode().backgroundColor = backgroundColor
  }

  getBackgroundColor() {
    return this.requireRenderNode().backgroundColor
  }

  setClipBounds(left: number, top: number, right: number, bottom: number) {
    if (!this.shapeClip) {
      this.shapeClip = new ShapeClip();
    }
    this.shapeClip.setRectShape({
      left: left,
      top: top,
      right: right,
      bottom: bottom
    })
    this.requireRenderNode().shapeClip = this.shapeClip
  }

  clip(value: boolean) {
    return this.commonAttribute.clip(value)
  }

  clipShape(value: CircleShape | EllipseShape | PathShape | RectShape) {
    return this.commonAttribute.clipShape(value)
  }

  setSize(width: number, height: number) {
    this.commonAttribute.size({ width: width, height: height })
  }

  setTranslation(x: number, y: number) {
    return this.commonAttribute.offset({ x: x, y: y })
  }

  offset(value: Position | Edges | LocalizedEdges) {
    return this.commonAttribute.offset(value)
  }

  setVisible(visible: boolean) {
    return this.commonAttribute.visibility(visible ? Visibility.Visible : Visibility.Hidden)
  }

  visibility(value: Visibility) {
    return this.commonAttribute.visibility(value)
  }

  setOnClick(callback: Callback<ClickEvent> | undefined) {
    return this.commonEvent.setOnClick(callback)
  }

  customProperty(name: string, value: Optional<Object>) {
    return this.commonAttribute.customProperty(name, value)
  }

  getCustomProperty(name: string): Object | undefined {
    return super.getCustomProperty(name)
  }

  requireRenderNode(): RenderNode {
    const render = this.getRenderNode()
    if (render) {
      return render
    } else {
      throw Error("The render node of ArkUINode is null.")
    }
  }
}