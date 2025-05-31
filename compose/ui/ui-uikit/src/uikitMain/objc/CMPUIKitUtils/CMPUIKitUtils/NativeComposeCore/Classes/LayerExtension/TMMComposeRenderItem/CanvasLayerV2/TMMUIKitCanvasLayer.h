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

#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

/// CanvasView 的 View 的 Layer
@interface TMMUIKitCanvasLayer : CALayer

/// 准备被复用上屏了，应该在此处初始化一相关属性
- (void)prepareForReuse;

/// 设置阴影
/// - Parameters:
///   - color: 阴影颜色
///   - elevation: kt 侧的 elevation
///   - ktShadowRadius: 阴影圆角
- (void)setShadowWithColor:(UIColor *)color
                 elevation:(float)elevation
            ktShadowRadius:(float)ktShadowRadius;

/// 清除 shadow
- (void)clearShadow;

@end

NS_ASSUME_NONNULL_END
