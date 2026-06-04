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

#import "OVComposeSkiaOptFuncs.h"
#import "TMMComposeInjectCommonServiceImpl.h"
#import <UIKit/UIKit.h>
#include <dlfcn.h>

typedef NS_ENUM(int, OVMPSkCTFontSmoothBehavior) {
    OVMPSkCTFontSmoothBehaviorNone = 0,
    OVMPSkCTFontSmoothBehaviorSome = 1,
    OVMPSkCTFontSmoothBehaviorSubpixel = 2,
};

#pragma mark - Skia All
// 默认为开，后续可以直接关闭做反向实验
static bool gOVMPSkiaGlobalFOOMFixEnable = true;

bool OVMPSkiaGlobalFOOMGetFixEnable(void) {
    return gOVMPSkiaGlobalFOOMFixEnable;
}

void OVMPSkiaGlobalFOOMSetFixEnable(bool fixEnable) {
    gOVMPSkiaGlobalFOOMFixEnable = fixEnable;
}

#pragma mark - SkTypeface_Mac::getVariationAxes
/**
 * CTFontCopyVariationAxes provides the localized name of all axes, making it very slow.
 * This is unfortunate, its result is needed just to see if there are any axes at all.
 * To avoid calling internal APIs cache the result of CTFontCopyVariationAxes.
 * https://github.com/WebKit/WebKit/commit/1842365d413ed87868e7d33d4fad1691fa3a8129
 * https://bugs.webkit.org/show_bug.cgi?id=232690
 */

typedef CFArrayRef (*CTFontCopyVariationAxesInternalFunc)(CTFontRef font);

FOUNDATION_EXTERN CFArrayRef OVMPCTFontCopyVariationAxesInternal(CTFontRef font) {
    // 该功能已经全量，直接由全局开关进行控制
    if (!gOVMPSkiaGlobalFOOMFixEnable) {
        return CTFontCopyVariationAxes(font);
    }
    static dispatch_once_t onceToken;
    static CTFontCopyVariationAxesInternalFunc _CTFontCopyVariationAxesInternalFunc = NULL;
    dispatch_once(&onceToken, ^{
        void *handle = dlopen("/System/Library/Frameworks/CoreText.framework/CoreText", RTLD_LAZY);
        if (handle) {
            _CTFontCopyVariationAxesInternalFunc = (CTFontCopyVariationAxesInternalFunc)dlsym(handle, "CTFontCopyVariationAxesInternal");
            dlclose(handle);
        }
    });
    
    if (_CTFontCopyVariationAxesInternalFunc != NULL) {
        return _CTFontCopyVariationAxesInternalFunc(font);
    }
    return CTFontCopyVariationAxes(font);
}

#pragma mark - SkCTFontGetDataFontWeightMapping
/// 默认为开
static bool gOVMPSkCTFontGetDataFontWeightMappingCrashFix = true;

/// CMPUtils 内部决定 Skia 内部是否走新逻辑，原因是 Skia 的 SkCTFontGetDataFontWeightMapping 内使用的 CoreText API 导致业务线上 ANR
/// - Parameter enable: 业务测作为开关使用的函数
void OVMPSkCTFontGetDataFontWeightMappingNewMappingSetEnable(bool enable) {
    gOVMPSkCTFontGetDataFontWeightMappingCrashFix = enable;
}

// 返回是否开启 SkCTFontGetDataFontWeightMapping crash 修复的开关，由 Skia 调用
FOUNDATION_EXTERN bool OVMPSkCTFontGetDataFontWeightMappingFixEnable(void) {
    return gOVMPSkiaGlobalFOOMFixEnable && gOVMPSkCTFontGetDataFontWeightMappingCrashFix;
}

/// 当 Skia 内部调用 OVMPSkCTFontGetDataFontWeightMappingFixEnable() 为 true 后，将会调用此方法，代替原本的 SkCTFontGetDataFontWeightMapping 方法
/// 避免 crash，这里开放该函数是为了后面找到新的权重替代逻辑
FOUNDATION_EXTERN void OVMPSkCTFontGetDataFontWeightMapping(double outArray[11]) {
    // 经过测试 SkCTFontGetDataFontWeightMapping 内部的值，可以不用被修改，Skia 内部有正确的兜底逻辑，此处不用处理
}


