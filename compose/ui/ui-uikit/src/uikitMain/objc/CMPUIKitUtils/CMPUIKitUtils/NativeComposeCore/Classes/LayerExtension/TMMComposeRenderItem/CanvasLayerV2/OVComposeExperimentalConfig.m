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

#import "OVComposeExperimentalConfig.h"

@implementation OVComposeExperimentalConfig

+ (instancetype)experimentalConfig {
    OVComposeExperimentalConfig *config = [[self alloc] init];
    config.textFixLeakType = TMMTextFixLeakTypeNone;
    return config;
}

@end

intptr_t OVComposeExperimentalConfigCreate(void) {
    OVComposeExperimentalConfig *config = [OVComposeExperimentalConfig experimentalConfig];
    CFTypeRef configRef = (__bridge_retained CFTypeRef)config;
    return (intptr_t)configRef;
}

void OVComposeExperimentalConfigRelease(intptr_t configPtr) {
    if (configPtr != 0) {
        CFRelease((CFTypeRef)configPtr);
    }
}

OVComposeExperimentalConfig *OVComposeConfigPtrToExperimentalConfig(intptr_t configPtr) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        return config;
    }
    return nil;
}

void OVComposeExperimentalConfigSetEnablePerspectiveTransformFix(intptr_t configPtr, BOOL enable) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        config.enablePerspectiveTransformFix = enable;
    }
}

void OVComposeExperimentalConfigSetEnableTextAsyncPaint(intptr_t configPtr, BOOL enable) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        config.enableTextAsyncPaint = enable;
    }
}

void OVComposeExperimentalConfigSetEnableCALayerClipOpt(intptr_t configPtr, BOOL enable) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        config.enableCALayerClipOpt = enable;
    }
}

void OVComposeExperimentalConfigSetEnableImageLog(intptr_t configPtr, BOOL enable) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        config.enableImageLog = enable;
    }
}

void OVComposeExperimentalConfigSetFixTextLeakType(intptr_t configPtr, TMMTextFixLeakType textFixLeakType) {
    if (configPtr != 0) {
        OVComposeExperimentalConfig *config = (__bridge OVComposeExperimentalConfig *)(CFTypeRef)configPtr;
        config.textFixLeakType = textFixLeakType;
    }
}
