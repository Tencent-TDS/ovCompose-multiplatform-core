/*
 * Tencent is pleased to support the open source community by making ovCompose
 * available. Copyright (C) 2025 THL A29 Limited, a Tencent company. All rights
 * reserved.
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

/// 高斯模糊滤镜
@interface TMMGaussianBlurFilter : NSObject

/// 模糊半径
@property (nonatomic, assign) CGFloat blurRadius;

/// 对应的 CIFilter
- (CIFilter *)filter;

/// 返回对应的高斯模糊图片
- (nullable UIImage *)filterImageWithImageRef:(CGImageRef)imageRef;

@end

NS_ASSUME_NONNULL_END
