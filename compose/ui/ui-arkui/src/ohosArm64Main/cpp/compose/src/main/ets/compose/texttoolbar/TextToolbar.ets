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

import { Point } from '../geometry/Point';
import { Rect } from '../geometry/Rect';
import { Messenger } from '../messenger/Messenger';

export class TextToolbar {
  private messenger: Messenger
  private onShow: (params: TextToolbarParams) => void
  private onHide: () => void

  constructor(messenger: Messenger, onShow: (params: TextToolbarParams) => void, onHide: () => void) {
    this.messenger = messenger
    this.messenger.onReceive("compose.ui.TextToolbar:showMenu", (message) => this.showMenu(message))
    this.messenger.onReceive("compose.ui.TextToolbar:hide", () => this.hide())
    this.onShow = onShow
    this.onHide = onHide
  }

  private showMenu(message: string): string {
    this.onShow(this.parseParams(message))
    this.messenger.send("compose.ui.TextToolbar:onShown", "")
    return ""
  }

  private hide(): string {
    this.onHide()
    this.messenger.send("compose.ui.TextToolbar:onHidden", "")
    return ""
  }

  // message: "left,top,right,bottom,Cut,Copy,Paste,SelectAll"
  private parseParams(message: string): TextToolbarParams {
    const result = new TextToolbarParams()
    const params = message.split(",").map(param => param.trim());
    result.anchor.left = px2vp(params.length >= 1 ? Number.parseFloat(params[0]) || 0 : 0);
    result.anchor.top = px2vp(params.length >= 2 ? Number.parseFloat(params[1]) || 0 : 0);
    result.anchor.right = px2vp(params.length >= 3 ? Number.parseFloat(params[2]) || 0 : 0);
    result.anchor.bottom = px2vp(params.length >= 4 ? Number.parseFloat(params[3]) || 0 : 0);
    result.onCut = params.includes("Cut", 4) ? () => this.onCut() : null;
    result.onCopy = params.includes("Copy", 4) ? () => this.onCopy() : null;
    result.onPaste = params.includes("Paste", 4) ? () => this.onPaste() : null;
    result.onSelectAll = params.includes("SelectAll", 4) ? () => this.onSelectAll() : null;
    return result
  }

  private onCut(): void {
    this.messenger.send("compose.ui.TextToolbar:showMenu.onCut", "")
  }

  private onCopy(): void {
    this.messenger.send("compose.ui.TextToolbar:showMenu.onCopy", "")
  }

  private onPaste(): void {
    this.messenger.send("compose.ui.TextToolbar:showMenu.onPaste", "")
  }

  private onSelectAll(): void {
    this.messenger.send("compose.ui.TextToolbar:showMenu.onSelectAll", "")
  }
}

export class TextToolbarParams {
  anchor: Rect = {
    left: 0,
    top: 0,
    right: 0,
    bottom: 0
  }
  onCut: (() => void) | null = null
  onCopy: (() => void) | null = null
  onPaste: (() => void) | null = null
  onSelectAll: (() => void) | null = null

  update(params: TextToolbarParams) {
    this.anchor = params.anchor
    this.onCut = params.onCut
    this.onCopy = params.onCopy
    this.onPaste = params.onPaste
    this.onSelectAll = params.onSelectAll
  }

  reset() {
    this.anchor = {
      left: 0,
      top: 0,
      right: 0,
      bottom: 0
    }
    this.onCut = null
    this.onCopy = null
    this.onPaste = null
    this.onSelectAll = null
  }
}

@Component
export struct TextToolbarOverlay {
  @Require @State params: TextToolbarParams | null = null

  onMeasureSize(selfLayoutInfo: GeometryInfo, children: Array<Measurable>, constraint: ConstraintSizeOptions): SizeResult {
    children.forEach((child) => {
      child.measure(constraint)
    })
    return { width: selfLayoutInfo.width, height: selfLayoutInfo.height };
  }

  onPlaceChildren(selfLayoutInfo: GeometryInfo, children: Array<Layoutable>, constraint: ConstraintSizeOptions) {
    const container: Rect = {
      left: 0,
      top: 0,
      right: selfLayoutInfo.width,
      bottom: selfLayoutInfo.height
    }
    const anchor: Rect = this.params?.anchor ?? container
    children.forEach((child) => {
      const popup: Size = { width: child.measureResult.width, height: child.measureResult.height }
      child.layout(this.calculatePopupPosition(container, anchor, popup))
    })
  }

