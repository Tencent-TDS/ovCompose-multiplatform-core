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

typedef NS_ENUM(NSUInteger, OVMPAccessibilityElementSyncType) {
    /// 无操作
    OVMPAccessibilityElementSyncTypeNone,
    /// 同步 AccessibilityLabel (朗读内容)
    OVMPAccessibilityElementSyncTypeAccessibilityLabel,
    /// 同步 AccessibilityHint (朗读提示)
    OVMPAccessibilityElementSyncTypeAccessibilityHint,
    /// 同步自定义操作 (转轮中的 Actions)
    OVMPAccessibilityElementSyncTypeAccessibilityCustomActions,
    /// 同步标识符 (用于 UI 测试或查找)
    OVMPAccessibilityElementSyncTypeAccessibilityIdentifier,
    /// 同步值 (如滑块进度的百分比文本)
    OVMPAccessibilityElementSyncTypeAccessibilityValue,
    /// 通知 Kotlin 侧：该元素失去了无障碍焦点
    OVMPAccessibilityElementSyncTypeAccessibilityDidLoseFocus,
    /// 执行激活操作 (通常对应双击)
    OVMPAccessibilityElementSyncTypeActive,
    /// 执行增加操作 (通常对应滑块上滑/Adjustable)
    OVMPAccessibilityElementSyncTypeIncrement,
    /// 执行减少操作 (通常对应滑块下滑/Adjustable)
    OVMPAccessibilityElementSyncTypeDecrement,
    /// 执行“魔术点击”/返回操作 (双指Z形手势)
    OVMPAccessibilityElementSyncTypePerformEscape,
    /// 同步是否可以获取焦点 (通常指键盘/硬件焦点，非 VoiceOver 焦点)
    OVMPAccessibilityElementSyncTypeCanBecomeFocused,
    /// 通知 Kotlin 侧：已放弃硬件/键盘焦点
    OVMPAccessibilityElementSyncTypeDidResignFocused,
    /// 通知 Kotlin 侧：已获得硬件/键盘焦点
    OVMPAccessibilityElementSyncTypeDidBecomeFocused,
    /// 通知 Kotlin 侧：已获得无障碍(VoiceOver)焦点
    OVMPAccessibilityElementSyncTypeAccessibilityDidBecomeFocused,
    /// 同步滚动结果状态
    OVMPAccessibilityElementSyncTypeAccessibilityScrollResult,
    /// 请求更新焦点状态
    OVMPAccessibilityElementSyncTypeSetNeedsFocusUpdate,
    /// 请求滚动到可见区域
    OVMPAccessibilityElementSyncTypeAccessibilityScrollToVisible,
};

@class OVMPAccessibilityElementProxy;
@class OVMPAccessibilityElementsCache;

/// 无障碍事件回调 Block
/// @param proxy 触发事件的代理对象
/// @param syncType 事件或属性同步的类型
/// @param scrollDirection 如果是滚动事件，指明滚动方向
typedef void (^OVMPAccessibilityElementActionBlock)(OVMPAccessibilityElementProxy *proxy,
                                                    OVMPAccessibilityElementSyncType syncType,
                                                    UIAccessibilityScrollDirection scrollDirection);

/// 无障碍元素代理类
///
/// 该类充当 iOS UIAccessibility 协议与跨平台(Kotlin)逻辑层之间的中间件。
/// 它是一个虚拟的 Accessibility Element，负责将 iOS 系统的无障碍查询转发给 Kotlin，
/// 并将用户的无障碍操作（如点击、滑动）回调给 Kotlin 处理。
@interface OVMPAccessibilityElementProxy : NSObject

#pragma mark - Properties / State

/// kotlin 侧复用的标志位 (唯一 ID)
/// 用于在 Native 和 Kotlin 之间映射同一个虚拟节点
@property (nonatomic, assign, readonly) int64_t identifierKey;

/// kotlin 侧通过这个 block 同步属性或接收事件
/// 当 iOS 系统触发某些操作（如 accessibilityIncrement）时，通过此 Block 通知 Kotlin。
@property (nonatomic, strong, nullable) OVMPAccessibilityElementActionBlock sync;

#pragma mark - UIAccessibility Standard Properties

/// 无障碍标签 (VoiceOver 朗读的主要内容)
@property (nonatomic, strong, nullable) NSString *accessibilityLabel;

/// 无障碍提示 (VoiceOver 朗读完 Label 后稍作停顿朗读的补充提示)
@property (nonatomic, strong, nullable) NSString *accessibilityHint;

/// 无障碍标识符 (不朗读，用于 UI Automation 测试)
@property (nonatomic, strong, nullable) NSString *accessibilityIdentifier;

/// 无障碍值 (例如：滑块的 "50%"，开关的 "已开启")
@property (nonatomic, strong, nullable) NSString *accessibilityValue;

/// 自定义无障碍操作数组
/// 对应 VoiceOver 转轮中的 "Actions" / "操作" 菜单
@property (nonatomic, strong, nullable) NSArray <UIAccessibilityCustomAction *> *accessibilityCustomActions;

/// 标记该对象是否为无障碍元素
/// YES: VoiceOver 可以选中并朗读它；NO: VoiceOver 会忽略它或只将其作为容器。
@property (nonatomic, assign) BOOL isAccessibilityElement;

