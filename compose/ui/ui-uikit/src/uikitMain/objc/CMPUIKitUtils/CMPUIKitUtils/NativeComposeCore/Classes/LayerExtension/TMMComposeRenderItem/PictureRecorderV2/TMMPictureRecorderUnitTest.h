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


#import <Foundation/Foundation.h>

#ifdef __cplusplus
#ifdef COCOAPODS

/// 仅仅给单元测试
struct PictureRecorderDebugTrace {
    int diffDrawCommandsCount = 0;
    int prepareForNextRecordingCount = 0;
};

#endif
#endif

#ifdef COCOAPODS
#define MARK_DIFF_DRAW_COMMAND_CALL trace.diffDrawCommandsCount += 1;
#define MARK_PREPARE_FOR_NEXT_RECORDING_CALL trace.prepareForNextRecordingCount += 1;
#else
#define MARK_DIFF_DRAW_COMMAND_CALL
#define MARK_PREPARE_FOR_NEXT_RECORDING_CALL
#endif
