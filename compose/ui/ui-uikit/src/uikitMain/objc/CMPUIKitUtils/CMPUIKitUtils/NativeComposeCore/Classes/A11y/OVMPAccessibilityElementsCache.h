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

#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

@class OVMPAccessibilityElement;
@class OVMPAccessibilityElementProxy;

/// 无障碍元素缓存管理器
/// @note 负责维护 OVMPAccessibilityElementProxy (数据/逻辑) 与 OVMPAccessibilityElement (iOS 视图包装) 之间的映射关系，
/// 确保同一个 Proxy 在 iOS 侧始终对应同一个 Element 实例（对象同一性）。
@interface OVMPAccessibilityElementsCache : NSObject

/// 获取或创建无障碍元素实例 (Get or Create)
/// @note 检查缓存中是否已存在该 proxy 对应的元素。如果存在则返回缓存对象；如果不存在，则创建一个新对象并存入缓存。
/// @param proxy 跨平台侧传递过来的无障碍代理对象
/// @return 对应的 iOS 无障碍元素实例
- (OVMPAccessibilityElement *)getOrCreateElementWithProxy:(OVMPAccessibilityElementProxy *)proxy;

/// 移除指定 Proxy 对应的缓存
/// @note 当跨平台侧的 UI 元素被销毁、回收或移出屏幕时调用，用于释放对应的 iOS 包装对象，防止内存泄漏。
/// @param proxy 需要移除映射关系的代理对象
- (void)removeElementWithProxy:(OVMPAccessibilityElementProxy *)proxy;

/// 销毁缓存管理器并清理资源
/// @note 清空内部所有的映射表 (Map/Dictionary)，断开所有持有的引用。
/// 通常在宿主 ViewController 销毁或整个模块卸载时调用。
- (void)dispose;

@end

NS_ASSUME_NONNULL_END
