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

import hilog from '@ohos.hilog'

const domain = 0x8001

export interface Logger {
  d(tag: string, content: string): void

  i(tag: string, content: string): void

  w(tag: string, content: string): void

  e(tag: string, content: string): void
}

export class HiLogger implements Logger {
  d(tag: string, content: string): void {
    if (hilog.isLoggable(domain, tag, hilog.LogLevel.DEBUG)) {
      hilog.debug(domain, tag, content)
    }
  }

  i(tag: string, content: string): void {
    if (hilog.isLoggable(domain, tag, hilog.LogLevel.INFO)) {
      hilog.info(domain, tag, content)
    }
  }

  w(tag: string, content: string): void {
    if (hilog.isLoggable(domain, tag, hilog.LogLevel.WARN)) {
      hilog.warn(domain, tag, content)
    }
  }

  e(tag: string, content: string): void {
    if (hilog.isLoggable(domain, tag, hilog.LogLevel.ERROR)) {
      hilog.error(domain, tag, content)
    }
  }
}

export class ComposeLogger implements Logger {
  private logger: Logger | null = null

  private constructor() {
  }

  setLogger(logger: Logger) {
    this.logger = this.logger
  }

  d(tag: string, content: string): void {
    this.logger?.d(tag, content)
  }

  i(tag: string, content: string): void {
    this.logger?.i(tag, content)
  }

  w(tag: string, content: string): void {
    this.logger?.w(tag, content)
  }

  e(tag: string, content: string): void {
    this.logger?.e(tag, content)
  }

  private static instance: ComposeLogger

  public static getInstance(): ComposeLogger {
    if (!ComposeLogger.instance) {
      ComposeLogger.instance = new ComposeLogger()
    }
    return ComposeLogger.instance
  }
}

export default ComposeLogger.getInstance()