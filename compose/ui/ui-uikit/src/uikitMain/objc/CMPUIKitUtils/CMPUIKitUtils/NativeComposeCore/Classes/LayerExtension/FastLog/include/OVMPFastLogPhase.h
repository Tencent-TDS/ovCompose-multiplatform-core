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

/* ============================================================================
 * 全局唯一，根据这个 phase ，拼接出不同的日志，这个是日志最终解释
 * ============================================================================ */
typedef NS_ENUM(NSUInteger, OVMPFastLogPhase) {
    OVMPFastLogPhaseTextOnKtSyncBegin = 0,
    OVMPFastLogPhaseTextOnKtAsync1Begin = 1,
    OVMPFastLogPhaseTextOnKtAsync3Begin = 2,
    OVMPFastLogPhaseTextOnKtSyncHitCacheEnd = 3,
    OVMPFastLogPhaseTextOnKtSyncDrawNullTextEnd = 4,
    OVMPFastLogPhaseTextOnKtAsync3DrawNullTextEnd = 5,
    OVMPFastLogPhaseTextOnKtAsync3HitCacheEnd = 6,
    OVMPFastLogPhaseTextOnKtAsync1DrawNullTextEnd = 7,
    OVMPFastLogPhaseTextOnKtAsync1HitCacheEnd = 8,
    OVMPFastLogPhaseTextTextFunc = 9,
    OVMPFastLogPhaseTextImageFunc = 10,
    
    // 文本
    OVMPFastLogPhaseTextOnCommitDrawTextSkiaBitmapWithCacheKey = 201,
    OVMPFastLogPhaseTextOnCommitDrawNullText = 202,
    OVMPFastLogPhaseTextOnExecDrawTMMCALayerDrawTextSkBitmapV3 = 203,
    OVMPFastLogPhaseTextOnExecDrawNullTextV3 = 204,
    OVMPFastLogPhaseTextOnExecSkBitmapWithUIImagePtr = 205,
    OVMPFastLogPhaseTextOnExecDrawImageWithCacheKeyIfNeed = 206,
    OVMPFastLogPhaseTextOnExecTMMCALayerDrawTextSkBitmapUIImageV3UpdateImage = 207,
    OVMPFastLogPhaseTextOnExecTMMCALayerDrawTextSkBitmapUIImageV3ClearContents = 208,
    OVMPFastLogPhaseTextOnCommitaAsyncDrawIntoCanvas = 209,
    OVMPFastLogPhaseTextAsyncLayerGlobalQueueCreateTextImage = 210,
    OVMPFastLogPhaseTextAsyncLayerSetImage = 211,
    OVMPFastLogPhaseTextAsyncLayerTaskIdInvalid = 212,
    OVMPFastLogPhaseTextAsyncLayerGlobalQueueCreateTextImageV2 = 213,
    
    // Image
    OVMPFastLogPhaseImageOnCommitDrawImage = 301,
    OVMPFastLogPhaseImageOnLayerBeginSetImage = 302,
    OVMPFastLogPhaseImageOnLayerAsyncCreateImageByColorMatrix = 303,
    OVMPFastLogPhaseImageOnLayerAsyncSetImageByColorMatrix = 304,
    OVMPFastLogPhaseImageOnLayerWillNoClipAndSetImageWithTintColor = 305,
    
    OVMPFastLogPhaseImageOnClipLayerSetImageBegin = 306,
    OVMPFastLogPhaseImageOnClipLayerTargetLayerSetImageBegin = 307,
    OVMPFastLogPhaseImageOnClipLayerTargetLayerPrintParams = 308,
    OVMPFastLogPhaseImageOnClipLayerTargetLayerSetImageFinish = 309,
    OVMPFastLogPhaseImageOnFastTintColorImageContentLayerSetImage = 310,
    
};
