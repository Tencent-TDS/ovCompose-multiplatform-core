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
#import "OVMPAccessibilityElementProxy.h"

NS_ASSUME_NONNULL_BEGIN

@class OVMPAccessibilityElementProxy;

@interface OVMPAccessibilityElement : UIAccessibilityElement <UIFocusItem, UIFocusItemContainer>

/// 禁用默认初始化方法
/// @note 本类不支持通过 init 或 new 创建，请使用指定初始化方法 initWithElementProxy:
- (instancetype)init NS_UNAVAILABLE;
+ (instancetype)new NS_UNAVAILABLE;

/// 指定初始化方法
/// @param proxy 关联的无障碍元素代理对象 (OVMPAccessibilityElementProxy)
/// @note 通过此代理对象桥接原生与跨平台端的无障碍数据
- (instancetype)initWithElementProxy:(OVMPAccessibilityElementProxy *)proxy;

/// 获取用于调试工具的唯一标识 Key
/// @return 64位整数类型的 ID
/// @note 供 Debug 工具使用，用于在视图层级调试或自动化测试中唯一锚定该元素
- (int64_t)debugIdentifierKey;

/// 主动释放资源
/// @note 通常由 Kotlin (KT) 侧桥接层主动调用。
/// @discussion 用于在对象生命周期结束前，手动断开引用、清理缓存或释放非托管资源，以防止跨平台桥接导致的内存泄漏。
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