  calculatePopupPosition(
    container: Rect,
    anchor: Rect,
    popup: Size,
    marginContainer: number = 16,
    marginAnchor: number = 16
  ): Position {
    // 将 popup 环绕在 anchor 四周将会占用的矩形
    const surround: Rect = {
      left: anchor.left - marginAnchor - popup.width - marginContainer,
      top: anchor.top - marginAnchor - popup.height - marginContainer,
      right: anchor.right + marginAnchor + popup.width + marginContainer,
      bottom: anchor.bottom + marginAnchor + popup.height + marginContainer
    }
    const containerWidth = container.right - container.left
    const containerHeight = container.bottom - container.top

    // surround 四个方向与 container 之间的剩余空间
    const leftArea = (surround.left - container.left) * containerHeight
    const topArea = (surround.top - container.top) * containerWidth
    const rightArea = (container.right - surround.right) * containerHeight
    const bottomArea = (container.bottom - surround.bottom) * containerWidth

    const popupCenterX = (anchor.left + anchor.right - popup.width) / 2
    const popupCenterY = (anchor.top + anchor.bottom - popup.height) / 2

    // 根据 surround 四个方向与 container 之间剩余的空间选择合适摆放 popup 的位置
    let area = 0
    let position: Point = { x: popupCenterX, y: popupCenterY }
    if (bottomArea > area) {
      area = bottomArea
      position = { x: popupCenterX, y: anchor.bottom + marginAnchor }
    }
    if (topArea > area) {
      area = topArea
      position = { x: popupCenterX, y: anchor.top - marginAnchor - popup.height }
    }
    // 仅当上下都没有空间时才考虑左右
    if (area == 0) {
      if (rightArea > area) {
        area = rightArea
        position = { x: anchor.right + marginAnchor, y: popupCenterY }
      }
      if (leftArea > area) {
        area = leftArea
        position = { x: anchor.left - marginAnchor - popup.width, y: popupCenterY }
      }
    }

    // 将 popup 左上角约束在 margined 中
    const margined: Rect = {
      left: marginContainer,
      top: marginContainer,
      right: container.right - marginContainer - popup.width,
      bottom: container.bottom - marginContainer - popup.height
    }
    position = {
      x: Math.max(margined.left, Math.min(position.x, margined.right)),
      y: Math.max(margined.top, Math.min(position.y, margined.bottom))
    }
    return position;
  }

  @Builder
  popup() {
    Row() {
      if (this.params?.onCut) {
        Button($r('app.string.text_toolbar_cut'))
          .monopolizeEvents(true)
          .onClick(() => {
            this.params?.onCut?.()
          })
          .fontSize(14)
          .fontColor(Color.Black)
          .padding({ left: "12vp", right: "12vp" })
          .borderRadius('50%')
          .backgroundColor(Color.White)
      }

      if (this.params?.onCopy) {
        Button($r("app.string.text_toolbar_copy"))
          .monopolizeEvents(true)
          .onClick(() => {
            this.params?.onCopy?.()
          })
          .fontSize(14)
          .fontColor(Color.Black)
          .padding({ left: "12vp", right: "12vp" })
          .borderRadius('50%')
          .backgroundColor(Color.White)
      }

      if (this.params?.onPaste) {
        Stack() {
          PasteButton({ text: PasteDescription.PASTE, buttonType: ButtonType.Normal })
            .onClick((event, result) => {
              this.params?.onPaste?.()
              console.log("222")
            })
            .fontSize(14)
            .fontColor(Color.Black)
            .padding({ left: "12vp", right: "12vp" })
            .borderRadius('50%')
            .backgroundColor(Color.White)
        }
        .onTouch((event) => {
          event.stopPropagation()
        })
      }

      if (this.params?.onSelectAll) {
        Button($r('app.string.text_toolbar_select_all'))
          .monopolizeEvents(true)
          .onClick(() => {
            this.params?.onSelectAll?.()
          })
          .fontSize(14)
          .fontColor(Color.Black)
          .padding({ left: "12vp", right: "12vp" })
          .borderRadius('50%')
          .backgroundColor(Color.White)
      }
    }
    .padding($r('sys.float.corner_radius_level3'))
    .height("40vp")
    .backgroundColor(Color.White)
    .borderRadius('50%')
    .shadow({ radius: 40, color: "#32000000" })
  }

  build() {
    if (this.params && (this.params.onCut || this.params.onCopy || this.params.onPaste || this.params.onSelectAll)) {
      this.popup()
    }
  }
}