/// 是否支持 "Perform Escape" 手势
/// 对应双指 Z 形手势，通常用于模态弹窗的关闭或层级返回。
@property (nonatomic, assign) BOOL accessibilityPerformEscape;

/// 是否可以成为硬件/键盘焦点 (UIFocusItem 协议)
@property (nonatomic, assign) BOOL canBecomeFocused;

/// 是否支持激活操作
/// 通常对应 VoiceOver 的双击确认。
@property (nonatomic, assign) BOOL accessibilityActivate;

/// 无障碍容器类型
/// 例如：UIAccessibilityContainerTypeList (列表), UIAccessibilityContainerTypeSemanticGroup (语义组)
@property (nonatomic, assign) UIAccessibilityContainerType accessibilityContainerType;

/// 无障碍特征
/// 用于描述元素类型，如 UIAccessibilityTraitButton, UIAccessibilityTraitHeader, UIAccessibilityTraitAdjustable 等。
@property (nonatomic, assign) UIAccessibilityTraits accessibilityTraits;

/// 滚动操作的结果标志位
/// 用于同步滚动是否成功或是否到达边界。
@property (nonatomic, assign) BOOL accessibilityScrollResult;

/// 元素在屏幕坐标系中的无障碍响应区域 (Frame)
@property (nonatomic, assign, readonly) CGRect accessibilityFrame;

/// 元素在屏幕坐标系中的硬件/键盘焦点响应区域
@property (nonatomic, assign, readonly) CGRect focusFrame;

/// 标记是否需要滚动到可见区域
@property (nonatomic, assign) BOOL accessibilityScrollToVisible;

#pragma mark - Actions (System to Kotlin)

/// 当该元素获得 VoiceOver 焦点时调用
/// 对应系统方法：- (void)accessibilityElementDidBecomeFocused
- (void)accessibilityElementDidBecomeFocused;

/// 当该元素失去 VoiceOver 焦点时调用
/// 对应系统方法：- (void)accessibilityElementDidLoseFocus
- (void)accessibilityElementDidLoseFocus;

/// 执行“增加”操作 (特质为 Adjustable 时，单指上滑)
/// 对应系统方法：- (void)accessibilityIncrement
- (void)accessibilityIncrement;

/// 执行“减少”操作 (特质为 Adjustable 时，单指下滑)
/// 对应系统方法：- (void)accessibilityDecrement
- (void)accessibilityDecrement;

/// 执行滚动操作 (三指滑动)
/// @param direction 滚动的方向
/// @return 如果处理了滚动返回 YES，否则返回 NO
- (BOOL)accessibilityScroll:(UIAccessibilityScrollDirection)direction;

/// 请求将该元素滚动到可见区域
/// @return 操作是否成功
- (BOOL)accessibilityScrollToVisible;

/// 当该元素失去硬件/键盘焦点时调用 (UIFocusItem)
- (void)didResignFocused;

/// 当该元素获得硬件/键盘焦点时调用 (UIFocusItem)
- (void)didBecomeFocused;

/// 标记焦点系统需要更新
/// 强制系统重新查询焦点状态。
- (void)setNeedsFocusUpdate;

#pragma mark - Geometry & Lifecycle

/// 检查无障碍 Frame 是否为空 (CGRectZero)
- (BOOL)isAccessibilityFrameEmpty;

/// 清除所有状态和子元素引用
- (void)clear;

/// 设置硬件焦点的布局信息
/// @param originX X 坐标 (通常是相对于 Window 或 Screen)
/// @param originY Y 坐标
/// @param width 宽度
/// @param height 高度
- (void)setFocusFrame:(CGFloat)originX originY:(CGFloat)originY width:(CGFloat)width height:(CGFloat)height;

/// 设置无障碍响应区域的布局信息
/// @param originX X 坐标 (通常是相对于 Window 或 Screen)
/// @param originY Y 坐标
/// @param width 宽度
/// @param height 高度
- (void)setAccessibilityFrame:(CGFloat)originX originY:(CGFloat)originY width:(CGFloat)width height:(CGFloat)height;

#pragma mark - Tree Management (Container Support)

/// 准备更改子元素
/// 在批量更新 `accessibilityElements` 之前调用，用于开始一个事务或清理旧数据。
- (void)willChangeAccessibilityElements;

/// 添加一个子代理元素
/// 用于构建无障碍树，将子节点挂载到当前节点下。
/// @param element 子节点代理对象
- (void)addAccessibilityElementProxy:(OVMPAccessibilityElementProxy *)element;

/// 添加一个原生的互操作视图 (Interop View)
/// 当无障碍树中混合了原生 UIView (如地图、原生视频播放器) 时使用。
/// @param interopView 嵌入的原生视图
- (void)addAccessibilityInteropView:(UIView *)interopView;

/// 完成子元素的更改
/// 提交更改并更新 `accessibilityElements` 属性。
/// @param cache 可选的缓存对象，用于复用元素
- (void)didChangeAccessibilityElements:(OVMPAccessibilityElementsCache  * _Nullable)cache;

/// 销毁对象
/// 释放资源，断开与 Kotlin 的连接。
- (void)dispose;

/// 调试方法
/// @return 返回当前的无障碍容器对象，用于调试层级关系。
- (id)debugAccessibilityContainer;

@end

NS_ASSUME_NONNULL_END
