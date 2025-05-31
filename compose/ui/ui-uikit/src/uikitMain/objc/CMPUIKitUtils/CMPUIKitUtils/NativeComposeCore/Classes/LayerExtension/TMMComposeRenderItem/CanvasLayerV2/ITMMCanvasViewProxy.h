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

#import "TMMNativeComposeAdaptivedCanvas.h"
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@class TMMComposeNativePaint;

@protocol ITMMCanvasViewProxy <TMMNativeComposeAdaptivedCanvas>

/// 获取内部的真实视图, kt 侧应该尽可能不去使用该接口
- (UIView *)view;

/// 直接添加一个 UIView , kt 侧应该尽可能不去使用该接口
/// - Parameter view: UIView
- (void)addSubview:(UIView *)view;

/// 从父视图上移除
- (void)removeFromSuperView;

/// 获取内部的 paint
- (TMMComposeNativePaint *)paint;

/// 设置 bounds
/// - Parameters:
///   - originX: originX
///   - originY: originY
///   - boundsWidth: boundsWidth
///   - boundsHeight: boundsHeight
- (void)setBounds:(float)originX originY:(float)originY boundsWidth:(float)boundsWidth boundsHeight:(float)boundsHeight;

/// 设置 center
/// - Parameters:
///   - centerX: centerX
///   - centerY: centerY
- (void)setCenter:(float)centerX centerY:(float)centerY;

/// 设置 layer 的锚点
/// - Parameters:
///   - pointX: pointX
///   - pointY: pointY
- (void)setAnchorPoint:(float)pointX pointY:(float)pointY;

/// 设置透明度 alpha
/// - Parameter alpha: float
- (void)setAlpha:(float)alpha;

/// 将自身带到视图的顶层
- (void)bringSelfToFront;

/// 设置是否隐藏
/// - Parameter hidden: BOOL
- (void)setHidden:(BOOL)hidden;

/// 设置是否裁剪子视图
/// - Parameter clipsToBounds: BOOL
- (void)setClipsToBounds:(BOOL)clipsToBounds;

/// 将传入的 proxy 的 layer 放入自身的 view 的 layer 中展示
/// - Parameter subproxy: TMMCanvasViewProxy
- (void)drawLayerWithSubproxy:(nullable id<ITMMCanvasViewProxy>)subproxy;

/// 设置父节点
- (void)setParent:(nullable id<ITMMCanvasViewProxy>)parentProxy;

/// 将自身添加到 rootView 上
/// - Parameter rootView: UIView
- (void)attachToRootView:(UIView *)rootView;

/// 同步矩阵信息
- (void)applyTransformMatrix:(float)rotationX
                   rotationY:(float)rotationY
                   rotationZ:(float)rotationZ
                      scaleX:(float)scaleX
                      scaleY:(float)scaleY
                translationX:(float)translationX
                translationY:(float)translationY
                transformM34:(double)transformM34;

/// 设置阴影信息
- (void)setShadowWithElevation:(float)shadowElevation
                  shadowRadius:(float)shadowRadius
                shadowColorRed:(float)shadowColorRed
                shadowColorBlue:(float)shadowColorBlue
              shadowColorGreen:(float)shadowColorGreen
              shadowColorAlpha:(float)shadowColorAlpha;

/// 清除 shadow
- (void)clearShadow;

/// 准备复用
- (void)prepareForReuse;

/// 获取 UIView 截图
- (UIImage *)getSnapshotImage;

/// 通过给定的画布大小，获取 UIView 截图
/// - Parameters:
///   - width: int 像素值
///   - height: int 像素值
- (UIImage *)getSnapshotImageWithWidth:(int)width height:(int)height;

@end

NS_ASSUME_NONNULL_END
