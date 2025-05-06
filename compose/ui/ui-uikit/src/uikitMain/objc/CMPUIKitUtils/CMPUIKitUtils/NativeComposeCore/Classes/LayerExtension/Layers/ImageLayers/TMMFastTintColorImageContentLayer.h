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

/// 该 Layer 单纯的使用 CALayer 的能力实现 tintColor，不涉及到对图片进行滤镜的处理
@interface TMMFastTintColorImageContentLayer : CALayer

/// 清除图片内容
- (void)clearContents;

/// 设置图片
/// - Parameters:
///   - image: 图片
///   - tintColor: 染色的颜色
- (void)setImage:(CGImageRef)image tintColor:(nullable UIColor *)tintColor;

@end

NS_ASSUME_NONNULL_END
