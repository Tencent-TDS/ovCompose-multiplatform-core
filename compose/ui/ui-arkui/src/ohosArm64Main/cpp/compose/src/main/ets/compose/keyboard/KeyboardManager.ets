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

import { common } from '@kit.AbilityKit'
import { InternalArkUIViewController } from 'libcompose_arkui_utils.so'
import { KeyboardStatusThrottle } from '../KeyboardStatusThrottle'

export class KeyboardManager {
  private context: common.UIAbilityContext
  private controller: InternalArkUIViewController
  // 鸿蒙通知有bug，键盘打开时，会从SHOW->HIDE->SHOW，为避免频繁通知，使用节流处理
  private keyboardStatusThrottle = new KeyboardStatusThrottle((keyboardHeight) => {
    this.controller.onKeyboardShow(keyboardHeight)
  }, () => {
    this.controller.onKeyboardHide()
  });
  private keyboardHeightChangeCallback: Callback<number> = (height) => {
    if (height > 0) {
      this.keyboardStatusThrottle.onKeyboardShow(height)
    } else {
      this.keyboardStatusThrottle.onKeyboardHide()
    }
  }

  constructor(context: common.UIAbilityContext, controller: InternalArkUIViewController) {
    this.context = context
    this.controller = controller
  }

  aboutToAppear() {
    this.context.windowStage.getMainWindow().then((window) => {
      window.on("keyboardHeightChange", this.keyboardHeightChangeCallback)
    })
  }

  aboutToDisappear() {
    this.context.windowStage.getMainWindow().then((window) => {
      window.off("keyboardHeightChange", this.keyboardHeightChangeCallback)
    })
  }
}