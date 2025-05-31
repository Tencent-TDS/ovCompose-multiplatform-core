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

import { window } from '@kit.ArkUI'

export class RotateScreenManager {
  private TAG = "RotateScreenManager"
  private ROTATE_ANIM_TIME = 600
  private timeoutId: number | undefined = undefined
  private callback: ScreenChangeCallback | undefined = undefined

  /**
   * 系统回调转屏开始通知
   * */
  private sysCallback: Callback<Size> = () => {
    console.log(this.TAG, "onScreenChangeStart～～～")
    // 转屏开始时结束上一次转屏
    this.notifyScreenChangeEnd()
    this.callback?.onScreenChangeStart()
    this.timeoutId = setTimeout(this.pendingCallback, this.ROTATE_ANIM_TIME)
  }

  /**
   * 自定义转屏结束通知
   * */
  private pendingCallback = () => {
    this.notifyScreenChangeEnd()
  }

  registerScreenChange(component: object, callback: ScreenChangeCallback) {
    this.callback = callback
    window.getLastWindow(getContext(component)).then((windowClass: window.Window) => {
      windowClass.on('windowSizeChange', this.sysCallback)
    })
  }

  unregisterScreenChange(component: object) {
    if (!this.sysCallback) {
      return
    }
    window.getLastWindow(getContext(component)).then((windowClass: window.Window) => {
      windowClass.off('windowSizeChange', () => this.sysCallback)
      this.notifyScreenChangeEnd()
      this.callback = undefined
    })
  }

  private notifyScreenChangeEnd() {
    if (this.timeoutId) {
      clearTimeout(this.timeoutId)
      this.timeoutId = undefined
      console.log(this.TAG, "onScreenChangeEnd～～～")
      this.callback?.onScreenChangeEnd()
    }
  }
}

export interface ScreenChangeCallback {
  onScreenChangeStart: () => void
  onScreenChangeEnd: () => void
}
