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

#import "TMMComposeSignPost.h"
#import <os/signpost.h>

#define TMMCOMPOSE_HAS_OS_SIGNPOST __has_include(<os/signpost.h>)

/*
  OSSignpostID：Apple 用来区分有重叠任务的标记 id，一般都会使用这个
  参考资料：https://devstreaming-cdn.apple.com/videos/wwdc/2018/405bjty1j94taqv8ii/405/405_measuring_performance_using_logging.pdf?dl=1

  os_signpost_id_make_with_id identifier 参数一样，
  则 os_signpost_id_t 一样
 */

/// Performance log
os_log_t _Nonnull TMMPerformanceLog(void);

#define TraceSPBegin(name, identifier, format, ...)                               \
    {                                                                             \
        if (@available(iOS 12.0, *)) {                                            \
            os_log_t log = TMMPerformanceLog();                                   \
            os_signpost_id_t spid = os_signpost_id_make_with_id(log, identifier); \
            os_signpost_interval_begin(log, spid, #name, format, ##__VA_ARGS__);  \
        }                                                                         \
    }

#define TraceSPEnd(name, identifier, format, ...)                                 \
    {                                                                             \
        if (@available(iOS 12.0, *)) {                                            \
            os_log_t log = TMMPerformanceLog();                                   \
            os_signpost_id_t spid = os_signpost_id_make_with_id(log, identifier); \
            os_signpost_interval_end(log, spid, #name, format, ##__VA_ARGS__);    \
        }                                                                         \
    }

os_log_t _Nonnull TMMPerformanceLog(void) {
    static dispatch_once_t onceToken;
    static os_log_t logT;
    dispatch_once(&onceToken, ^{
        logT = os_log_create("compose", OS_LOG_CATEGORY_POINTS_OF_INTEREST);
    });
    return logT;
}

#pragma mark - public
static NSString *DrawFrameIdentifier = @"DrawFrameIdentifier";
static NSString *ScheduledTasksIdentifier = @"ScheduledTasksIdentifier";
static NSString *RecompositionIdentifier = @"RecompositionIdentifier";
static NSString *LayoutIdentifier = @"LayoutIdentifier";
static NSString *EffectsIdentifier = @"EffectsIdentifier";
static NSString *PointInputIdentifier = @"PointInputIdentifier";
static NSString *DrawIdentifier = @"DrawIdentifier";
static NSString *ListPreComposeIdentifier = @"ListPreComposeIdentifier";
static NSString *ListPreMeasureIdentifier = @"ListPreMeasureIdentifier";
static NSString *ListPreVsyncIdentifier = @"ListPreVsyncIdentifier";

void TMMNativeTraceBegin(NSInteger scene, NSInteger frameId) {
    switch (scene) {
        case TMMNativeTraceSceneDrawFrame:
            TraceSPBegin("DrawFrame", DrawFrameIdentifier, "%s-%ld", "DrawFrame", frameId);
            break;
        case TMMNativeTraceSceneScheduledTasks:
            TraceSPBegin("ScheduledTasks", ScheduledTasksIdentifier, "%s-%ld", "ScheduledTasks", frameId);
            break;
        case TMMNativeTraceSceneRecomposition:
            TraceSPBegin("Recomposition", RecompositionIdentifier, "%s-%ld", "Recomposition", frameId);
            break;
        case TMMNativeTraceSceneLayout:
            TraceSPBegin("Layout", LayoutIdentifier, "%s-%ld", "Layout", frameId);
            break;
        case TMMNativeTraceSceneEffects:
            TraceSPBegin("Effects", EffectsIdentifier, "%s-%ld", "Effects", frameId);
            break;
        case TMMNativeTraceScenePointInput:
            TraceSPBegin("PointInput", PointInputIdentifier, "%s-%ld", "PointInput", frameId);
            break;
        case TMMNativeTraceSceneDraw:
            TraceSPBegin("Draw", DrawIdentifier, "%s-%ld", "Draw", frameId);
            break;
        case TMMNativeTraceScenePreCompose:
            TraceSPBegin("ListPreCompose", ListPreComposeIdentifier, "%s-%ld", "ListPreCompose", frameId);
            break;
        case TMMNativeTraceScenePreMeasure:
            TraceSPBegin("ListPreMeasure", ListPreMeasureIdentifier, "%s-%ld", "ListPreMeasure", frameId);
            break;
        default:
            break;
    }
}

void TMMNativeTraceEnd(NSInteger scene, NSInteger frameId) {
    switch (scene) {
        case TMMNativeTraceSceneDrawFrame:
            TraceSPEnd("DrawFrame", DrawFrameIdentifier, "%s-%ld", "DrawFrame", frameId);
            break;
        case TMMNativeTraceSceneScheduledTasks:
            TraceSPEnd("ScheduledTasks", ScheduledTasksIdentifier, "%s-%ld", "ScheduledTasks", frameId);
            break;
        case TMMNativeTraceSceneRecomposition:
            TraceSPEnd("Recomposition", RecompositionIdentifier, "%s-%ld", "Recomposition", frameId);
            break;
        case TMMNativeTraceSceneLayout:
            TraceSPEnd("Layout", LayoutIdentifier, "%s-%ld", "Layout", frameId);
            break;
        case TMMNativeTraceSceneEffects:
            TraceSPEnd("Effects", EffectsIdentifier, "%s-%ld", "Effects", frameId);
            break;
        case TMMNativeTraceScenePointInput:
            TraceSPEnd("PointInput", PointInputIdentifier, "%s-%ld", "PointInput", frameId);
            break;
        case TMMNativeTraceSceneDraw:
            TraceSPEnd("Draw", DrawIdentifier, "%s-%ld", "Draw", frameId);
            break;
        case TMMNativeTraceScenePreCompose:
            TraceSPEnd("ListPreCompose", ListPreComposeIdentifier, "%s-%ld", "ListPreCompose", frameId);
            break;
        case TMMNativeTraceScenePreMeasure:
            TraceSPEnd("ListPreMeasure", ListPreMeasureIdentifier, "%s-%ld", "ListPreMeasure", frameId);
            break;
        default:
            break;
    }
}
