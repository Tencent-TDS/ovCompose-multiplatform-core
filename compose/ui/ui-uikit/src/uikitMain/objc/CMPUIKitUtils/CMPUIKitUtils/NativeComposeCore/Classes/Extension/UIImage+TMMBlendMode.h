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

#import "TMMNativeEnums.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface UIImage (TMMBlendMode)

/// 根据染色的颜色和混合模式生成新的图片
/// - Parameters:
///   - tintColor: 染色的颜色
///   - blendMode: 混合模式
- (UIImage *)tmmcompose_imageWithTintColor:(UIColor *)tintColor blendMode:(TMMNativeDrawBlendMode)blendMode;

@end

NS_ASSUME_NONNULL_END
