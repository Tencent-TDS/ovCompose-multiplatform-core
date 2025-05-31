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

/// Compose 中 UIKitView 的中间容器
@interface TMMInteropWrapView : UIView

/// 实际的 AccessibilityContainer，这个值在 kt 侧设置
@property (nonatomic, weak) id actualAccessibilityContainer;

/// 绑定 compose 的拦截事件的容器
/// - Parameter view: UIView
- (void)bindComposeInteropContainer:(UIView *)view;

/// 当混排的UIView的frame发生变化时，回调到Compose侧
- (void)setOnSizeChange:(void (^)(CGFloat width, CGFloat height))onSizeChange;

@end

NS_ASSUME_NONNULL_END
