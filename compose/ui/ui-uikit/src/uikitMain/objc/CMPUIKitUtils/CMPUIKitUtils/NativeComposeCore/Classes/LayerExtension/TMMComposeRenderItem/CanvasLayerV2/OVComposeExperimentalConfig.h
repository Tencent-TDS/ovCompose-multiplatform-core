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

NS_ASSUME_NONNULL_BEGIN

typedef NS_ENUM(NSInteger, TMMTextFixLeakType) {
    TMMTextFixLeakTypeNone,
    TMMTextFixLeakTypeByteArray,
    TMMTextFixLeakTypeEncodeImage,
    TMMTextFixLeakTypeDecodeImage
};

/// 用于给 CMPUtils 内部做一些实验性的开关控制
@interface OVComposeExperimentalConfig : NSObject

/// 是否开启实验性质的透视矩阵修复逻辑
@property (nonatomic, assign) BOOL enablePerspectiveTransformFix;

/// 是否开启文本异步渲染
@property (nonatomic, assign) BOOL enableTextAsyncPaint;

/// 是否开启 CALayer clip 优化
@property (nonatomic, assign) BOOL enableCALayerClipOpt;

/// 是否开启图片日志打印
@property (nonatomic, assign) BOOL enableImageLog;

/// 是否开启text内存泄漏修复
@property (nonatomic, assign) TMMTextFixLeakType textFixLeakType;


+ (instancetype)new NS_UNAVAILABLE;

- (instancetype)init NS_UNAVAILABLE;

@end

#pragma mark - kotlin bridge

/// 创建一个 OVComposeExperimentalConfig 对象，拿到其地址
FOUNDATION_EXTERN intptr_t OVComposeExperimentalConfigCreate(void);

/// 根据 OVComposeExperimentalConfig 对象的地址，使其引用计数 -1
/// - Parameter configPtr: 对象的地址
FOUNDATION_EXTERN void OVComposeExperimentalConfigRelease(intptr_t configPtr);

/// 将 configPtr 转 OVComposeExperimentalConfig
/// - Parameter configPtr: configPtr
FOUNDATION_EXTERN OVComposeExperimentalConfig  * _Nullable OVComposeConfigPtrToExperimentalConfig(intptr_t configPtr);

/// 设置 OVComposeExperimentalConfig 对象的 enablePerspectiveTransformFix 属性
FOUNDATION_EXTERN void OVComposeExperimentalConfigSetEnablePerspectiveTransformFix(intptr_t configPtr, BOOL enable);

/// 设置 OVComposeExperimentalConfig 对象的 enableTextAsyncPaint 属性
FOUNDATION_EXTERN void OVComposeExperimentalConfigSetEnableTextAsyncPaint(intptr_t configPtr, BOOL enable);

/// 设置 OVComposeExperimentalConfig 对象的 enableCALayerClipOpt 属性
FOUNDATION_EXTERN void OVComposeExperimentalConfigSetEnableCALayerClipOpt(intptr_t configPtr, BOOL enable);

/// 是否开启图片日志打印
FOUNDATION_EXTERN void OVComposeExperimentalConfigSetEnableImageLog(intptr_t configPtr, BOOL enable);

/// 是否开启text内存泄漏修复
FOUNDATION_EXTERN void OVComposeExperimentalConfigSetFixTextLeakType(intptr_t configPtr, TMMTextFixLeakType textFixLeakType);

NS_ASSUME_NONNULL_END
