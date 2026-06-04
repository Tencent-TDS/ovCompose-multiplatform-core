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
#import "UIView+TMMInspector.h"

NS_ASSUME_NONNULL_BEGIN

typedef NSArray<NSDictionary<NSString *, id> *> * _Nullable (^TMMNativeGetInspectorNodeBlock)(void);

/// Compose 的根容器的基类，kotlin 侧会通过 kt 的 class 继承制该类
@interface TMMInteropBaseView : UIView

/// 是否禁用点击事件，该值会在 kt 侧被修改
@property (nonatomic, strong) NSNumber *disableTouch;

/// kotlin  侧根据自身配置需求，决定是否开启 Native 获取手势速度的功能
- (void)enableNativeGestureVelocity;

/// kt 侧通过此接口获取到 Y 方向的速度。背景：Compose 在用户手指离开屏幕后进行滑动动画 ，需要计算初速度，
/// 但是无论如何拟合，都很难和 iOS 原生的初速度一致，导致滑动体验不佳。通过此接口可以获取和原生一致的初速度，
/// 可以极大提升交互体验。此外，该接口并没有设计为返回 CGSize。kt 侧会对 CGSize 装箱，有损性能，性能非常重要
/// 因此分开了两个接口
- (float)panGestureVelocityY;

/// kt 侧通过此接口获取到 Y 方向的速度
- (float)panGestureVelocityX;

/// kt 侧在滑动过程中，手指暂停的时候通过此接口重置初速度，算出为 0，则停止 fling 动画
- (void)resetGestureVelocity;

/// 获取节点信息
@property (nonatomic, strong) TMMNativeGetInspectorNodeBlock nodeBlock;

@end

NS_ASSUME_NONNULL_END
