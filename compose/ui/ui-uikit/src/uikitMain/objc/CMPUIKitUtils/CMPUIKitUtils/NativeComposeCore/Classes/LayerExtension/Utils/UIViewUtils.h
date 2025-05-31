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

/// UIView 添加子视图，该方法用于避免其他库 Hook 了 UIView 的 addSubview 作过多的逻辑导致卡顿
/// - Parameters:
///   - superview: 父视图
///   - subview: 子视图
FOUNDATION_EXTERN OS_ALWAYS_INLINE void TMMCMPUIViewFastAddSubview(UIView *superview, UIView *subview);

/// 将 UIView 从父视图移除，该方法用于避免其他库 Hook 了 UIView 的 removeFromSuperview 作过多的逻辑导致卡顿
/// - Parameters:
///   - view: 视图
FOUNDATION_EXTERN OS_ALWAYS_INLINE void TMMCMPUIViewFastRemoveFromSuperview(UIView *view);

/// 设置 UIView 的 hidden 属性，该方法用于避免其他库 Hook 了 UIView 的 setHidden 作过多的逻辑导致卡顿
/// - Parameters:
///   - view: 视图
///   - hidden: BOOL
FOUNDATION_EXTERN OS_ALWAYS_INLINE void TMMCMPUIViewFastSetHidden(UIView *view, BOOL hidden);

/// 获取 Native 视图是否消费某个Compose 事件
/// - Parameters:
///   - touchEvent: UIEvent
///   - wrappingView: wrappingView
FOUNDATION_EXTERN OS_ALWAYS_INLINE BOOL TMMCMPUIViewShouldConsumeEvent(_Nullable id touchEvent, UIView *wrappingView);

/// 对 UIView 进行截屏
/// - Parameters:
///   - view: 截屏的 UIView
///   - width: 宽度 dp
///   - height: 高度 dp
FOUNDATION_EXTERN UIImage *OVCMPSnapshotImageFromUIView(UIView *view, float width, float height, float density);

NS_ASSUME_NONNULL_END
