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

#import "TMMComposeNativePaint.h"
#import "TMMNativeBaseLayer.h"
#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePath;

/// Compose Canvas  绘制 Point 的 Layer
@interface TVComposePointLayer : TMMNativeBaseLayer

/// 绘制一系列点
/// - Parameters:
///   - points: 点的数组
///   - strokeSize: 描边宽度
///   - color: 填充/描边色
///   - strokeCap: 线段端点类型
- (void)updatePoints:(NSArray<NSNumber *> *)points strokeSize:(float)strokeSize color:(UIColor *)color strokeCap:(TMMNativeDrawStrokeCap)strokeCap;

@end

NS_ASSUME_NONNULL_END
