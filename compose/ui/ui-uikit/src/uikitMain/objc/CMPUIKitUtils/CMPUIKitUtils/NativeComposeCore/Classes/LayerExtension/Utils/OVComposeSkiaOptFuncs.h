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
#import <CoreText/CoreText.h>

NS_ASSUME_NONNULL_BEGIN

/// 返回是否开启 Skia 所有优化的总开关，此开关为关则所有优化失效
FOUNDATION_EXTERN bool OVMPSkiaGlobalFOOMGetFixEnable(void);

/// 更改 Skia 优化总开关的值
/// - Parameter fixEnable: bool
FOUNDATION_EXTERN void OVMPSkiaGlobalFOOMSetFixEnable(bool fixEnable);

#pragma mark - SkCTFontGetDataFontWeightMapping

/// CMPUtils 内部决定 Skia 内部是否走新逻辑，原因是 Skia 的 SkCTFontGetDataFontWeightMapping 内使用的 CoreText API 导致业务线上 ANR
/// - Parameter enable: 业务测作为开关使用的函数
FOUNDATION_EXTERN void OVMPSkCTFontGetDataFontWeightMappingNewMappingSetEnable(bool enable);

#pragma mark - SkCTFontGetSmoothBehavior

/// 设置强制开始 Skia 的字体 Subpixel 优化，前提是 OVMPSkCTFontSetSmoothBehaviorFixEnable 开启
FOUNDATION_EXTERN void OVMPSetSkiaSmoothBehaviorSubpixelOptimization(bool enable);

/// 设置是否打开 SkCTFontSetSmoothBehavior crash 修复的开关，由 Skia 内部调用过来
FOUNDATION_EXTERN void OVMPSkCTFontSetSmoothBehaviorFixEnable(bool enable);

/// 设置是否开启 SkCTFontSetNSFontWeightMapping() 函数耗时优化
/// - Parameter enable: 是否开启
FOUNDATION_EXTERN void OVMPSkCTFontSetNSFontWeightMappingFixEnable(bool enable);

FOUNDATION_EXTERN bool OVMPSkFixFontMatchBug(void);

NS_ASSUME_NONNULL_END
