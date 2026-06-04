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

import List from '@ohos.util.List';

export interface ComponentLifecycle {

  currentState: ComponentLifecycleState

  addObserver(observer: ComponentLifecycleObserver): void

  removeObserver(observer: ComponentLifecycleObserver): void
}

export class ComponentLifecycleImpl implements ComponentLifecycle, ComponentLifecycleObserver {
  private observers: List<ComponentLifecycleObserver> = new List()
  currentState: ComponentLifecycleState = ComponentLifecycleState.INITIALIZED

  aboutToAppear(): void {
    this.currentState = ComponentLifecycleState.APPEARED
    this.observers.forEach(element => {
      element.aboutToAppear()
    });
  }

  aboutToDisappear(): void {
    this.currentState = ComponentLifecycleState.DISAPPEARED
    this.observers.forEach(element => {
      element.aboutToDisappear()
    });
    this.observers.clear()
  }

  addObserver(observer: ComponentLifecycleObserver): void {
    if (this.currentState >= ComponentLifecycleState.DISAPPEARED) {
      return
    }
    if (this.currentState >= ComponentLifecycleState.APPEARED) {
      observer.aboutToAppear()
    }
    this.observers.add(observer)
  }

  removeObserver(observer: ComponentLifecycleObserver): void {
    // 参考 Android Lifecycle 生命周期，移除监听时故意不发送销毁事件
    // we consciously decided not to send destruction events here in opposition to addObserver.
    // Our reasons for that:
    // 1. These events haven't yet happened at all. In contrast to events in addObservers, that
    // actually occurred but earlier.
    // 2. There are cases when removeObserver happens as a consequence of some kind of fatal
    // event. If removeObserver method sends destruction events, then a clean up routine becomes
    // more cumbersome. More specific example of that is: your LifecycleObserver listens for
    // a web connection, in the usual routine in OnStop method you report to a server that a
    // session has just ended and you close the connection. Now let's assume now that you
    // lost an internet and as a result you removed this observer. If you get destruction
    // events in removeObserver, you should have a special case in your onStop method that
    // checks if your web connection died and you shouldn't try to report anything to a server.
    this.observers.remove(observer)
  }
}

export interface ComponentLifecycleObserver {
  aboutToAppear(): void

  aboutToDisappear(): void
}

export enum ComponentLifecycleState {
  INITIALIZED, APPEARED, DISAPPEARED
}