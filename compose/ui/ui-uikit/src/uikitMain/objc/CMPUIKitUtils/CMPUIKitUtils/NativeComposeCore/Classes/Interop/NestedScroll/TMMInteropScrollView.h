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

/**
通过对 UIScrollView 内部手势的 touch 事件拦截直接转发到 Compose 处理，解决
 Compose 滚动容器嵌套 Native 滚动容器的手势响应问题
 */
@interface TMMInteropScrollView : UIScrollView

/// 设置Compose 的处理事件的 root 视图
- (void)bindComposeInteropContainer:(UIView *)view;

@end

NS_ASSUME_NONNULL_END
