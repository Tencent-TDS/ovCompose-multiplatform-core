/*
 * Tencent is pleased to support the open source community by making tvCompose available.
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

/// Native 实现的键盘监听器
@interface OVComposeKeyboardVisibilityListener : NSObject

/// 键盘高度
@property (nonatomic, assign, readonly) CGFloat keyboardHeight;

/// 关闭键盘
+ (void)endEditing;

/// 准备注册一些通知，给到后面键盘使用
- (void)prepareIfNeeded;

/// 绑定 Compose 容器视图，用于校正底部边距
- (void)bindComposeView:(nullable UIView *)view;

/// 键盘高度变化后执行，kt 侧重写
/// - Parameter keyboardHeight: 键盘高度
- (void)keyboardOverlapHeightChanged:(float)keyboardHeight;

/// 键盘即将被展示，kt 侧重写
- (void)keyboardWillShow;

/// 键盘即将被移除，kt 侧重写
- (void)keyboardWillHide;

/// 销毁
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
