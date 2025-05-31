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

export declare interface ArkUIViewController {

  onPageShow(): void

  onPageHide(): void

  onBackPress(): boolean
}

export declare interface InternalArkUIViewController extends ArkUIViewController {
  init(id: string, backRootView: any /*ArkUIRootView*/, foreRootView: any /*ArkUIRootView*/, touchableRootView: any /*ArkUIRootView*/): void

  initContext(context: any /*Context*/): void

  initUIContext(uiContext: any /*UIContext*/): void

  /* 注入 ArkUI 侧的 Messenger, 给 Compose 侧调用 */
  initMessenger(messenger: /*Messenger*/ any)

  /* 向 Kotlin 发送消息 */
  sendMessage(type: string, message: string): string | null

  aboutToAppear(): void

  aboutToDisappear(): void

  dispatchTouchEvent(touchEvent: any /*TouchEvent*/, ignoreInteropView: boolean): boolean;

  onKeyboardShow(keyboardHeight: number): void

  onKeyboardHide(): void

  requestSyncRefresh(): number

  cancelSyncRefresh(refreshId: number)
}