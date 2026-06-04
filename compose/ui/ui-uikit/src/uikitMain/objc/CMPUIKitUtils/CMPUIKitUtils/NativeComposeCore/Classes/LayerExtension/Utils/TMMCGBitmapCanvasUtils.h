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

/**
 创建一个 CGBitmap 画布（上下文）。
 
 @param cacheKey 缓存键值。如果大于 0，内部可能会尝试复用或缓存该画布内存。
 @param width 画布宽度。
 @param height 画布高度。
 @param hasBrush 是否包含笔刷特性（可能涉及特殊的内存分配或层级处理）。
 @return 返回一个指向内部画布结构的不透明指针（Handle/Context），后续操作需传入此指针。
 */
FOUNDATION_EXTERN void *TMMCreateCGBitmapCanvas(int cacheKey, int width, int height, BOOL hasBrush);

/**
 获取画布底层的原始像素数据指针。
 
 @param canvasPtr 通过 TMMCreateCGBitmapCanvas 创建的画布指针。
 @return 指向像素数据的内存首地址（通常用于直接读写像素颜色值）。
 */
FOUNDATION_EXTERN void *TMMCGBitmapCanvasGetPixelsPtr(void *canvasPtr);

/**
 获取画布每一行像素所占用的字节数（Bytes Per Row / Stride）。
 
 @discussion 在遍历像素数据时，需要使用此值来计算换行的内存偏移量，因为考虑到内存对齐，行字节数并不一定等于 width * pixelSize。
 @param canvasPtr 通过 TMMCreateCGBitmapCanvas 创建的画布指针。
 @return 每行的字节数。
 */
FOUNDATION_EXTERN int TMMCGBitmapCanvasGetRowBytes(void *canvasPtr);

/**
 从当前画布生成一个 UIImage 对象，并应用指定的颜色。
 
 @discussion 这通常用于将 Alpha 通道或灰度画布转换为指定颜色的图片。
 @param canvasPtr 通过 TMMCreateCGBitmapCanvas 创建的画布指针。
 @param colorArgb 指定的颜色值（通常为 ARGB 格式的整数，例如 0xAARRGGBB）。
 @return 生成的 UIImage 对象。
 */
FOUNDATION_EXTERN UIImage *TMMCGBitmapCanvasGetImage(void *canvasPtr, int colorArgb);

/**
 释放画布资源。
 
 @discussion 销毁通过 TMMCreateCGBitmapCanvas 创建的画布，防止内存泄漏。
 @param canvasPtr 需要释放的画布指针。
 */
FOUNDATION_EXTERN void TMMReleaseCGBitmapCanvas(void *canvasPtr);

/**
 根据 Alpha 遮罩图生成指定颜色的图片。
 
 @discussion 使用 maskImage 的透明度信息，结合 colorARGB 生成一张纯色的、形状与 mask 相同的图片。
 @param maskImage 源遮罩图片（CGImageRef）。
 @param colorARGB 目标填充颜色（ARGB 整数格式）。
 @param width 目标图片宽度。
 @param height 目标图片高度。
 @return 生成的着色 UIImage。
 */
FOUNDATION_EXTERN UIImage *CreateImageFromAlphaMask(CGImageRef maskImage, int colorARGB, int width, int height);

/**
 根据灰度图（Gray Image）生成着色图片。
 
 @discussion 通常将灰度值映射为透明度或混合强度，从而生成一张带颜色的图片。
 @param grayImage 源灰度图片（CGImageRef）。
 @param colorARGB 目标颜色（ARGB 整数格式）。
 @return 生成的着色 UIImage。
 */
FOUNDATION_EXTERN UIImage *CreateImageByMaskingGrayImage(CGImageRef grayImage, int colorARGB);

NS_ASSUME_NONNULL_END

