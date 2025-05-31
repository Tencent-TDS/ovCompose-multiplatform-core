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

import { window } from '@kit.ArkUI';
import { ComponentLifecycle, ComponentLifecycleObserver } from '../lifecycle/ComponentLifecycle';
import { Messenger } from '../messenger/Messenger';

export class WindowStageManager implements ComponentLifecycleObserver {
  private windowStage: window.WindowStage
  private messenger: Messenger
  private componentLifecycle: ComponentLifecycle
  private windowStageEventCallback: Callback<window.WindowStageEventType> = (event) => {
    this.messenger.send("compose.ui:WindowStage.onWindowStageEvent", this.eventToString(event))
  }

  constructor(windowStage: window.WindowStage, messenger: Messenger, componentLifecycle: ComponentLifecycle) {
    this.windowStage = windowStage
    this.messenger = messenger
    this.componentLifecycle = componentLifecycle
    this.componentLifecycle.addObserver(this);
  }

  aboutToAppear(): void {
    this.windowStage.on("windowStageEvent", this.windowStageEventCallback)
  }

  aboutToDisappear(): void {
    this.windowStage.off("windowStageEvent", this.windowStageEventCallback)
  }

  private eventToString(event: window.WindowStageEventType): string {
    switch (event) {
      case window.WindowStageEventType.SHOWN:
        return "SHOWN";
      case window.WindowStageEventType.ACTIVE:
        return "ACTIVE";
      case window.WindowStageEventType.INACTIVE:
        return "INACTIVE";
      case window.WindowStageEventType.HIDDEN:
        return "HIDDEN";
      case window.WindowStageEventType.RESUMED:
        return "RESUMED";
      case window.WindowStageEventType.PAUSED:
        return "PAUSED";
    }
    return "UNKNOWN"
  }
}