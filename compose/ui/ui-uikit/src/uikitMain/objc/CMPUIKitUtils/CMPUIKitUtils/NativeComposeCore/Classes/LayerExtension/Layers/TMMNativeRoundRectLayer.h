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

#import "TMMNativeBaseLayer.h"
#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePath;
@class TMMComposeNativePaint;

/// Compose Canvas 绘制圆角矩形的 Layer
@interface TMMNativeRoundRectLayer : TMMNativeBaseLayer

/// 更新贝塞尔路径的绘制属性并执行指定操作
///
/// - Parameters:
///   - path: 需要更新的路径对象，支持任意形状的贝塞尔路径。传入前必须已完成路径构造。
///   - strokeWidth: 描边线宽度（单位：逻辑像素）。设置为 0 时表示不描边，正值会影响路径的视觉宽度。
///   - color: 路径的填充/描边颜色。当传入 nil 时，默认使用系统黑色（UIColor.black）。
///   - strokeCap: 线段端点样式，控制开放路径两端和虚线片段的显示方式。
///     - `.butt`: 平头端点（精确结束于终点）
///     - `.round`: 圆头端点（添加半圆形帽）
///     - `.square`: 方头端点（延伸线宽的一半长度）
///   - pathOperation: 路径操作类型，支持使用位掩码组合多个操作：
///     - `.stroke`: 描边操作
///     - `.fill`: 非零规则填充
///     - `.eoFill`: 奇偶规则填充
///     - `.clip`: 路径裁剪（需配合图形上下文使用）
- (void)updateUIBezierPath:(UIBezierPath *)path
               strokeWidth:(CGFloat)strokeWidth
                     color:(UIColor *)color
                 strokeCap:(TMMNativeDrawStrokeCap)strokeCap
             pathOperation:(TMMNativeDrawPathOperation)pathOperation;

@end

NS_ASSUME_NONNULL_END
