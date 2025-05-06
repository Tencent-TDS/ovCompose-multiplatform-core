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

#import "TMMFastTintColorImageContentLayer.h"

NS_ASSUME_NONNULL_BEGIN

/// 该 Layer 通过 CoreGraphics 的能力实现图片的裁剪、染色、缩放
@interface TMMImageClipLayer : TMMFastTintColorImageContentLayer

/// 设置图片
/// - Parameters:
///   - imagePointer: 图片的 CGImageRef 指针
///   - srcOffset:截取的原始图片的起始点
///   - srcSize: 截取的原始图片的 size
///   - dstOffset: 绘制到 Canvas 上的起始点
///   - dstSize: 绘制到 Canvas 上的 size
///   - density: 缩放系数
///   - tintColor: 染色的颜色
- (void)setImage:(CGImageRef)imagePointer
       srcOffset:(const CGPoint)srcOffset
         srcSize:(const CGSize)srcSize
       dstOffset:(const CGPoint)dstOffset
         dstSize:(const CGSize)dstSize
         density:(CGFloat)density
       tintColor:(UIColor *)tintColor;

@end

NS_ASSUME_NONNULL_END
