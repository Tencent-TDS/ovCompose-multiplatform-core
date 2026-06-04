/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

#import "TMMAsyncTaskLayer.h"
#import "TMMDrawUtils.h"
#import "TMMCALayerSaveState.h"
#import "OVComposeExperimentalConfig.h"
#import "OVMPFastLogCAPI.h"

NS_INLINE dispatch_queue_t TextAsyncPaintQueue(void) {
    static dispatch_queue_t queue = NULL;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        queue = dispatch_queue_create("com.tencent.ovmp.compose.text.paint.queue", DISPATCH_QUEUE_SERIAL);
        dispatch_set_target_queue(queue, dispatch_get_global_queue(DISPATCH_QUEUE_PRIORITY_HIGH, 0));
    });
    return queue;
}

@interface TMMAsyncTaskLayer()

/// 异步任务的 id，用于取消一个异步任务
@property (nonatomic, assign) NSInteger taskId;

@end

@implementation TMMAsyncTaskLayer

+ (BOOL)automaticallyNotifiesObserversForKey:(NSString *)key {
    if ([key isEqualToString:@"bounds"]) {
        return NO;
    } else if ([key isEqualToString:@"position"]) {
        return NO;
    } else if ([key isEqualToString:@"transform"]) {
        return NO;
    } else if ([key isEqualToString:@"frame"]) {
        return NO;
    }
    return [super automaticallyNotifiesObserversForKey:key];
}

- (id<CAAction>)actionForKey:(NSString *)event {
    return nil;
}

- (void)cancelAsyncTask {
    self.taskId += 1;
}

- (void)commitAsyncTask:(TMMNativeOneResultBlock)asyncTask
                density:(float)density
     experimentalConfig:(OVComposeExperimentalConfig *)experimentalConfig
         completedBlock:(TMMNativeTextImageDrawCompleted)completedBlock{
    self.contentsGravity = kCAGravityTop;
    self.contentsScale = density;
    NSInteger currentTaskId = self.taskId;
    intptr_t layerPtr = (intptr_t)self;
    int64_t taskId = ovmp_get_current_trace_id();
    if (experimentalConfig.textFixLeakType == TMMTextFixLeakTypeNone) {
        dispatch_async(TextAsyncPaintQueue(), ^{
            intptr_t imageAddress = asyncTask();
            CFTypeRef imageRef = (CFTypeRef)imageAddress;
            UIImage *image = (__bridge UIImage *)imageRef;
            ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                              taskId,
                              OVMPFastLogPhaseTextAsyncLayerGlobalQueueCreateTextImage,
                              layerPtr,
                              (intptr_t)imageRef);
            dispatch_async(dispatch_get_main_queue(), ^{
                if (currentTaskId == self.taskId) {
                    if (image) {
                        self.contents = (__bridge id)image.CGImage;
                        CFRelease(imageRef);
                    } else {
                        self.contents = nil;
                    }
                    ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                                      taskId,
                                      OVMPFastLogPhaseTextAsyncLayerSetImage,
                                      layerPtr,
                                      (intptr_t)imageRef);
                } else {
                    ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                                      taskId,
                                      OVMPFastLogPhaseTextAsyncLayerTaskIdInvalid,
                                      layerPtr,
                                      (intptr_t)imageRef);
                    if (image) {
                        CFRelease(imageRef);
                    }
                }
                if (completedBlock) {
                    completedBlock();
                }
            });
        });
    } else {
        [self commitNewAsyncTaskId:currentTaskId
                              task:asyncTask
                           density:density
                experimentalConfig:experimentalConfig
                    completedBlock:completedBlock];
    }
}

- (void)commitNewAsyncTaskId:(NSInteger)currentTaskId
                        task:(TMMNativeOneResultBlock)asyncTask
                     density:(float)density
          experimentalConfig:(OVComposeExperimentalConfig *)experimentalConfig
              completedBlock:(TMMNativeTextImageDrawCompleted)completedBlock {
    const intptr_t layerPtr = (intptr_t)self;
    const int64_t taskId = ovmp_get_current_trace_id();
    dispatch_async(TextAsyncPaintQueue(), ^{
        intptr_t imageAddress = asyncTask();
        CFTypeRef imageRef = (CFTypeRef)imageAddress;
        __block UIImage *image = (__bridge UIImage *)imageRef;
        ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                          taskId,
                          OVMPFastLogPhaseTextAsyncLayerGlobalQueueCreateTextImageV2,
                          layerPtr,
                          (intptr_t)imageRef);
        dispatch_async(dispatch_get_main_queue(), ^{
            if (currentTaskId == self.taskId) {
                if (image) {
                    self.contents = (__bridge id)image.CGImage;
                } else {
                    self.contents = nil;
                }
                ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                                  taskId,
                                  OVMPFastLogPhaseTextAsyncLayerSetImage,
                                  layerPtr,
                                  (intptr_t)imageRef);
            } else {
                ovmp_fastlog_log4(OVMP_FASTLOG_TAG_TEXT,
                                  taskId,
                                  OVMPFastLogPhaseTextAsyncLayerTaskIdInvalid,
                                  layerPtr,
                                  (intptr_t)imageRef);
            }
            if (imageRef != NULL) {
                CFRelease(imageRef);
                image = nil;
            }
            if (completedBlock) {
                completedBlock();
            }
        });
    });
}

#ifdef DEBUG
- (NSDictionary *)lookin_customDebugInfos {
    return @{
        @"title" : [NSString stringWithFormat:@"Text-%ld", self.debugTag],
        @"properties" : @[ @{
            @"title" : @"Layer 信息",
            @"valueType" : @"string",
            @"section" : @"Layer 详细信息",
            @"value" : [NSString stringWithFormat:@"%@ {skBitmap:%ld} transform:%@", self, self.debugTag, TMMNSStringFromCATransform3D(self.transform)]
        } ]
    };
}
#endif

@end
