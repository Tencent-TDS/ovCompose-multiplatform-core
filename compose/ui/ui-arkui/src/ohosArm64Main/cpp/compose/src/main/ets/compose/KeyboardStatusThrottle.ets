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

import hilog from '@ohos.hilog';

enum State {
  Hide,
  Show
}

abstract class StatusRecord {
  readonly state: State;

  constructor(state: State) {
    this.state = state;
  }
}

class ShowRecord extends StatusRecord {
  readonly keyboardHeight: number;

  constructor(keyboardHeight: number) {
    super(State.Show);
    this.keyboardHeight = keyboardHeight;
  }
}

class HideRecord extends StatusRecord {
  constructor() {
    super(State.Hide);
  }
}

const DELAY_TIME = 50

export class KeyboardStatusThrottle {

  private static idGenerator = 0

  private key: string;
  private keyboardShowCallback: (keyboardHeight: number) => void;
  private keyboardHideCallback: () => void;

  private lastStatusRecord: StatusRecord | null = null;
  private statusRecordQueue: StatusRecord[] = [];
  private pendingCallback: (() => void) | null = null;

  constructor(
    keyboardShowCallback: (keyboardHeight: number) => void,
    keyboardHideCallback: () => void
  ) {
    this.key = `KeyboardStatusThrottle_${KeyboardStatusThrottle.idGenerator++}`;
    this.keyboardShowCallback = keyboardShowCallback;
    this.keyboardHideCallback = keyboardHideCallback;
  }

  onKeyboardShow(keyboardHeight: number) {
    this.sendStatusRecord(new ShowRecord(keyboardHeight));
  }

  onKeyboardHide() {
    this.sendStatusRecord(new HideRecord());
  }

  private sendStatusRecord(record: StatusRecord) {
    hilog.debug(0, this.key, "sendStatusRecord, record: " + JSON.stringify(record));
    this.statusRecordQueue.push(record);
    if (!this.pendingCallback) {
      this.pendingCallback = () => {
        this.pendingCallback = null;
        this.processStatusRecords();
      }
      setTimeout(this.pendingCallback, DELAY_TIME)
    }
  }

  private processStatusRecords() {
    while (this.statusRecordQueue.length) {
      const record = this.statusRecordQueue.shift()!!;
      hilog.debug(0, this.key, "processStatusRecords, process record: " + JSON.stringify(record));
      this.lastStatusRecord = record;
    }
    hilog.debug(0, this.key, "processStatusRecords, last record: " + JSON.stringify(this.lastStatusRecord));
    if (this.lastStatusRecord!!.state == State.Show) {
      const showRecord = this.lastStatusRecord as ShowRecord;
      this.keyboardShowCallback(showRecord.keyboardHeight);
    } else {
      this.keyboardHideCallback();
    }
  }
}