#pragma mark - SkCTFontGetSmoothBehavior
/// SkCTFontGetSmoothBehavior ANR 修复开关，默认为开
static bool gOVMPSkCTFontGetSmoothBehaviorFixEnable = true;

/// 强制 Skia 开启 SmoothBehaviorSubpixel 的开关，默认为开
static bool gOVMPSkiaSmoothBehaviorSubpixelOptimizationEnable = true;

/// 设置是否打开 SkCTFontSetSmoothBehavior crash 修复的开关
void OVMPSkCTFontSetSmoothBehaviorFixEnable(bool enable) {
    gOVMPSkCTFontGetSmoothBehaviorFixEnable = enable;
}

/// 返回是否开启 SkCTFontGetSmoothBehavior 修复，由 Skia 调用
FOUNDATION_EXTERN bool OVMPSkCTFontGetSmoothBehaviorFixEnable(void) {
    return gOVMPSkiaGlobalFOOMFixEnable && gOVMPSkCTFontGetSmoothBehaviorFixEnable;
}

FOUNDATION_EXTERN void OVMPSetSkiaSmoothBehaviorSubpixelOptimization(bool enable) {
    gOVMPSkiaSmoothBehaviorSubpixelOptimizationEnable = enable;
}

/// 当 Skia 内部调用 OVMPSkCTFontGetSmoothBehaviorFixEnable() 为 true 后，将会调用此方法，代替原本的 SkCTFontGetSmoothBehavior 方法，避免 crash
FOUNDATION_EXTERN int OVMPSkCTFontGetSmoothBehavior(void) {
    if (gOVMPSkiaSmoothBehaviorSubpixelOptimizationEnable) {
        return OVMPSkCTFontSmoothBehaviorSubpixel;
    }
    // iOS 实际测试 Skia 运行完整的 SkCTFontGetSmoothBehavior 后都是返回为 0，所以这里可以固定返回为 0
    return OVMPSkCTFontSmoothBehaviorNone;
}

#pragma mark - SkCTFontGetNSFontWeightMapping

static bool gOVMPSkCTFontSetNSFontWeightMappingFixEnable = false;

void OVMPSkCTFontSetNSFontWeightMappingFixEnable(bool enable) {
    gOVMPSkCTFontSetNSFontWeightMappingFixEnable = enable;
}

FOUNDATION_EXTERN bool OVMPSkCTFontGetNSFontWeightMappingFixEnable(void) {
    return gOVMPSkCTFontSetNSFontWeightMappingFixEnable;
}

/// Skia 内部的实现有点离谱，通过 dlsym 获取到 UIFontWeightUltraLight 等符号，再获取默认值，耗时劣化后有 40ms +
/// 因此这里使用新函数去进行代替 iOS 部分
FOUNDATION_EXTERN void *OVMPSkCTFontGetNSFontWeightMapping(void) {
    static CGFloat nsFontWeights[11];
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        int i = 0;
        nsFontWeights[i++] = -1.00;
        nsFontWeights[i++] = UIFontWeightUltraLight;
        nsFontWeights[i++] = UIFontWeightThin;
        nsFontWeights[i++] = UIFontWeightLight;
        nsFontWeights[i++] = UIFontWeightRegular;
        nsFontWeights[i++] = UIFontWeightMedium;
        nsFontWeights[i++] = UIFontWeightSemibold;
        nsFontWeights[i++] = UIFontWeightBold;
        nsFontWeights[i++] = UIFontWeightHeavy;
        nsFontWeights[i++] = UIFontWeightBlack;
        nsFontWeights[i++] = 1.00;
    });
    return (void *)nsFontWeights;
}


bool OVMPSkFixFontMatchBug(void) {
    static BOOL enable = NO;
    static dispatch_once_t onceToken;
    dispatch_once(&onceToken, ^{
        NSString *tabKey = @"ios_compose_font_bug_fix_enable";
        enable = [TMMComposeInjectCommonServiceImpl.sharedInstance tmm_getTabToggleIsOnKey:tabKey defaultValue:NO];
    });
    return enable;
}
