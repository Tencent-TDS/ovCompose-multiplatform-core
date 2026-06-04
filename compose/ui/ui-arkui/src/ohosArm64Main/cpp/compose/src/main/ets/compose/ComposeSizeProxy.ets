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

import { Messenger } from './messenger/Messenger';


export class ComposeSizeProxy {
  private sizeCache: Size = { width: 0, height: 0 }
  private listener: ((width: number, height: number) => void) | null = null

  constructor(messenger: Messenger) {
    messenger.onReceive("compose.ui.ComposeSizeChange", (message) => this.onComposeSizeChange(message))
  }

  private onComposeSizeChange(message: string): string {
    let parts = message.split('#');
    if (parts.length === 2) {
      const width = parseInt(parts[0], 10);
      const height = parseInt(parts[1], 10);

      this.sizeCache = { width: width, height: height }
      this.listener?.(width, height)
      console.log("ComposeSizeProxy", "onComposeSizeChange width ", width, "height:", height)

    } else {
      console.error("Invalid input string format");
    }
    return ""
  }

  public registerSizeChange(callback: (width: number, height: number) => void) {
    this.listener = callback
  }

  public unregisterSizeChange() {
    this.listener = null
  }

  public getComposeSize(): Size {
    return this.sizeCache
  }
}
