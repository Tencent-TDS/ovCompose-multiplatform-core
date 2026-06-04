/*
 * Copyright 2023 The Android Open Source Project
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

/// 键盘按键响应回调 Block
/// @param presses 包含 UIPress 对象的集合，表示按下的物理按键信息
typedef void(^OVMPResponderPressBlock)(NSSet *presses);

/// 屏幕阅读器 (VoiceOver) 状态变更回调 Block
/// @param isActive 当前屏幕阅读器是否处于开启状态
typedef void(^OVMPOnScreenReaderActiveBlock)(BOOL isActive);

@class OVMPAccessibilityElementProxy;
@class OVMPAccessibilityElementsCache;

#pragma mark - OVMPAccessibilityRootElement

/// 无障碍根元素
///
/// 这个类是 iOS 原生视图系统与虚拟无障碍树之间的**顶级入口**。
/// 它继承自 `UIAccessibilityElement` 并实现了 `UIFocusItemContainer` 协议，
/// 既负责向 iOS 暴露无障碍内容，也负责管理硬件键盘/Focus Engine 的焦点导航。
@interface OVMPAccessibilityRootElement : UIAccessibilityElement <UIFocusItemContainer>

/// 创建一个新的无障碍根节点实例
+ (instancetype)accessibilityRootElement;

#pragma mark - Properties

/// 元素缓存池
/// 用于缓存和复用 `OVMPAccessibilityElementProxy` 或原生包装对象，
/// 避免在频繁的 UI 刷新中重复创建对象，提高性能。
@property (nonatomic, strong, readonly) OVMPAccessibilityElementsCache *cache;

/// 中介视图 (宿主 View)
/// 指向持有此无障碍树的原生 UIView (例如 ComposeView 或 GLKView)。
/// 根元素需要这个 View 来进行坐标转换和响应链传递。
@property (nonatomic, strong, nullable) UIView *mediatorView;

/// 激活无障碍的延迟回调
/// 当需要确保无障碍树已构建或屏幕阅读器开启时调用的逻辑块。
/// 用于按需初始化无障碍数据，避免在非无障碍模式下消耗资源。
@property (nonatomic, strong, nullable) dispatch_block_t activateAccessibilityIfNeeded;

/// 屏幕阅读器状态监听回调
/// 当 VoiceOver 开关状态发生变化时，通过此 Block 通知 Kotlin 侧，
/// 以便业务层决定是否需要生成语义树。
@property (nonatomic, strong, nullable) OVMPOnScreenReaderActiveBlock onScreenReaderActive;

/// 物理键盘按键事件回调
/// 当宿主 View 接收到 `pressesBegan/Ended` 等物理按键事件时，
/// 将事件通过此 Block 转发给 Kotlin 侧处理 (例如处理外接键盘的方向键导航)。
@property (nonatomic, strong, nullable) OVMPResponderPressBlock onKeyboardPresses;

#pragma mark - Methods

/// 绑定逻辑代理
/// 将一个逻辑层的代理对象 (Proxy, 包含数据) 绑定到这个系统层的根节点上。
///
/// @param proxy Kotlin 侧传递过来的根节点数据代理，如果为 nil 则表示清空绑定。
- (void)bindProxy:(OVMPAccessibilityElementProxy * _Nullable)proxy;

/// 销毁与清理
/// 释放缓存、断开与 MediatorView 的连接、移除通知监听等。
/// 通常在宿主 View 销毁时调用。
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
