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
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativeColorFilter;
@class TMMGaussianBlurFilter;
@class TMMComposeNativePaint;

/// Compose 渲染图片的 Layer
@interface TMMImageDisplayLayer : TMMFastTintColorImageContentLayer

#ifdef DEBUG
/// 染色的主题色
@property (nonatomic, strong) UIColor *tintColor;
#endif

/// 设置图像并应用指定的变换和滤镜效果
///
/// - Parameters:
///   - imagePointer: 要绘制的源图像，必须是有效的 CGImageRef。如果传入 NULL 会清空当前图像。
///   - srcOffset: 源图像的裁剪偏移量（单位：像素），指定从图像的哪个位置开始取样。
///   - srcSize: 源图像的裁剪尺寸（单位：像素），如果设置为 CGSizeZero 则使用整个图像。
///   - dstOffset: 目标绘制偏移量（单位：逻辑点），指定在绘制上下文的哪个位置开始绘制。
///   - dstSize: 目标绘制尺寸（单位：逻辑点），控制图像最终显示的缩放尺寸。
///   - colorFilter: 颜色滤镜对象，可为 nil。用于调整图像的色调、饱和度等颜色属性。
///   - blurFilter: 模糊滤镜对象，可为 nil。用于应用高斯模糊等效果。
///   - paint: 绘制样式对象，可为 nil。控制混合模式、透明度等绘制属性。
///   - density: 显示密度比例（通常与 UIScreen scale 相关），用于正确计算像素与点的转换。
///
- (void)setImage:(CGImageRef)imagePointer
       srcOffset:(CGPoint)srcOffset
         srcSize:(CGSize)srcSize
       dstOffset:(CGPoint)dstOffset
         dstSize:(CGSize)dstSize
     colorFilter:(TMMComposeNativeColorFilter *)colorFilter
      blurFilter:(TMMGaussianBlurFilter *)blurFilter
           paint:(TMMComposeNativePaint *)paint
         density:(CGFloat)density;

@end

NS_ASSUME_NONNULL_END
